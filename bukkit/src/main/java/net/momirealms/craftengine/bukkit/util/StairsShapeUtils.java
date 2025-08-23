package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.block.state.properties.StairsShape;

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
            case STRAIGHT -> CoreReflections.instance$StairsShape$STRAIGHT;
            case INNER_LEFT -> CoreReflections.instance$StairsShape$INNER_LEFT;
            case INNER_RIGHT -> CoreReflections.instance$StairsShape$INNER_RIGHT;
            case OUTER_LEFT -> CoreReflections.instance$StairsShape$OUTER_LEFT;
            case OUTER_RIGHT -> CoreReflections.instance$StairsShape$OUTER_RIGHT;
        };
    }
}
