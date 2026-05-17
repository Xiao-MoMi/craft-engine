package net.momirealms.craftengine.core.plugin.network.mod;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkDecoder;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkMemberEncoder;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public interface ModPacket {

    ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type();

    default String permission(PacketFlow flow) {
        Key location = type().location();
        return "ce.mod." + flow.id + "."
                + (Key.CRAFTENGINE_NAMESPACE.equals(location.namespace) ? "" : location.namespace + ".")
                + location.value;
    }

    default void handle(NetWorkUser user) {
    }

    static <B extends ByteBuf, T extends ModPacket> NetworkCodec<B, T> codec(NetworkMemberEncoder<B, T> networkMemberEncoder, NetworkDecoder<B, T> networkDecoder) {
        return NetworkCodec.ofMember(networkMemberEncoder, networkDecoder);
    }
}
