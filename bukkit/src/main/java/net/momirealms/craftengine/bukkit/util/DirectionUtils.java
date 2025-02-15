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
}
