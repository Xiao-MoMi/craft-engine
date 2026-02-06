package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;

import java.util.HashMap;
import java.util.Map;

public final class BlockTags {
    private static final Map<Key, Object> CACHE = new HashMap<>(64);

    private BlockTags() {}

    public static Object getOrCreate(Key key) {
        Object value = CACHE.get(key);
        if (value == null) {
            value = TagKeyProxy.INSTANCE.create(MRegistries.BLOCK, KeyUtils.toIdentifier(key));
            CACHE.put(key, value);
        }
        return value;
    }
}
