package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.mod.ServerCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record ServerboundBedrockHandshakePacket(int protocolVersion) implements ServerCustomPacket {
    public static final Key ID = Key.of(BedrockProtocol.HANDSHAKE_CHANNEL);
    public static final NetworkCodec<FriendlyByteBuf, ServerboundBedrockHandshakePacket> CODEC = ServerCustomPacket.codec(
            (packet, buf) -> buf.writeInt(packet.protocolVersion),
            buf -> new ServerboundBedrockHandshakePacket(buf.readInt())
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ServerboundBedrockHandshakePacket> codec() {
        return CODEC;
    }

    @Override
    public void handle(NetWorkUser user, ByteBufPacketEvent event) {
        event.setCancelled(true);
        if (this.protocolVersion == BedrockProtocol.VERSION) {
            user.setBedrockProtocol(this.protocolVersion);
        }
    }
}
