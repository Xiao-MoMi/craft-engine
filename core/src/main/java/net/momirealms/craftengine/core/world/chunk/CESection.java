package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.BlockPos;

public class CESection {
    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = SECTION_WIDTH * SECTION_WIDTH * SECTION_HEIGHT;

    private final int sectionY;
    private final PalettedContainer<ImmutableBlockState> statesContainer;

    public CESection(int sectionY, PalettedContainer<ImmutableBlockState> statesContainer) {
        this.sectionY = sectionY;
        this.statesContainer = statesContainer;
    }

    public void setBlockState(BlockPos pos, ImmutableBlockState state) {
        setBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15, state);
    }

    public void setBlockState(int x, int y, int z, ImmutableBlockState state) {
        statesContainer.set((y << 4 | z) << 4 | x, state);
    }

    public void setBlockState(int index, ImmutableBlockState state) {
        statesContainer.set(index, state);
    }

    public ImmutableBlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15);
    }

    public ImmutableBlockState getBlockState(int x, int y, int z) {
        return statesContainer.get((y << 4 | z) << 4 | x);
    }

    public ImmutableBlockState getBlockState(int index) {
        return statesContainer.get(index);
    }

    public PalettedContainer<ImmutableBlockState> statesContainer() {
        return statesContainer;
    }

    public int sectionY() {
        return sectionY;
    }
}
