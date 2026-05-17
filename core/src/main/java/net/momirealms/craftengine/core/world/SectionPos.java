package net.momirealms.craftengine.core.world;

public final class SectionPos extends Vec3i {
    public SectionPos(int x, int y, int z) {
        super(x, y, z);
    }

    public static int blockToSectionCoord(int coord) {
        return coord >> 4;
    }

    public static SectionPos of(BlockPos pos) {
        return new SectionPos(pos.x() >> 4, pos.y() >> 4, pos.z() >> 4);
    }

    public static int sectionRelative(int rel) {
        return rel & 15;
    }

    public static SectionPos of(long packed) {
        return new SectionPos((int) (packed >> 42), (int) (packed << 44 >> 44), (int) (packed << 22 >> 42));
    }

    public long asLong() {
        return ((long) this.x & 4194303L) << 42 | (long) this.y & 1048575L | ((long) this.z & 4194303L) << 20;
    }

    public ChunkPos asChunkPos() {
        return new ChunkPos(this.x, this.z);
    }

    public static short packSectionRelativePos(BlockPos pos) {
        return (short) ((pos.x & 15) << 8 | (pos.z & 15) << 4 | pos.y & 15);
    }

    public static BlockPos unpackSectionRelativePos(short encoded) {
        int x = (encoded >> 8) & 15;
        int z = (encoded >> 4) & 15;
        int y = encoded & 15;
        return new BlockPos(x, y, z);
    }

    public final int minBlockX() {
        return this.x << 4;
    }

    public final int minBlockY() {
        return this.y << 4;
    }

    public final int minBlockZ() {
        return this.z << 4;
    }
}
