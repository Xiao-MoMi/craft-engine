package net.momirealms.craftengine.core.util;

public final class ModelUtils {

    private ModelUtils() {
    }

    public static float[] defaultUv(String face, float[] from, float[] to) {
        return switch (face) {
            case "down" -> new float[]{from[0], 16f - to[2], to[0], 16f - from[2]};
            case "up" -> new float[]{from[0], from[2], to[0], to[2]};
            case "north" -> new float[]{16f - to[0], 16f - to[1], 16f - from[0], 16f - from[1]};
            case "south" -> new float[]{from[0], 16f - to[1], to[0], 16f - from[1]};
            case "west" -> new float[]{from[2], 16f - to[1], to[2], 16f - from[1]};
            case "east" -> new float[]{16f - to[2], 16f - to[1], 16f - from[2], 16f - from[1]};
            default -> null;
        };
    }
}
