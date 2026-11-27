package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;

public final class BoundBlockEntityTicker<T extends BlockEntityController> implements BlockEntityTicker<T> {
    private final BlockEntityTicker<BlockEntityController> ticker;
    private final BlockEntityController controller;

    public BoundBlockEntityTicker(BlockEntityTicker<BlockEntityController> ticker, BlockEntityController controller) {
        this.ticker = ticker;
        this.controller = controller;
    }

    @Override
    public void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, T controller) {
        this.ticker.tick(world, pos, state, this.controller);
    }

    @Override
    public boolean isSleeping() {
        return this.ticker.isSleeping();
    }

    @Override
    public void setTickingStateListener(BlockEntityTickingStateListener listener) {
        this.ticker.setTickingStateListener(listener);
    }
}
