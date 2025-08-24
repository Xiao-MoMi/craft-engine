package net.momirealms.craftengine.core.world;

public class WorldPosition implements Position {
    private final World world;
    private final double x;
    private final double y;
    private final double z;
    private final float xRot;
    private final float yRot;

    public WorldPosition(World world, Position position) {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorldPosition other)) return false;
        return Double.compare(other.x, x) == 0 &&
               Double.compare(other.y, y) == 0 &&
               Double.compare(other.z, z) == 0 &&
               Float.compare(other.xRot, xRot) == 0 &&
               Float.compare(other.yRot, yRot) == 0 &&
               world.equals(other.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        long temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + Float.floatToIntBits(xRot);
        result = 31 * result + Float.floatToIntBits(yRot);
        return result;
    }
}
