package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MiscUtils;

public class BlockPos extends Vec3i {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);

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

    public static BlockPos fromVec3d(Vec3d vec) {
        return new BlockPos(MiscUtils.floor(vec.x), MiscUtils.floor(vec.y), MiscUtils.floor(vec.z));
    }

    public static BlockPos of(long packedPos) {
        return new BlockPos(unpackX(packedPos), unpackY(packedPos), unpackZ(packedPos));
    }

    public static int unpackX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    public static int unpackY(long packedPos) {
        return (int) (packedPos << 52 >> 52);
    }

    public static int unpackZ(long packedPos) {
        return (int) (packedPos << 26 >> 38);
    }

    public BlockPos relative(Direction direction, int i) {
        return i == 0
                ? this
                : new BlockPos(this.x() + direction.stepX() * i, this.y() + direction.stepY() * i, this.z() + direction.stepZ() * i);
    }

    public BlockPos above() {
        return new BlockPos(this.x(), this.y() + 1, this.z());
    }

    public BlockPos below() {
        return new BlockPos(this.x(), this.y() - 1, this.z());
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

    public BlockPos offset(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new BlockPos(this.x() + x, this.y() + y, this.z() + z);
    }

    public BlockPos immutable() {
        return this;
    }

    @Override
    public BlockPos north() {
        return new BlockPos(this.x(), this.y(), this.z() - 1);
    }

    @Override
    public BlockPos north(int distance) {
        return distance == 0 ? this.immutable() : new BlockPos(this.x(), this.y(), this.z() - distance);
    }

    @Override
    public BlockPos south() {
        return new BlockPos(this.x(), this.y(), this.z() + 1);
    }

    @Override
    public BlockPos south(int distance) {
        return distance == 0 ? this.immutable() : new BlockPos(this.x(), this.y(), this.z() + distance);
    }

    @Override
    public BlockPos west() {
        return new BlockPos(this.x() - 1, this.y(), this.z());
    }

    @Override
    public BlockPos west(int distance) {
        return distance == 0 ? this.immutable() : new BlockPos(this.x() - distance, this.y(), this.z());
    }

    @Override
    public BlockPos east() {
        return new BlockPos(this.x() + 1, this.y(), this.z());
    }

    @Override
    public BlockPos east(int distance) {
        return distance == 0 ? this.immutable() : new BlockPos(this.x() + distance, this.y(), this.z());
    }
}
