package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;

import java.util.HashMap;
import java.util.Map;

public final class ItemTags {
    private static final Map<Key, Object> CACHE = new HashMap<>();

    public static final Key AXES = Key.of("minecraft:axes");
    public static final Key SWORDS = Key.of("minecraft:swords");
    public static final Key DYEABLE = Key.of("minecraft:dyeable");

    private ItemTags() {}

    public static Object getOrCreate(Key key) {
        Object value = CACHE.get(key);
        if (value == null) {
            value = TagKeyProxy.INSTANCE.create(MRegistries.ITEM, KeyUtils.toIdentifier(key));
            CACHE.put(key, value);
        }
        return value;
    }
}
