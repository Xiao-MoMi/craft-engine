package net.momirealms.craftengine.bukkit.plugin.network;


import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetWorkDataTypes<T> {
    private static final Map<Integer, NetWorkDataTypes<?>> id2NetWorkDataTypes = new HashMap<>();

    public static final NetWorkDataTypes<Integer> CLIENT_CUSTOM_BLOCK =
            new NetWorkDataTypes<>(0, FriendlyByteBuf::readInt, FriendlyByteBuf::writeInt);

    public static final NetWorkDataTypes<Boolean> CANCEL_BLOCK_UPDATE =
            new NetWorkDataTypes<>(1, FriendlyByteBuf::readBoolean, FriendlyByteBuf::writeBoolean);

    static {
        register(CLIENT_CUSTOM_BLOCK);
        register(CANCEL_BLOCK_UPDATE);
    }

    private static void register(NetWorkDataTypes<?> type) {
        id2NetWorkDataTypes.put(type.id, type);
    }

    private final int id;
    private final Function<FriendlyByteBuf, T> decoder;
    private final BiConsumer<FriendlyByteBuf, T> encoder;

    public NetWorkDataTypes(int id, Function<FriendlyByteBuf, T> decoder, BiConsumer<FriendlyByteBuf, T> encoder) {
        this.id = id;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public T decode(FriendlyByteBuf buf) {
        return decoder.apply(buf);
    }

    public void encode(FriendlyByteBuf buf, T data) {
        encoder.accept(buf, data);
    }

    public int id() {
        return id;
    }

    public void writeType(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static NetWorkDataTypes<?> readType(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        return id2NetWorkDataTypes.get(id);
    }

    @SuppressWarnings("unchecked")
    public <R> NetWorkDataTypes<R> as(Class<R> clazz) {
        return (NetWorkDataTypes<R>) this;
    }
}