package net.momirealms.craftengine.core.plugin.network;

public enum PacketFlow {
    SERVERBOUND("serverbound"), // c2s
    CLIENTBOUND("clientbound"); // s2c
    public final String id;

    PacketFlow(final String id) {
        this.id = id;
    }

    public PacketFlow opposite() {
        return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
    }
}
