package net.momirealms.craftengine.bedrock.geyser.item.component;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class GeyserItemComponentPatcher {
    private final Map<String, UnaryOperator<NbtMap>> patches;
    private final Map<String, GeyserItemText> itemText;
    private final boolean hasRecipeLore;
    private final GeyserPlayer player;
    private final Map<String, Int2ObjectMap<ItemDefinition>> trimItems = new HashMap<>();

    public GeyserItemComponentPatcher(GeyserPlayer player, GeyserItemComponentPatchCache patchCache) {
        this.patches = patchCache.forMappings(player.session().getItemMappings());
        this.itemText = patchCache.itemText();
        this.hasRecipeLore = patchCache.hasRecipeLore();
        this.player = player;
    }

    public void patchRecipes(GeyserPacketEvent<CraftingDataPacket> event) {
        CraftingDataPacket packet = event.packet();
        if (this.hasRecipeLore) {
            GeyserRecipeLore.patch(packet, this.itemText, this.player.locale());
        }
        List<SmithingTrimRecipeData> recipes = packet.getSmithingTrimData();
        if (this.trimItems.isEmpty() || recipes.stream().noneMatch(recipe -> GeyserArmorTrimRecipes.supports(recipe, this.trimItems))) {
            return;
        }
        List<SmithingTrimRecipeData> snapshot = List.copyOf(recipes);
        // Geyser 在发包返回后更新配方 ID，延后分配，避免新 ID 被覆盖或与原配方冲突。
        this.player.session().scheduleInEventLoop(() -> {
            CraftingDataPacket extra = GeyserArmorTrimRecipes.translate(snapshot, this.trimItems, this.player.session().getLastRecipeNetId()::incrementAndGet);
            this.player.session().sendUpstreamPacket(extra);
        }, 0, TimeUnit.MILLISECONDS);
    }

    public void patch(GeyserPacketEvent<ItemComponentPacket> event) {
        ItemComponentPacket packet = event.packet();
        List<ItemDefinition> definitions = packet.getItems();
        String locale = this.player.locale();
        Map<GeyserItemText, String> localizedNames = this.itemText.isEmpty() ? Map.of() : new IdentityHashMap<>();
        Function<GeyserItemText, String> translateName = text -> text.displayName(locale);
        for (int i = 0; i < definitions.size(); i++) {
            ItemDefinition definition = definitions.get(i);
            String identifier = definition.getIdentifier();
            UnaryOperator<NbtMap> patch = this.patches.get(identifier);
            GeyserItemText text = this.itemText.get(identifier);
            String displayName = text == null ? null : localizedNames.computeIfAbsent(text, translateName);
            if (patch == null && displayName == null) {
                continue;
            }
            NbtMap original = definition.getComponentData();
            NbtMap data = original;
            if (patch != null) {
                data = patch.apply(original);
                this.collectTrimItem(definition, data);
            }
            if (displayName != null) {
                data = withDisplayName(data, displayName);
            }
            if (data == original) {
                continue;
            }
            // 只替换发给客户端的组件数据，保留 Geyser 注册对象、runtime ID 和栈身份。
            definitions.set(i, new SimpleItemDefinition(
                    identifier,
                    definition.getRuntimeId(),
                    definition.getVersion(),
                    definition.isComponentBased(),
                    data
            ));
        }
    }

    private void collectTrimItem(ItemDefinition definition, NbtMap data) {
        for (String tag : data.getCompound("components").getList("item_tags", NbtType.STRING)) {
            if (tag.startsWith(GeyserArmorTrimRecipes.BASE_TAG_PREFIX)) {
                this.trimItems.computeIfAbsent(tag, key -> new Int2ObjectOpenHashMap<>()).putIfAbsent(definition.getRuntimeId(), definition);
            }
        }
    }

    private static NbtMap withDisplayName(NbtMap data, String name) {
        NbtMap components = data.getCompound("components");
        NbtMap displayName = components.getCompound("minecraft:display_name");
        if (name.equals(displayName.getString("value"))) {
            return data;
        }
        NbtMap named = displayName.toBuilder().putString("value", name).build();
        NbtMap updatedComponents = components.toBuilder().putCompound("minecraft:display_name", named).build();
        return data.toBuilder().putCompound("components", updatedComponents).build();
    }
}
