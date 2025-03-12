/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
}
