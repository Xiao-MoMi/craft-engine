package net.momirealms.craftengine.core.util;

public enum Mirror {
    NONE,
    LEFT_RIGHT,
    FRONT_BACK;

    public int mirror(int rotation, int fullTurn) {
        int i = fullTurn / 2;
        int j = rotation > i ? rotation - fullTurn : rotation;
        return switch (this) {
            case LEFT_RIGHT -> (i - j + fullTurn) % fullTurn;
            case FRONT_BACK -> (fullTurn - j) % fullTurn;
            default -> rotation;
        };
    }

    public Rotation getRotation(Direction direction) {
        Direction.Axis axis = direction.axis();
        return (this != LEFT_RIGHT || axis != Direction.Axis.Z) && (this != FRONT_BACK || axis != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    public Direction mirror(Direction direction) {
        if (this == FRONT_BACK && direction.axis() == Direction.Axis.X) {
            return direction.opposite();
        } else {
            return this == LEFT_RIGHT && direction.axis() == Direction.Axis.Z ? direction.opposite() : direction;
        }
    }
}
