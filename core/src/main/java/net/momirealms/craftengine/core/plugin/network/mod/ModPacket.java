package net.momirealms.craftengine.core.plugin.network.mod;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkDecoder;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkMemberEncoder;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.ResourceKey;

public interface ModPacket {

    ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type();

    default void handle(NetWorkUser user) {
    }

    static <B extends ByteBuf, T extends ModPacket> NetworkCodec<B, T> codec(NetworkMemberEncoder<B, T> networkMemberEncoder, NetworkDecoder<B, T> networkDecoder) {
        return NetworkCodec.ofMember(networkMemberEncoder, networkDecoder);
    }
}
