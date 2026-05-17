package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ObfuscatedItemModelProcessor implements SimpleNetworkItemProcessor {
    public static final Set<Key> CAN_OBF = ConcurrentHashMap.newKeySet();
    private static Map<Key, Key> mappings = Map.of();
    private final Key data;

    public ObfuscatedItemModelProcessor(Key data) {
        this.data = data;
    }

    public static void setMappings(Map<Key, Key> mappings) {
        ObfuscatedItemModelProcessor.mappings = Objects.requireNonNull(mappings);
    }

    public static void resetMappings() {
        ObfuscatedItemModelProcessor.mappings = Map.of();
    }

    public static Map<Key, Key> getMappings() {
        return mappings;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_MODEL;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (item.hasNonDefaultComponent(DataComponentKeys.ITEM_MODEL)) return item;
        Key model = mappings.getOrDefault(this.data, this.data);
        return item.itemModel(model.asString());
    }
}
