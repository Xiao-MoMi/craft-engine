package net.momirealms.craftengine.core.world.chunk.client;

import java.util.Arrays;

public final class PackedOcclusionStorage implements ClientSectionOcclusionStorage {
    private static final int SIZE = 4096;
    private static final int LONGS = SIZE / 64;
    private final long[] data;

    public PackedOcclusionStorage() {
        this.data = new long[LONGS];
    }

    public PackedOcclusionStorage(boolean defaultValue) {
        this.data = new long[LONGS];
        if (defaultValue) {
            Arrays.fill(this.data, -1L); // 所有位设为1
        }
    }

    @Override
    public boolean isOccluding(int index) {
        int arrayIndex = index >>> 6; // index / 64
        int bitIndex = index & 0x3F;  // index % 64
        return (this.data[arrayIndex] & (1L << bitIndex)) != 0;
    }

    public void set(int index, boolean occlusion) {
        int arrayIndex = index >>> 6;  // index / 64
        int bitIndex = index & 0x3F;   // index % 64
        if (occlusion) {
            this.data[arrayIndex] |= (1L << bitIndex);
        } else {
            this.data[arrayIndex] &= ~(1L << bitIndex);
        }
    }
}