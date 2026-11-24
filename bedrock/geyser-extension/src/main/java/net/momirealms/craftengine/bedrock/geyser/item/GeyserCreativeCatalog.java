package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GeyserCreativeCatalog {
    private static final int CATEGORY_COUNT = CreativeItemCategory.values().length;

    private final List<CreativeItemData> entries;
    private final List<CreativeItemGroup> groups;
    private final int[] unnamedGroupIds;
    private final Map<ItemData, ItemStack> javaStacksByEntry;
    private final Map<ItemDefinition, ItemMapping> reverseMappings;

    private GeyserCreativeCatalog(ItemMappings base, int expectedEntries) {
        this.entries = new ArrayList<>(base.getCreativeItems().size() + expectedEntries);
        this.entries.addAll(base.getCreativeItems());
        this.groups = new ArrayList<>(base.getCreativeItemGroups());
        this.unnamedGroupIds = new int[CATEGORY_COUNT];
        Arrays.fill(this.unnamedGroupIds, -1);
        for (int i = 0; i < this.groups.size(); i++) {
            CreativeItemGroup group = this.groups.get(i);
            if (group.getName().isEmpty()) {
                this.unnamedGroupIds[group.getCategory().ordinal()] = i;
            }
        }
        this.javaStacksByEntry = new IdentityHashMap<>(expectedEntries);
        this.reverseMappings = LinkedHashMap.newLinkedHashMap(expectedEntries);
    }

    @Nullable
    static ItemMappings build(GeyserSession session, ItemMappings base, List<ItemStack> stacks,
                              Map<String, CreativeItemCategory> categoriesByVariant, ExtensionLogger logger) {
        GeyserCreativeCatalog catalog = new GeyserCreativeCatalog(base, Math.min(stacks.size(), categoriesByVariant.size()));
        for (ItemStack stack : stacks) {
            ItemData appearance;
            try {
                appearance = ItemTranslator.translateToBedrock(session, stack);
            } catch (RuntimeException exception) {
                logger.error("Could not translate a CraftEngine creative item with Java ID " + stack.getId(), exception);
                continue;
            }
            CreativeItemCategory category = categoriesByVariant.get(appearance.getDefinition().getIdentifier());
            if (category != null) {
                catalog.add(stack, appearance, category);
            }
        }
        if (catalog.reverseMappings.isEmpty()) {
            return null;
        }
        return catalog.extendMappings(base);
    }

    private void add(ItemStack stack, ItemData appearance, CreativeItemCategory category) {
        int netId = this.entries.size() + 1;
        ItemData entry = appearance.toBuilder().netId(netId).build();
        this.entries.add(new CreativeItemData(entry, netId, this.groupId(category)));
        this.javaStacksByEntry.put(entry, stack);

        ItemDefinition definition = entry.getDefinition();
        if (!this.reverseMappings.containsKey(definition)) {
            String identifier = definition.getIdentifier();
            ItemMapping mapping = ItemMapping.builder()
                    .bedrockIdentifier(identifier)
                    .bedrockDefinition(definition)
                    .bedrockBlockDefinition(entry.getBlockDefinition())
                    .javaItem(new CreativeItem(identifier, this.javaStacksByEntry))
                    .build();
            this.reverseMappings.put(definition, mapping);
        }
    }

    private int groupId(CreativeItemCategory category) {
        if (this.groups.isEmpty()) {
            return 0;
        }
        int categoryIndex = category.ordinal();
        int groupId = this.unnamedGroupIds[categoryIndex];
        if (groupId == -1) {
            groupId = this.groups.size();
            this.groups.add(new CreativeItemGroup(category, "", ItemData.AIR));
            this.unnamedGroupIds[categoryIndex] = groupId;
        }
        return groupId;
    }

    private ItemMappings extendMappings(ItemMappings base) {
        ItemMapping[] original = base.getItems();
        ItemMapping[] extended = Arrays.copyOf(original, original.length + this.reverseMappings.size());
        int index = original.length;
        for (ItemMapping mapping : this.reverseMappings.values()) {
            extended[index++] = mapping;
        }
        return ItemMappingsProxy.INSTANCE.copy(base, extended, Collections.unmodifiableList(this.entries), Collections.unmodifiableList(this.groups));
    }

    private static final class CreativeItem extends Item {
        private final Map<ItemData, ItemStack> javaStacksByEntry;

        private CreativeItem(String identifier, Map<ItemData, ItemStack> javaStacksByEntry) {
            super(identifier, Item.builder());
            this.javaStacksByEntry = javaStacksByEntry;
        }

        @Override
        public boolean ignoreDamage() {
            return true;
        }

        @NotNull
        @Override
        public GeyserItemStack translateToJava(GeyserSession session, @NotNull ItemData data, @NotNull ItemMapping mapping, @NotNull ItemMappings mappings) {
            // Geyser resolves a creative request to the exact catalogue entry. Other packets may carry the same runtime ID.
            ItemStack template = this.javaStacksByEntry.get(data);
            if (template == null || session.getGameMode() != GameMode.CREATIVE) {
                return Items.AIR.translateToJava(session, data, mapping, mappings);
            }
            DataComponents components = template.getDataComponentsPatch();
            return GeyserItemStack.of(session, template.getId(), template.getAmount(), components == null ? null : components.clone());
        }
    }
}
