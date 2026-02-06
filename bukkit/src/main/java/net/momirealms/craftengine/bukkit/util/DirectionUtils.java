package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
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
            case UP -> DirectionProxy.UP;
            case DOWN -> DirectionProxy.DOWN;
            case NORTH -> DirectionProxy.NORTH;
            case SOUTH -> DirectionProxy.SOUTH;
            case WEST -> DirectionProxy.WEST;
            case EAST -> DirectionProxy.EAST;
        };
    }

    public static Object toNMSDirection(HorizontalDirection direction) {
        return switch (direction) {
            case NORTH -> DirectionProxy.NORTH;
            case SOUTH -> DirectionProxy.SOUTH;
            case WEST -> DirectionProxy.WEST;
            case EAST -> DirectionProxy.EAST;
        };
    }

    public static Direction fromNMSDirection(Object direction) {
        Enum<?> directionEnum = (Enum<?>) direction;
        int index = directionEnum.ordinal();
        return Direction.values()[index];
    }
}
