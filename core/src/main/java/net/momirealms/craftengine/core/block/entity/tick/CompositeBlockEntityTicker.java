package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;

public final class CompositeBlockEntityTicker<T extends BlockEntityController> implements BlockEntityTicker<T> {
    private final BlockEntityTicker<BlockEntityController>[] tickers;
    private final BlockEntityController[] controllers;

    public CompositeBlockEntityTicker(BlockEntityTicker<BlockEntityController>[] tickers, BlockEntityController[] controllers) {
        this.tickers = tickers;
        this.controllers = controllers;
    }

    @Override
    public void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, T controller) {
        for (int i = 0; i < this.tickers.length; i++) {
            BlockEntityTicker<BlockEntityController> ticker = this.tickers[i];
            if (!ticker.isSleeping()) {
                ticker.tick(world, pos, state, this.controllers[i]);
            }
        }
    }

    @Override
    public boolean isSleeping() {
        for (BlockEntityTicker<?> ticker : this.tickers) {
            if (!ticker.isSleeping()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTickingStateListener(BlockEntityTickingStateListener listener) {
        for (BlockEntityTicker<?> ticker : this.tickers) {
            ticker.setTickingStateListener(listener);
        }
    }
}
