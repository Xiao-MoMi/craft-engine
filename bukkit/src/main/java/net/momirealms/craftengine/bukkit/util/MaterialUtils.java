package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class MaterialUtils {

    public static Material MACE;

    static {
        try {
            MACE = Material.valueOf("MACE");
        } catch (Exception ignore) {
            MACE = null;
        }
    }

    private MaterialUtils() {}

    @Nullable
    public static Material getMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        if (name.contains(":")) return Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(name)));
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ENGLISH));
        return Optional.ofNullable(Registry.MATERIAL.get(key)).orElseGet(() -> {
            try {
                return Material.valueOf(name.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    public static Material getMaterial(Key key) {
        return Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(key.toString())));
    }
}
