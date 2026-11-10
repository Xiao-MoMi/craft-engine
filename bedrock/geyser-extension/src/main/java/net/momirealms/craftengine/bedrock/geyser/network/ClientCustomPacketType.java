package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

public record ClientCustomPacketType<T extends ClientCustomPacket>(String id, PacketCodec<T> codec) {
}
