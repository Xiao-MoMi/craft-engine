package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.MiscUtils;

public final class MutableVec3d implements Position {
    public double x;
    public double y;
    public double z;

    public MutableVec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVec3d toCenter() {
        this.x = MiscUtils.floor(x) + 0.5;
        this.y = MiscUtils.floor(y) + 0.5;
        this.z = MiscUtils.floor(z) + 0.5;
        return this;
    }

    public MutableVec3d add(MutableVec3d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        return this;
    }

    public MutableVec3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public MutableVec3d divide(MutableVec3d vec3d) {
        this.x /= vec3d.x;
        this.z /= vec3d.z;
        this.y /= vec3d.y;
        return this;
    }

    public MutableVec3d normalize() {
        double mag = Math.sqrt(x * x + y * y + z * z);
        this.x /= mag;
        this.y /= mag;
        this.z /= mag;
        return this;
    }

    public static double distanceToSqr(MutableVec3d vec1, MutableVec3d vec2) {
        double dx = vec2.x - vec1.x;
        double dy = vec2.y - vec1.y;
        double dz = vec2.z - vec1.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(MutableVec3d vec3d, double x, double y, double z) {
        this.x += (vec3d.x + x);
        this.y += (vec3d.y + y);
        this.z += (vec3d.z + z);
    }

    public void add(Vec3d vec3d, double x, double y, double z) {
        this.x += (vec3d.x + x);
        this.y += (vec3d.y + y);
        this.z += (vec3d.z + z);
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
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
        if (!(o instanceof MutableVec3d vec3d)) return false;
        return this.x == vec3d.x && this.y == vec3d.y && this.z == vec3d.z;
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
