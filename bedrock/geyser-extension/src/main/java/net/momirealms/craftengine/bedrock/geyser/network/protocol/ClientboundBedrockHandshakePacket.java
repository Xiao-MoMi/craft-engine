package net.momirealms.craftengine.bedrock.geyser.network.protocol;

import net.momirealms.craftengine.bedrock.geyser.network.ClientCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

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
    public void handle(GeyserPlayer player) {
        if (this.protocolVersion == BedrockProtocol.VERSION) {
            player.send(ServerboundBedrockHandshakePacket.INSTANCE);
        }
    }
}
