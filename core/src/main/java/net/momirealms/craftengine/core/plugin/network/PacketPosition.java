package net.momirealms.craftengine.core.plugin.network;

public record PacketPosition(double x, double y, double z) {

    public PacketPosition move(short dx, short dy, short dz) {
        return new PacketPosition(decode(this.x, dx), decode(this.y, dy), decode(this.z, dz));
    }

    private static double decode(double base, short delta) {
        return delta == 0 ? base : (Math.round(base * 4096.0) + delta) / 4096.0;
    }

    public PacketPosition resolve(PacketPosition change, int relatives) {
        return new PacketPosition(
                change.x + ((relatives & 1) != 0 ? this.x : 0),
                change.y + ((relatives & 2) != 0 ? this.y : 0),
                change.z + ((relatives & 4) != 0 ? this.z : 0)
        );
    }

    public double distanceSquared(PacketPosition other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
