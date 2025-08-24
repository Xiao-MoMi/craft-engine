package net.momirealms.craftengine.bukkit.plugin.network.payload;

import io.netty.buffer.ByteBuf;

public enum NetWorkDataTypes {
    CLIENT_CUSTOM_BLOCK(NetWorkCodecs.INTEGER),
    CANCEL_BLOCK_UPDATE(NetWorkCodecs.BOOLEAN);

    private final NetWorkCodec<?> codec;

    NetWorkDataTypes(NetWorkCodec<?> codec) {
        this.codec = codec;
    }

    public NetWorkCodec<?> codec() {
        return codec;
    }

    @SuppressWarnings("unchecked")
    public <V> V decode(ByteBuf buf) {
        return (V) codec.decode(buf);
    }

    @SuppressWarnings("unchecked")
    public <V> void encode(ByteBuf buf, V value) {
        ((NetWorkCodec<V>) codec).encode(buf, value);
    }
}