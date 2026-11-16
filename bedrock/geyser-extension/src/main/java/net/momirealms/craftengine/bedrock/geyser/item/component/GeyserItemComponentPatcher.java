package net.momirealms.craftengine.bedrock.geyser.item.component;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GeyserItemComponentPatcher {
    private final Map<String, GeyserItemComponentPatch> patches;
    private final ItemMappings mappings;
    private final Set<String> trimBases = new HashSet<>();

    GeyserItemComponentPatcher(Map<String, GeyserItemComponentPatch> patches, ItemMappings mappings) {
        this.patches = patches;
        this.mappings = mappings;
    }

    public static void install(GeyserPlayer player, Map<String, GeyserItemComponentPatch> patches) {
        GeyserItemComponentPatcher patcher = new GeyserItemComponentPatcher(patches, player.session().getItemMappings());
        player.network().onSend(ItemComponentPacket.class, patcher::patch);
        player.network().onSend(CraftingDataPacket.class, event -> patcher.patchRecipes(player, event.packet()));
    }

    private void patchRecipes(GeyserPlayer player, CraftingDataPacket packet) {
        List<SmithingTrimRecipeData> recipes = packet.getSmithingTrimData();
        if (recipes.stream().noneMatch(recipe -> GeyserArmorTrimRecipes.supports(recipe, this.trimBases))) {
            return;
        }
        List<SmithingTrimRecipeData> snapshot = List.copyOf(recipes);
        // Geyser 在发包返回后更新配方 ID，延后分配，避免新 ID 被覆盖或与原配方冲突。
        player.session().scheduleInEventLoop(() -> {
            CraftingDataPacket extra = GeyserArmorTrimRecipes.translate(snapshot, this.trimBases,
                    player.session().getLastRecipeNetId()::incrementAndGet);
            player.session().sendUpstreamPacket(extra);
        }, 0, TimeUnit.MILLISECONDS);
    }

    private void patch(GeyserPacketEvent<ItemComponentPacket> event) {
        ItemComponentPacket packet = event.packet();
        List<ItemDefinition> definitions = packet.getItems();
        for (int i = 0; i < definitions.size(); i++) {
            ItemDefinition definition = definitions.get(i);
            GeyserItemComponentPatch patch = this.patches.get(definition.getIdentifier());
            if (patch != null) {
                NbtMap original = definition.getComponentData();
                NbtMap data = patch.apply(original, this.mappings);
                for (String tag : data.getCompound("components").getList("item_tags", NbtType.STRING)) {
                    if (tag.startsWith(GeyserArmorTrimRecipes.BASE_TAG_PREFIX)) {
                        this.trimBases.add(tag);
                    }
                }
                if (data == original) {
                    continue;
                }
                // 只替换发给客户端的组件数据，保留 Geyser 注册对象、runtime ID 和栈身份。
                definitions.set(i, new SimpleItemDefinition(
                        definition.getIdentifier(),
                        definition.getRuntimeId(),
                        definition.getVersion(),
                        definition.isComponentBased(),
                        data
                ));
            }
        }
    }
}
