package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Direction;

public class Vec3i implements Comparable<Vec3i> {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    public final int x;
    public final int y;
    public final int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i(int all) {
        this.x = all;
        this.y = all;
        this.z = all;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public Vec3i offset(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new Vec3i(this.x() + x, this.y() + y, this.z() + z);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Vec3i vec3i && this.x == vec3i.x && this.y == vec3i.y && this.z == vec3i.z;
    }

    @Override
    public int hashCode() {
        return (this.y + this.z * 31) * 31 + this.x;
    }

    @Override
    public String toString() {
        return "Vec3i{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public Vec3i relative(Direction direction) {
        return this.relative(direction, 1);
    }

    public Vec3i relative(Direction direction, int distance) {
        return distance == 0
                ? this
                : new Vec3i(
                this.x() + direction.stepX() * distance, this.y() + direction.stepY() * distance, this.z() + direction.stepZ() * distance
        );
    }

    public Vec3i multiply(int scale) {
        if (scale == 1) {
            return this;
        } else {
            return scale == 0 ? ZERO : new Vec3i(this.x() * scale, this.y() * scale, this.z() * scale);
        }
    }

    @Override
    public int compareTo(Vec3i vec3i) {
        if (this.y() == vec3i.y()) {
            return this.z() == vec3i.z() ? this.x() - vec3i.x() : this.z() - vec3i.z();
        } else {
            return this.y() - vec3i.y();
        }
    }

    public Vec3i north() {
        return this.north(1);
    }

    public Vec3i north(int distance) {
        return this.relative(Direction.NORTH, distance);
    }

    public Vec3i south() {
        return this.south(1);
    }

    public Vec3i south(int distance) {
        return this.relative(Direction.SOUTH, distance);
    }

    public Vec3i west() {
        return this.west(1);
    }

    public Vec3i west(int distance) {
        return this.relative(Direction.WEST, distance);
    }

    public Vec3i east() {
        return this.east(1);
    }

    public Vec3i east(int distance) {
        return this.relative(Direction.EAST, distance);
    }

}
