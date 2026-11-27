package net.momirealms.craftengine.bedrock.network.codec;

import java.nio.ByteBuffer;
import java.util.function.Function;

public interface PacketCodec<T> {

    byte[] encode(T packet);

    T decode(ByteBuffer buffer);

    static <T> PacketCodec<T> of(Function<T, byte[]> encoder, Function<ByteBuffer, T> decoder) {
        return new PacketCodec<>() {
            @Override
            public byte[] encode(T packet) {
                return encoder.apply(packet);
            }

            @Override
            public T decode(ByteBuffer buffer) {
                return decoder.apply(buffer);
            }
        };
    }
}
