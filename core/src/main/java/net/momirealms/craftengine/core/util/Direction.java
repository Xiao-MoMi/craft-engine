package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.world.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Predicate;

public enum Direction {
    DOWN(0, 1, -1, AxisDirection.NEGATIVE, Axis.Y, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, AxisDirection.POSITIVE, Axis.Y, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, AxisDirection.NEGATIVE, Axis.Z, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, AxisDirection.POSITIVE, Axis.Z, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, AxisDirection.NEGATIVE, Axis.X, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, AxisDirection.POSITIVE, Axis.X, new Vec3i(1, 0, 0));

    private static final Direction[] VALUES = values();
    private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES)
            .sorted(Comparator.comparingInt(direction -> direction.data3d))
            .toArray(Direction[]::new);
    private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
            .filter(direction -> direction.axis().isHorizontal())
            .sorted(Comparator.comparingInt(direction -> direction.data2d))
            .toArray(Direction[]::new);

    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;
    private final Axis axis;
    private final AxisDirection axisDirection;
    private final Vec3i vec;
    private final int adjX;
    private final int adjY;
    private final int adjZ;

    Direction(
            final int id,
            final int idOpposite,
            final int idHorizontal,
            final AxisDirection direction,
            final Axis axis,
            final Vec3i vector
    ) {
        this.data3d = id;
        this.data2d = idHorizontal;
        this.oppositeIndex = idOpposite;
        this.axis = axis;
        this.axisDirection = direction;
        this.vec = vector;
        this.adjX = vector.x();
        this.adjY = vector.y();
        this.adjZ = vector.z();
    }

    public static Direction fromYaw(float yaw) {
        yaw = normalizeAngle(yaw);
        if (yaw < 45) {
            if (yaw > -45) {
                return NORTH;
            } else if (yaw > -135) {
                return EAST;
            } else {
                return SOUTH;
            }
        } else {
            if (yaw < 135) {
                return WEST;
            } else {
                return SOUTH;
            }
        }
    }

    private static float normalizeAngle(float angle) {
        angle %= 360;
        angle = (angle + 360) % 360;
        if (angle > 180) {
            angle -= 360;
        }
        return angle;
    }

    public HorizontalDirection toHorizontalDirection() {
        return switch (this) {
            case DOWN, UP -> null;
            case NORTH -> HorizontalDirection.NORTH;
            case SOUTH -> HorizontalDirection.SOUTH;
            case WEST -> HorizontalDirection.WEST;
            case EAST -> HorizontalDirection.EAST;
        };
    }

    public static Direction[] orderedByNearest(AbstractEntity entity) {
        float xRotation = entity.getXRot() * (float) (Math.PI / 180.0);
        float yRotation = -entity.getYRot() * (float) (Math.PI / 180.0);
        float sinX = (float) Math.sin(xRotation);
        float cosX = (float) Math.cos(xRotation);
        float sinY = (float) Math.sin(yRotation);
        float cosY = (float) Math.cos(yRotation);
        boolean isFacingPositiveY = sinY > 0.0F;
        boolean isFacingNegativeX = sinX < 0.0F;
        boolean isFacingPositiveX = cosY > 0.0F;
        float adjustedSinY = isFacingPositiveY ? sinY : -sinY;
        float adjustedSinX = isFacingNegativeX ? -sinX : sinX;
        float adjustedCosY = isFacingPositiveX ? cosY : -cosY;
        float horizontalProjection = adjustedSinY * cosX;
        float verticalProjection = adjustedCosY * cosX;
        Direction horizontalDirection = isFacingPositiveY ? EAST : WEST;
        Direction verticalDirection = isFacingNegativeX ? UP : DOWN;
        Direction depthDirection = isFacingPositiveX ? SOUTH : NORTH;
        if (adjustedSinY > adjustedCosY) {
            if (adjustedSinX > horizontalProjection) {
                return createDirectionArray(verticalDirection, horizontalDirection, depthDirection);
            } else {
                return verticalProjection > adjustedSinX
                        ? createDirectionArray(horizontalDirection, depthDirection, verticalDirection)
                        : createDirectionArray(horizontalDirection, verticalDirection, depthDirection);
            }
        } else if (adjustedSinX > verticalProjection) {
            return createDirectionArray(verticalDirection, depthDirection, horizontalDirection);
        } else {
            return horizontalProjection > adjustedSinX
                    ? createDirectionArray(depthDirection, horizontalDirection, verticalDirection)
                    : createDirectionArray(depthDirection, verticalDirection, horizontalDirection);
        }
    }

    private static Direction[] createDirectionArray(Direction first, Direction second, Direction third) {
        return new Direction[]{first, second, third, third.opposite(), second.opposite(), first.opposite()};
    }

    public static float getYaw(Direction direction) {
        switch (direction) {
            case NORTH -> {
                return 180f;
            }
            case SOUTH -> {
                return 0f;
            }
            case WEST -> {
                return 90f;
            }
            case EAST -> {
                return -90f;
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public int stepX() {
        return this.adjX;
    }

    public int stepY() {
        return this.adjY;
    }

    public int stepZ() {
        return this.adjZ;
    }

    public Vec3i vector() {
        return vec;
    }

    public Axis axis() {
        return this.axis;
    }

    public AxisDirection axisDirection() {
        return axisDirection;
    }

    public int data2d() {
        return data2d;
    }

    public int data3d() {
        return data3d;
    }

    public int oppositeIndex() {
        return oppositeIndex;
    }

    public Direction opposite() {
        return VALUES[oppositeIndex];
    }

    public Direction clockWise() {
        return switch (this) {
            case NORTH -> EAST;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            case EAST -> SOUTH;
            default -> throw new IllegalStateException();
        };
    }

    public Direction counterClockWise() {
        return switch (this) {
            case NORTH -> WEST;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            case EAST -> NORTH;
            default -> throw new IllegalStateException();
        };
    }

    public static Direction getApproximateNearest(double x, double y, double z) {
        Direction nearestDirection = null;
        double maxDotProduct = -Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            double dotProduct = x * direction.vec.x() +
                    y * direction.vec.y() +
                    z * direction.vec.z();
            if (dotProduct > maxDotProduct) {
                maxDotProduct = dotProduct;
                nearestDirection = direction;
            }
        }
        return nearestDirection;
    }

    public static Direction from3DDataValue(int id) {
        return BY_3D_DATA[Math.abs(id % BY_3D_DATA.length)];
    }

    public static Direction from2DDataValue(int value) {
        return BY_2D_DATA[Math.abs(value % BY_2D_DATA.length)];
    }

    public static Direction fromAxisAndDirection(Axis axis, AxisDirection direction) {
        return switch (axis) {
            case X -> direction == AxisDirection.POSITIVE ? EAST : WEST;
            case Y -> direction == AxisDirection.POSITIVE ? UP : DOWN;
            case Z -> direction == AxisDirection.POSITIVE ? SOUTH : NORTH;
        };
    }

    public enum Axis implements Predicate<Direction> {
        X() {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }

            @Override
            public Direction getPositive() {
                return Direction.EAST;
            }

            @Override
            public Direction getNegative() {
                return Direction.WEST;
            }
        },
        Y() {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }

            @Override
            public Direction getPositive() {
                return Direction.UP;
            }

            @Override
            public Direction getNegative() {
                return Direction.DOWN;
            }
        },
        Z() {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }

            @Override
            public Direction getPositive() {
                return Direction.SOUTH;
            }

            @Override
            public Direction getNegative() {
                return Direction.NORTH;
            }
        };

        public static final Axis[] VALUES = values();

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        public abstract Direction getPositive();

        public abstract Direction getNegative();

        public Direction[] getDirections() {
            return new Direction[]{this.getPositive(), this.getNegative()};
        }

        public static Axis random(Random random) {
            return values()[random.nextInt(VALUES.length)];
        }

        @Override
        public boolean test(@Nullable Direction direction) {
            return direction != null && direction.axis() == this;
        }

        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    public enum AxisDirection {
        POSITIVE(1),
        NEGATIVE(-1);

        private final int step;

        AxisDirection(final int offset) {
            this.step = offset;
        }

        public int step() {
            return this.step;
        }

        public AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }
}
