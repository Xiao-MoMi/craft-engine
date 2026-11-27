package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

public record ServerCustomPacketType<T extends ServerCustomPacket>(String id, Class<T> packetClass, PacketCodec<T> codec) {

    public byte[] encode(ServerCustomPacket packet) {
        return this.codec.encode(this.packetClass.cast(packet));
    }
}
