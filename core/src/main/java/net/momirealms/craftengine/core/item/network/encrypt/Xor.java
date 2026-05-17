package net.momirealms.craftengine.core.item.network.encrypt;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class Xor implements CryptoAlgorithm {
    private static final VarHandle LONG_VIEW = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private final byte[] keyBytes;
    private final long keyLong;

    public Xor(@NotNull String key) {
        this.keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[8];
        for (int i = 0; i < 8; i++) {
            paddedKey[i] = keyBytes[i % keyBytes.length];
        }
        this.keyLong = (long) LONG_VIEW.get(paddedKey, 0);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public byte[] encrypt(byte[] data) {
        int len = data.length;
        int i = 0;
        int limit = len - 8;
        for (; i <= limit; i += 8) {
            long val = (long) LONG_VIEW.get(data, i);
            LONG_VIEW.set(data, i, val ^ keyLong);
        }
        for (; i < len; i++) {
            data[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return data;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public byte[] decrypt(byte[] data) {
        int len = data.length;
        int i = 0;
        int limit = len - 8;
        for (; i <= limit; i += 8) {
            long val = (long) LONG_VIEW.get(data, i);
            LONG_VIEW.set(data, i, val ^ keyLong);
        }
        for (; i < len; i++) {
            data[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return data;
    }
}
