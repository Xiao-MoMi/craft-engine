package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;

public class KeyUtils {

    private KeyUtils() {}

    public static Key namespacedKey2Key(NamespacedKey key) {
        return Key.of(key.namespace(), key.value());
    }

    public static Key adventureKey2Key(net.kyori.adventure.key.Key key) {
        return Key.of(key.namespace(), key.value());
    }
}
