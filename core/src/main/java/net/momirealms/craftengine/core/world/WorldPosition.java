package net.momirealms.craftengine.core.world;

import java.util.Objects;

public class WorldPosition implements Position {
    public final World world;
    public final double x;
    public final double y;
    public final double z;
    public final float xRot;
    public final float yRot;

    public WorldPosition(World world, Position position) {
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.world = world;
        this.xRot = 0f;
        this.yRot = 0f;
    }

    public WorldPosition(World world, Vec3i position) {
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.world = world;
        this.xRot = 0f;
        this.yRot = 0f;
    }

    public WorldPosition(World world, Position position, float xRot, float yRot) {
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.world = world;
        this.xRot = xRot;
        this.yRot = yRot;
    }

    public WorldPosition(World world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = 0f;
        this.yRot = 0f;
    }

    public WorldPosition(World world, double x, double y, double z, float xRot, float yRot) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
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

    public World world() {
        return world;
    }

    public float xRot() {
        return xRot;
    }

    public float yRot() {
        return yRot;
    }

    public Vec3d toVec3d() {
        return new Vec3d(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldPosition that = (WorldPosition) o;
        return Double.compare(that.x, this.x) == 0 &&
                Double.compare(that.y, this.y) == 0 &&
                Double.compare(that.z, this.z) == 0 &&
                Float.compare(that.xRot, this.xRot) == 0 &&
                Float.compare(that.yRot, this.yRot) == 0 &&
                Objects.equals(this.world, that.world);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.world);
        result = 31 * result + Double.hashCode(this.x);
        result = 31 * result + Double.hashCode(this.y);
        result = 31 * result + Double.hashCode(this.z);
        result = 31 * result + Float.floatToIntBits(this.xRot);
        result = 31 * result + Float.floatToIntBits(this.yRot);
        return result;
    }
}
