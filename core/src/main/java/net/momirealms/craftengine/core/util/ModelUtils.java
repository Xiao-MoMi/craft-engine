package net.momirealms.craftengine.core.util;

import org.joml.Vector3fc;

public final class ModelUtils {

    private ModelUtils() {
    }

    public static float[] defaultUv(String face, float[] from, float[] to) {
        return defaultUv(face, from[0], from[1], from[2], to[0], to[1], to[2]);
    }

    public static float[] defaultUv(String face, Vector3fc from, Vector3fc to) {
        return defaultUv(face, from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
    }

    private static float[] defaultUv(String face, float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        return switch (face) {
            case "down" -> new float[]{fromX, 16f - toZ, toX, 16f - fromZ};
            case "up" -> new float[]{fromX, fromZ, toX, toZ};
            case "north" -> new float[]{16f - toX, 16f - toY, 16f - fromX, 16f - fromY};
            case "south" -> new float[]{fromX, 16f - toY, toX, 16f - fromY};
            case "west" -> new float[]{fromZ, 16f - toY, toZ, 16f - fromY};
            case "east" -> new float[]{16f - toZ, 16f - toY, 16f - fromZ, 16f - fromY};
            default -> null;
        };
    }
}
