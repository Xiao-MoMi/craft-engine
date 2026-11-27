package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public final class ClientboundNativeModelPacket implements ClientCustomPacket {
    public static final Key ID = Key.of(NativeModelPayload.CHANNEL);
    public static final NetworkCodec<FriendlyByteBuf, ClientboundNativeModelPacket> CODEC = ClientCustomPacket.codec(
            (packet, buffer) -> buffer.writeBytes(packet.bytes),
            buffer -> {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                return new ClientboundNativeModelPacket(bytes);
            }
    );
    private final byte[] bytes;

    public ClientboundNativeModelPacket(NativeModelPayload state) {
        this(NativeModelPayload.CODEC.encode(state));
    }

    private ClientboundNativeModelPacket(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public byte[] encode() {
        return this.bytes;
    }

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundNativeModelPacket> codec() {
        return CODEC;
    }
}
