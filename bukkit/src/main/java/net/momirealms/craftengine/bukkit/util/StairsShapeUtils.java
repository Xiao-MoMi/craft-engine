package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.block.properties.type.StairsShape;

public final class StairsShapeUtils {
    private StairsShapeUtils() {}

    public static StairsShape fromNMSStairsShape(Object shape) {
        try {
            int index = (int) CoreReflections.method$StairsShape$ordinal.invoke(shape);
            return StairsShape.values()[index];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toNMSStairsShape(StairsShape shape) {
        return switch (shape) {
            case STRAIGHT -> CoreReflections.Instance.stairsShape$STRAIGHT;
            case INNER_LEFT -> CoreReflections.Instance.stairsShape$INNER_LEFT;
            case INNER_RIGHT -> CoreReflections.Instance.stairsShape$INNER_RIGHT;
            case OUTER_LEFT -> CoreReflections.Instance.stairsShape$OUTER_LEFT;
            case OUTER_RIGHT -> CoreReflections.Instance.stairsShape$OUTER_RIGHT;
        };
    }
}
