package net.momirealms.craftengine.core.util;

import java.util.Random;

public enum Rotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    private static final Rotation[][] rotationMap = {
            {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90},
            {Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.NONE},
            {Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.NONE, Rotation.CLOCKWISE_90},
            {Rotation.COUNTERCLOCKWISE_90, Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180}
    };

    public Rotation getRotated(Rotation rotation) {
        int thisIndex = this.ordinal();
        int rotationIndex = rotation.ordinal();
        return rotationMap[thisIndex][rotationIndex];
    }

    public Direction rotate(Direction direction) {
        if (direction.axis() == Direction.Axis.Y) {
            return direction;
        } else {
            return switch (this) {
                case CLOCKWISE_90 -> direction.clockWise();
                case CLOCKWISE_180 -> direction.opposite();
                case COUNTERCLOCKWISE_90 -> direction.counterClockWise();
                default -> direction;
            };
        }
    }

    public int rotate(int rotation, int fullTurn) {
        return switch (this) {
            case CLOCKWISE_90 -> (rotation + fullTurn / 4) % fullTurn;
            case CLOCKWISE_180 -> (rotation + fullTurn / 2) % fullTurn;
            case COUNTERCLOCKWISE_90 -> (rotation + fullTurn * 3 / 4) % fullTurn;
            default -> rotation;
        };
    }

    public static Rotation random(Random random) {
        return values()[random.nextInt(values().length)];
    }
}
