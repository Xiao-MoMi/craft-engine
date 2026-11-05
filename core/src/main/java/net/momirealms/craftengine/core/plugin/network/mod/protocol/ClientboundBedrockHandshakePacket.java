package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record ClientboundBedrockHandshakePacket(int protocolVersion) implements ClientCustomPacket {
    public static final Key ID = Key.of(BedrockProtocol.HANDSHAKE_CHANNEL);
    public static final ClientboundBedrockHandshakePacket INSTANCE = new ClientboundBedrockHandshakePacket(BedrockProtocol.VERSION);
    public static final NetworkCodec<FriendlyByteBuf, ClientboundBedrockHandshakePacket> CODEC = ClientCustomPacket.codec(
            (packet, buf) -> buf.writeInt(packet.protocolVersion),
            buf -> new ClientboundBedrockHandshakePacket(buf.readInt())
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundBedrockHandshakePacket> codec() {
        return CODEC;
    }
}
