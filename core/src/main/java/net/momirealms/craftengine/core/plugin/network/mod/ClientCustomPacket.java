package net.momirealms.craftengine.core.plugin.network.mod;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkDecoder;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkMemberEncoder;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public interface ClientCustomPacket {

    static <B extends ByteBuf, T extends ClientCustomPacket> NetworkCodec<B, T> codec(NetworkMemberEncoder<B, T> networkMemberEncoder, NetworkDecoder<B, T> networkDecoder) {
        return NetworkCodec.ofMember(networkMemberEncoder, networkDecoder);
    }

    Key id();

    NetworkCodec<FriendlyByteBuf, ? extends ClientCustomPacket> codec();

    @SuppressWarnings("unchecked")
    default byte[] encode() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NetworkCodec<FriendlyByteBuf, ClientCustomPacket> codec = (NetworkCodec<FriendlyByteBuf, ClientCustomPacket>) this.codec();
            codec.encode(buffer, this);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    default void handle(NetWorkUser user, ByteBufPacketEvent event) {
    }
}
