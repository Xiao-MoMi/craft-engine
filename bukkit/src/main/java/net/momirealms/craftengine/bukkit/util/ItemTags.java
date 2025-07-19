package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.HashMap;
import java.util.Map;

public class ItemTags {
    private static final Map<Key, Object> CACHE = new HashMap<>();

    public static final Key AXES = Key.of("minecraft:axes");
    public static final Key SWORDS = Key.of("minecraft:swords");
    public static final Tag<Material> ITEMS_HARNESSES = getHarnessTag();

    private ItemTags() {}

    public static Object getOrCreate(Key key) {
        Object value = CACHE.get(key);
        if (value == null) {
            value = FastNMS.INSTANCE.method$TagKey$create(MRegistries.ITEM, KeyUtils.toResourceLocation(key));
            CACHE.put(key, value);
        }
        return value;
    }

    public static Tag<Material> getHarnessTag() {
        if (!VersionHelper.isOrAbove1_21_6()) return null;
        try {
            return Bukkit.getTag("items", NamespacedKey.minecraft("harnesses"), Material.class);
        } catch (Exception e) {
            return null;
        }
    }
}
