package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import io.netty.buffer.ByteBufUtil;
import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record ClientboundBedrockCreativeItemsPacket(boolean last, byte[] stacks) implements ClientCustomPacket {
    public static final Key ID = Key.of(BedrockProtocol.CREATIVE_ITEMS_CHANNEL);
    public static final NetworkCodec<FriendlyByteBuf, ClientboundBedrockCreativeItemsPacket> CODEC = ClientCustomPacket.codec(
            (packet, buf) -> {
                buf.writeBoolean(packet.last);
                buf.writeBytes(packet.stacks);
            },
            buf -> new ClientboundBedrockCreativeItemsPacket(buf.readBoolean(), ByteBufUtil.getBytes(buf))
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundBedrockCreativeItemsPacket> codec() {
        return CODEC;
    }
}
