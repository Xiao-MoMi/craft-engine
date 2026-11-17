package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.nio.ByteBuffer;

public record ClientboundNativeModelPacket(NativeModelPayload state) implements ClientCustomPacket {
    public static final Key ID = Key.of(NativeModelPayload.CHANNEL);
    public static final NetworkCodec<FriendlyByteBuf, ClientboundNativeModelPacket> CODEC = ClientCustomPacket.codec(
            (packet, buffer) -> buffer.writeBytes(NativeModelPayload.CODEC.encode(packet.state)),
            buffer -> {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                return new ClientboundNativeModelPacket(NativeModelPayload.CODEC.decode(ByteBuffer.wrap(bytes)));
            }
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundNativeModelPacket> codec() {
        return CODEC;
    }
}
