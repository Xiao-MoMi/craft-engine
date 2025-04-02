package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.block.BlockFace;

public class DirectionUtils {

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
            case UP -> Reflections.instance$Direction$UP;
            case DOWN -> Reflections.instance$Direction$DOWN;
            case NORTH -> Reflections.instance$Direction$NORTH;
            case SOUTH -> Reflections.instance$Direction$SOUTH;
            case WEST -> Reflections.instance$Direction$WEST;
            case EAST -> Reflections.instance$Direction$EAST;
        };
    }

    public static Object toNMSOppositeDirection(Direction direction) {
        return switch (direction) {
            case UP -> Reflections.instance$Direction$DOWN;
            case DOWN -> Reflections.instance$Direction$UP;
            case NORTH -> Reflections.instance$Direction$SOUTH;
            case SOUTH -> Reflections.instance$Direction$NORTH;
            case WEST -> Reflections.instance$Direction$EAST;
            case EAST -> Reflections.instance$Direction$WEST;
        };
    }
}
