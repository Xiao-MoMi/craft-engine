package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;

public final class DualBlockEntityTicker<T extends BlockEntityController> implements BlockEntityTicker<T> {
    private final BlockEntityTicker<BlockEntityController> firstTicker;
    private final BlockEntityController firstController;
    private final BlockEntityTicker<BlockEntityController> secondTicker;
    private final BlockEntityController secondController;

    public DualBlockEntityTicker(BlockEntityTicker<BlockEntityController> firstTicker, BlockEntityController firstController,
                                BlockEntityTicker<BlockEntityController> secondTicker, BlockEntityController secondController) {
        this.firstTicker = firstTicker;
        this.firstController = firstController;
        this.secondTicker = secondTicker;
        this.secondController = secondController;
    }

    @Override
    public void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, T controller) {
        if (!this.firstTicker.isSleeping()) this.firstTicker.tick(world, pos, state, this.firstController);
        if (!this.secondTicker.isSleeping()) this.secondTicker.tick(world, pos, state, this.secondController);
    }

    @Override
    public boolean isSleeping() {
        return this.firstTicker.isSleeping() && this.secondTicker.isSleeping();
    }

    @Override
    public void setTickingStateListener(BlockEntityTickingStateListener listener) {
        this.firstTicker.setTickingStateListener(listener);
        this.secondTicker.setTickingStateListener(listener);
    }
}
