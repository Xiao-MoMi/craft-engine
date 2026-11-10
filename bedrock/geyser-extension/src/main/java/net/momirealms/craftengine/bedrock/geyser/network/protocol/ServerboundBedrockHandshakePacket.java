package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.network.ServerCustomPacket;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

public record ServerboundBedrockHandshakePacket(int protocolVersion) implements ServerCustomPacket {
    public static final String ID = BedrockProtocol.HANDSHAKE_CHANNEL;
    public static final ServerboundBedrockHandshakePacket INSTANCE = new ServerboundBedrockHandshakePacket(BedrockProtocol.VERSION);
    public static final PacketCodec<ServerboundBedrockHandshakePacket> CODEC = PacketCodec.of(
            packet -> BedrockProtocol.handshake(packet.protocolVersion),
            buffer -> new ServerboundBedrockHandshakePacket(BedrockProtocol.readHandshake(buffer))
    );

    @Override
    public String id() {
        return ID;
    }
}
