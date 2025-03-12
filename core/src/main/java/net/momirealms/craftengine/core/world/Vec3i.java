package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Direction;

public class Vec3i implements Comparable<Vec3i> {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    protected int x;
    protected int y;
    protected int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    protected Vec3i setX(int x) {
        this.x = x;
        return this;
    }

    protected Vec3i setY(int y) {
        this.y = y;
        return this;
    }

    protected Vec3i setZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Vec3i vec3i && this.x() == vec3i.x() && this.y() == vec3i.y() && this.z() == vec3i.z();
    }

    @Override
    public int hashCode() {
        return (this.y() + this.z() * 31) * 31 + this.x();
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
}
