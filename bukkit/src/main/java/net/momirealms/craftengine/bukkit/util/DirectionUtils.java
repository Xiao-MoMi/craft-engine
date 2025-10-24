package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.block.BlockFace;

public final class DirectionUtils {

    private DirectionUtils() {}

    public static Direction toDirection(BlockFace face) {
        return switch (face) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
            default -> throw new IllegalStateException("Unexpected value: " + face);
        };
    }

    public static BlockFace toBlockFace(Direction direction) {
        return switch (direction) {
            case UP -> BlockFace.UP;
            case DOWN -> BlockFace.DOWN;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
        };
    }

    public static Object toNMSDirection(Direction direction) {
        return switch (direction) {
            case UP -> CoreReflections.Instance.direction$UP;
            case DOWN -> CoreReflections.Instance.direction$DOWN;
            case NORTH -> CoreReflections.Instance.direction$NORTH;
            case SOUTH -> CoreReflections.Instance.direction$SOUTH;
            case WEST -> CoreReflections.Instance.direction$WEST;
            case EAST -> CoreReflections.Instance.direction$EAST;
        };
    }

    public static Direction fromNMSDirection(Object direction) {
        Enum<?> directionEnum = (Enum<?>) direction;
        int index = directionEnum.ordinal();
        return Direction.values()[index];
    }

    public static boolean isYAxis(Object nmsDirection) {
        return nmsDirection == CoreReflections.Instance.direction$UP || nmsDirection == CoreReflections.Instance.direction$DOWN;
    }
}
