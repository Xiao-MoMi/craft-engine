package net.momirealms.craftengine.core.world;

public class ChunkPos {
    public static final long INVALID_CHUNK_POS = asLong(1875066, 1875066);
    public final int x;
    public final int z;
    public final long longKey;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
        this.longKey = asLong(this.x, this.z);
    }

    public static ChunkPos of(final int x, final int z) {
        return new ChunkPos(x, z);
    }

    public ChunkPos(BlockPos pos) {
        this.x = SectionPos.blockToSectionCoord(pos.x());
        this.z = SectionPos.blockToSectionCoord(pos.z());
        this.longKey = asLong(this.x, this.z);
    }

    public ChunkPos(long pos) {
        this.x = (int) pos;
        this.z = (int) (pos >> 32);
        this.longKey = asLong(this.x, this.z);
    }

    @Override
    public String toString() {
        return "ChunkPos{" +
                "x=" + x +
                ", z=" + z +
                ", longKey=" + longKey +
                '}';
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public int regionX() {
        return this.x >> 5;
    }

    public int regionLocalX() {
        return this.x & 31;
    }

    public int regionZ() {
        return this.z >> 5;
    }

    public int regionLocalZ() {
        return this.z & 31;
    }

    public long longKey() {
        return longKey;
    }

    public static long asLong(int chunkX, int chunkZ) {
        return (long) chunkX & 4294967295L | ((long) chunkZ & 4294967295L) << 32;
    }

    @Override
    public final boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ChunkPos chunkPos)) return false;
        return this.longKey == chunkPos.longKey;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.longKey);
    }
}
