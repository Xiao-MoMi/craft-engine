package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.MiscUtils;

public class Vec3d implements Position {
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);
    public final double x;
    public final double y;
    public final double z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d toCenter() {
        return new Vec3d(MiscUtils.fastFloor(x) + 0.5, MiscUtils.fastFloor(y) + 0.5, MiscUtils.fastFloor(z) + 0.5);
    }

    public Vec3d add(Vec3d vec) {
        return new Vec3d(x + vec.x, y + vec.y, z + vec.z);
    }

    public Vec3d add(double x, double y, double z) {
        return new Vec3d(x + this.x, y + this.y, z + this.z);
    }

    public static Vec3d atLowerCornerOf(Vec3i vec) {
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }

    public static Vec3d atLowerCornerWithOffset(Vec3i vec, double deltaX, double deltaY, double deltaZ) {
        return new Vec3d((double) vec.x() + deltaX, (double) vec.y() + deltaY, (double) vec.z() + deltaZ);
    }

    public static Vec3d atCenterOf(Vec3i vec) {
        return atLowerCornerWithOffset(vec, 0.5, 0.5, 0.5);
    }

    public static Vec3d atBottomCenterOf(Vec3i vec) {
        return atLowerCornerWithOffset(vec, 0.5, 0.0, 0.5);
    }

    public static Vec3d upFromBottomCenterOf(Vec3i vec, double deltaY) {
        return atLowerCornerWithOffset(vec, 0.5, deltaY, 0.5);
    }

    public static double distanceToSqr(Vec3d vec1, Vec3d vec2) {
        double dx = vec2.x - vec1.x;
        double dy = vec2.y - vec1.y;
        double dz = vec2.z - vec1.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3d vec3d)) return false;
        return Double.compare(x, vec3d.x) == 0 && Double.compare(y, vec3d.y) == 0 && Double.compare(z, vec3d.z) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        return result;
    }

    @Override
    public String toString() {
        return "Vec3d{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
