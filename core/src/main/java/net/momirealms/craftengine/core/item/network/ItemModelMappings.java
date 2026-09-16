package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

/** Item model names from the last successfully generated protected resource pack. */
public final class ItemModelMappings {
    private static volatile Map<Key, Key> mappings = Map.of();

    private ItemModelMappings() {
    }

    public static void setMappings(Map<Key, Key> mappings) {
        ItemModelMappings.mappings = Map.copyOf(mappings);
    }

    public static Map<Key, Key> getMappings() {
        return mappings;
    }
}
