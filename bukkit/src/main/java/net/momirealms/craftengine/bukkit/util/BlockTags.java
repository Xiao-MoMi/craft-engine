package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public final class BlockTags {
    private static final Map<Key, Object> CACHE = new HashMap<>();

    private BlockTags() {}

    public static Object getOrCreate(Key key) {
        Object value = CACHE.get(key);
        if (value == null) {
            try {
                value = CoreReflections.method$TagKey$create.invoke(null, MRegistries.BLOCK, KeyUtils.toResourceLocation(key));
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
