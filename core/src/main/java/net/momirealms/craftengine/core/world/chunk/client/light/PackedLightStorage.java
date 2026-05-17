package net.momirealms.craftengine.core.world.chunk.client.light;

import java.util.Arrays;

public final class PackedLightStorage implements ClientSectionLightStorage {
    private static final int SIZE = 4096;
    private static final int LONGS = SIZE / 32;
    private final long[] data;

    public PackedLightStorage() {
        this.data = new long[LONGS];
    }

    public PackedLightStorage(int blockType) {
        this.data = new long[LONGS];
        // 将 blockType 重复填充到 long 的所有 2-bit 槽位中
        if (blockType != 0) {
            long fillValue = 0;
            for (int i = 0; i < 32; i++) {
                fillValue |= ((long) blockType << (i << 1));
            }
            Arrays.fill(this.data, fillValue);
        }
    }

    // 0 -> Solid
    // 1 -> Air
    // 2 -> Water
    @Override
    public int blockType(int index) {
        int arrayIndex = index >>> 5; // index / 32
        int bitOffset = (index & 0x1F) << 1; // (index % 32) * 2
        // 提取对应的 2 位并掩码
        return (int) (this.data[arrayIndex] >>> bitOffset) & 0x3;
    }

    @Override
    public boolean isSolid(int index) {
        return blockType(index) == 0;
    }

    @Override
    public boolean isAir(int index) {
        return blockType(index) == 1;
    }

    @Override
    public boolean isWater(int index) {
        return blockType(index) == 2;
    }

    // 设置值
    public void set(int index, int type) {
        int arrayIndex = index >>> 5;
        int bitOffset = (index & 0x1F) << 1;
        long mask = 3L << bitOffset;

        // 清除旧值并写入新值
        this.data[arrayIndex] = (this.data[arrayIndex] & ~mask) | ((long) (type & 0x3) << bitOffset);
    }

    void setSolid(int index) {
        this.set(index, 0);
    }

    void setAir(int index) {
        this.set(index, 1);
    }

    void setWater(int index) {
        this.set(index, 2);
    }
}