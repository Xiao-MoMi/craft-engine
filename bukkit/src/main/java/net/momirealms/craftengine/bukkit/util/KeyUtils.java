package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import org.bukkit.NamespacedKey;

public final class KeyUtils {
    private KeyUtils() {}

    public static Key identifierToKey(Object identifier) {
        return Key.of(IdentifierProxy.INSTANCE.getNamespace(identifier), IdentifierProxy.INSTANCE.getPath(identifier));
    }

    public static Key namespacedKeyToKey(NamespacedKey key) {
        return Key.of(key.getNamespace(), key.getKey());
    }

    public static Key adventureKeyToKey(net.kyori.adventure.key.Key key) {
        return Key.of(key.namespace(), key.value());
    }

    @SuppressWarnings("PatternValidation")
    public static net.kyori.adventure.key.Key toAdventureKey(Key key) {
        return net.kyori.adventure.key.Key.key(key.namespace(), key.value());
    }

    public static Object toIdentifier(String namespace, String path) {
        return IdentifierProxy.INSTANCE.newInstance(namespace, path);
    }

    public static Object toIdentifier(Key key) {
        return toIdentifier(key.namespace(), key.value());
    }

    public static NamespacedKey toNamespacedKey(Key key) {
        return new NamespacedKey(key.namespace(), key.value());
    }

    /*
    todo 暂时保留兼容性后续请删除
     */

    @Deprecated(forRemoval = true)
    public static Key resourceLocationToKey(Object identifier) {
        return identifierToKey(identifier);
    }

    @Deprecated(forRemoval = true)
    public static Object toResourceLocation(String namespace, String path) {
        return toIdentifier(namespace, path);
    }

    @Deprecated(forRemoval = true)
    public static Object toResourceLocation(Key key) {
        return toIdentifier(key.namespace(), key.value());
    }
}
