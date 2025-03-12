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

import net.momirealms.craftengine.core.util.Direction;

public class BlockPos extends Vec3i {

    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public BlockPos relative(Direction direction) {
        return switch (direction) {
            case UP -> new BlockPos(this.x(), this.y() + 1, this.z());
            case DOWN -> new BlockPos(this.x(), this.y() - 1, this.z());
            case NORTH -> new BlockPos(this.x(), this.y(), this.z() - 1);
            case SOUTH -> new BlockPos(this.x(), this.y(), this.z() + 1);
            case WEST -> new BlockPos(this.x() - 1, this.y(), this.z());
            case EAST -> new BlockPos(this.x() + 1, this.y(), this.z());
        };
    }

    public static BlockPos of(long packedPos) {
        return new BlockPos((int) (packedPos >> 38), (int) ((packedPos << 52) >> 52), (int) ((packedPos << 26) >> 38));
    }

    public BlockPos relative(Direction direction, int i) {
        return i == 0
                ? this
                : new BlockPos(this.x() + direction.stepX() * i, this.y() + direction.stepY() * i, this.z() + direction.stepZ() * i);
    }

    public int toSectionBlockIndex() {
        return (y & 15) << 8 | (z & 15) << 4 | x & 15;
    }

    public long asLong() {
        return asLong(this.x(), this.y(), this.z());
    }

    public static long asLong(int x, int y, int z) {
        return (((long) x & (long) 67108863) << 38) | (((long) y & (long) 4095)) | (((long) z & (long) 67108863) << 12);
    }
}
