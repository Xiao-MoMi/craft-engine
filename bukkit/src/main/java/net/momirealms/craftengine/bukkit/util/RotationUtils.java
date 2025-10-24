package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.Rotation;

public final class RotationUtils {

    private RotationUtils() {}

    public static Rotation fromNMSRotation(Object rotation) {
        try {
            int index = (int) CoreReflections.method$Rotation$ordinal.invoke(rotation);
            return Rotation.values()[index];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toNMSRotation(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90 -> {
                return CoreReflections.Instance.rotation$CLOCKWISE_90;
            }
            case CLOCKWISE_180 -> {
                return CoreReflections.Instance.rotation$CLOCKWISE_180;
            }
            case COUNTERCLOCKWISE_90 -> {
                return CoreReflections.Instance.rotation$COUNTERCLOCKWISE_90;
            }
            default -> {
                return CoreReflections.Instance.rotation$NONE;
            }
        }
    }
}
