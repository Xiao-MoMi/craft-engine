package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LootEntryContainers {
    public static final Key ALTERNATIVES = Key.from("craftengine:alternatives");
    public static final Key ITEM = Key.from("craftengine:item");
    public static final Key FURNITURE_ITEM = Key.from("craftengine:furniture_item");
    public static final Key EXP = Key.from("craftengine:exp");

    static {
        register(ALTERNATIVES, AlternativesLootEntryContainer.FACTORY);
        register(ITEM, SingleItemLootEntryContainer.FACTORY);
        register(EXP, ExpLootEntryContainer.FACTORY);
        register(FURNITURE_ITEM, FurnitureItemLootEntryContainer.FACTORY);
    }

    public static <T> void register(Key key, LootEntryContainerFactory<T> factory) {
        Holder.Reference<LootEntryContainerFactory<?>> holder = ((WritableRegistry<LootEntryContainerFactory<?>>) BuiltInRegistries.LOOT_ENTRY_CONTAINER_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.LOOT_ENTRY_CONTAINER_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static <T> List<LootEntryContainer<T>> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<LootEntryContainer<T>> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    @SuppressWarnings("unchecked")
    public static <T> LootEntryContainer<T> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.loot_table.entry.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        LootEntryContainerFactory<T> factory = (LootEntryContainerFactory<T>) BuiltInRegistries.LOOT_ENTRY_CONTAINER_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.loot_table.entry.invalid_type", type);
        }
        return factory.create(map);
    }
}
