package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Rotation;

public final class RotationUtils {
    private RotationUtils() {}

    public static Rotation fromNMSRotation(Object rotation) {
        int index = ((Enum<?>) rotation).ordinal();
        return Rotation.values()[index];
    }
}
