package net.momirealms.craftengine.fabric.client.util;

import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetWorkDataTypes<T> {
    private static final Map<Integer, NetWorkDataTypes<?>> id2NetWorkDataTypes = new HashMap<>();

    public static final NetWorkDataTypes<Integer> CLIENT_CUSTOM_BLOCK =
            new NetWorkDataTypes<>(0, PacketByteBuf::readInt, PacketByteBuf::writeInt);

    public static final NetWorkDataTypes<Boolean> CANCEL_BLOCK_UPDATE =
            new NetWorkDataTypes<>(1, PacketByteBuf::readBoolean, PacketByteBuf::writeBoolean);

    static {
        register(CLIENT_CUSTOM_BLOCK);
        register(CANCEL_BLOCK_UPDATE);
    }

    private static void register(NetWorkDataTypes<?> type) {
        id2NetWorkDataTypes.put(type.id, type);
    }

    private final int id;
    private final Function<PacketByteBuf, T> decoder;
    private final BiConsumer<PacketByteBuf, T> encoder;

    public NetWorkDataTypes(int id, Function<PacketByteBuf, T> decoder, BiConsumer<PacketByteBuf, T> encoder) {
        this.id = id;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public T decode(PacketByteBuf buf) {
        return decoder.apply(buf);
    }

    public void encode(PacketByteBuf buf, T data) {
        encoder.accept(buf, data);
    }

    public int id() {
        return id;
    }

    public void writeType(PacketByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static NetWorkDataTypes<?> readType(PacketByteBuf buf) {
        int id = buf.readVarInt();
        return id2NetWorkDataTypes.get(id);
    }

    @SuppressWarnings("unchecked")
    public <R> NetWorkDataTypes<R> as(Class<R> clazz) {
        return (NetWorkDataTypes<R>) this;
    }
}