package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;

public final class KeyUtils {
    private KeyUtils() {}

    public static Key resourceLocationToKey(Object resourceLocation) {
        return Key.of(FastNMS.INSTANCE.method$ResourceLocation$namespace(resourceLocation), FastNMS.INSTANCE.method$ResourceLocation$path(resourceLocation));
    }

    public static Key namespacedKey2Key(NamespacedKey key) {
        return Key.of(key.getNamespace(), key.getKey());
    }

    public static Key adventureKey2Key(net.kyori.adventure.key.Key key) {
        return Key.of(key.namespace(), key.value());
    }

    public static Object toResourceLocation(String namespace, String path) {
        return FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath(namespace, path);
    }

    public static Object toResourceLocation(Key key) {
        return toResourceLocation(key.namespace(), key.value());
    }

    public static NamespacedKey toNamespacedKey(Key key) {
        return new NamespacedKey(key.namespace(), key.value());
    }
}
