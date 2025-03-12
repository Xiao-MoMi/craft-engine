package net.momirealms.craftengine.core.util;

public enum HorizontalDirection {
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public Direction toDirection() {
        return switch (this) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }
}
