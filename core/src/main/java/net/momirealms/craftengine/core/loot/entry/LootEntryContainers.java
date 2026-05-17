package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class LootEntryContainers {
    public static final LootEntryContainerType<AlternativesLootEntryContainer> ALTERNATIVES = register(Key.ce("alternatives"), AlternativesLootEntryContainer.FACTORY);
    public static final LootEntryContainerType<AlternativesLootEntryContainer> IF_ELSE = register(Key.ce("if_else"), AlternativesLootEntryContainer.FACTORY);
    public static final LootEntryContainerType<SingleItemLootEntryContainer> ITEM = register(Key.ce("item"), SingleItemLootEntryContainer.FACTORY);
    public static final LootEntryContainerType<ExpLootEntryContainer> EXP = register(Key.ce("exp"), ExpLootEntryContainer.FACTORY);
    public static final LootEntryContainerType<FurnitureItemLootEntryContainer> FURNITURE_ITEM = register(Key.ce("furniture_item"), FurnitureItemLootEntryContainer.FACTORY);
    public static final LootEntryContainerType<EmptyLoopEntryContainer> EMPTY = register(Key.ce("empty"), EmptyLoopEntryContainer.FACTORY);

    private LootEntryContainers() {}

    public static <T extends LootEntryContainer> LootEntryContainerType<T> register(Key key, LootEntryContainerFactory<T> factory) {
        LootEntryContainerType<T> type = new LootEntryContainerType<>(key, factory);
        ((WritableRegistry<LootEntryContainerType<? extends LootEntryContainer>>) BuiltInRegistries.LOOT_ENTRY_CONTAINER_TYPE)
                .register(ResourceKey.create(Registries.LOOT_ENTRY_CONTAINER_TYPE.location(), key), type);
        return type;
    }

    public static LootEntryContainer fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static LootEntryContainer fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        LootEntryContainerType<? extends LootEntryContainer> containerType = BuiltInRegistries.LOOT_ENTRY_CONTAINER_TYPE.getValue(key);
        if (containerType == null) {
            throw new KnownResourceException("loot.entry.unknown_type", section.assemblePath("type"), type);
        }
        return containerType.factory().create(section);
    }
}
