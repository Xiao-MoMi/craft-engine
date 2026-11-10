package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.network.ClientCustomPacket;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;
import org.geysermc.geyser.session.GeyserSession;

public record ClientboundBedrockHandshakePacket(int protocolVersion) implements ClientCustomPacket {
    public static final String ID = BedrockProtocol.HANDSHAKE_CHANNEL;
    public static final PacketCodec<ClientboundBedrockHandshakePacket> CODEC = PacketCodec.of(
            packet -> BedrockProtocol.handshake(packet.protocolVersion),
            buffer -> new ClientboundBedrockHandshakePacket(BedrockProtocol.readHandshake(buffer))
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void handle(GeyserSession session) {
        if (this.protocolVersion == BedrockProtocol.VERSION) {
            CraftEngineGeyserExtension.instance().customPayloads().send(session, ServerboundBedrockHandshakePacket.INSTANCE);
        }
    }
}
