package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;

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
            value = FastNMS.INSTANCE.method$TagKey$create(MRegistries.ITEM, KeyUtils.toResourceLocation(key));
            CACHE.put(key, value);
        }
        return value;
    }
}
