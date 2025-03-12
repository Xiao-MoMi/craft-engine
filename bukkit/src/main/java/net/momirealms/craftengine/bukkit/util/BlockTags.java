package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public class BlockTags {
    private static final Map<Key, Object> CACHE = new HashMap<>();

    private BlockTags() {}

    public static Object getOrCreate(Key key) {
        Object value = CACHE.get(key);
        if (value == null) {
            try {
                value = Reflections.method$TagKey$create.invoke(null, Reflections.instance$Registries$BLOCK, Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, key.namespace(), key.value()));
                CACHE.put(key, value);
                return value;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create block tag: " + key, e);
            }
        } else {
            return value;
        }
    }
}
