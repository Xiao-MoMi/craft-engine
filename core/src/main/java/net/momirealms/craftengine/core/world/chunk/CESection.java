package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.BlockPos;
import org.jetbrains.annotations.ApiStatus;

public final class CESection {
    public final int sectionY;
    public final PalettedContainer<ImmutableBlockState> statesContainer;

    public CESection(int sectionY, PalettedContainer<ImmutableBlockState> statesContainer) {
        this.sectionY = sectionY;
        this.statesContainer = statesContainer;
    }

    @ApiStatus.Internal
    public ImmutableBlockState setBlockState(BlockPos pos, ImmutableBlockState state) {
        return this.setBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15, state);
    }

    @ApiStatus.Internal
    public ImmutableBlockState setBlockState(int x, int y, int z, ImmutableBlockState state) {
        return this.setBlockState((y << 4 | z) << 4 | x, state);
    }

    @ApiStatus.Internal
    public ImmutableBlockState setBlockState(int index, ImmutableBlockState state) {
        return this.statesContainer.getAndSet(index, state);
    }

    @ApiStatus.Internal
    public ImmutableBlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.x() & 15, pos.y() & 15, pos.z() & 15);
    }

    @ApiStatus.Internal
    public ImmutableBlockState getBlockState(int x, int y, int z) {
        return statesContainer.get((y << 4 | z) << 4 | x);
    }

    @ApiStatus.Internal
    public ImmutableBlockState getBlockState(int index) {
        return this.statesContainer.get(index);
    }

    @ApiStatus.Internal
    public PalettedContainer<ImmutableBlockState> statesContainer() {
        return this.statesContainer;
    }

    public int sectionY() {
        return this.sectionY;
    }
}
