package net.momirealms.craftengine.core.world;

public class SectionPos extends Vec3i {

    public SectionPos(int x, int y, int z) {
        super(x, y, z);
    }

    public static int blockToSectionCoord(int coord) {
        return coord >> 4;
    }

    public static SectionPos of(BlockPos pos) {
        return new SectionPos(pos.x() >> 4, pos.y() >> 4, pos.z() >> 4);
    }
}
