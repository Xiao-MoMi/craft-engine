package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.network.ServerCustomPacket;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;
import org.geysermc.geyser.network.bedrock.GameProtocol;

public record ServerboundBedrockHandshakePacket(int protocolVersion, int javaProtocol) implements ServerCustomPacket {
    public static final String ID = BedrockProtocol.HANDSHAKE_CHANNEL;
    public static final ServerboundBedrockHandshakePacket WITHOUT_CREATIVE_ITEMS = new ServerboundBedrockHandshakePacket(BedrockProtocol.VERSION, -1);
    public static final ServerboundBedrockHandshakePacket WITH_CREATIVE_ITEMS = new ServerboundBedrockHandshakePacket(BedrockProtocol.VERSION, GameProtocol.getJavaProtocolVersion());
    public static final PacketCodec<ServerboundBedrockHandshakePacket> CODEC = PacketCodec.of(
            packet -> BedrockProtocol.handshake(packet.protocolVersion, packet.javaProtocol),
            buffer -> new ServerboundBedrockHandshakePacket(buffer.getInt(), buffer.getInt())
    );

    @Override
    public String id() {
        return ID;
    }
}
