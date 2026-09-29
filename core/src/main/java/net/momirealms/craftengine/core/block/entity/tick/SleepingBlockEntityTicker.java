package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;

/**
 * An explicitly sleepable ticker. Keep one instance per controller and tick channel,
 * and return it from the controller's ticker factory. Sleeping preserves the delegate;
 * the event that makes work available must call {@link #wakeUp()}.
 * Call sleep, wakeUp and isSleeping on the owning tick thread. This class does not
 * synchronize state or dispatch calls made from another thread.
 */
public final class SleepingBlockEntityTicker<T extends BlockEntityController> implements BlockEntityTicker<T> {
    private final BlockEntityTicker<T> delegate;
    private boolean sleeping;
    private BlockEntityTickingStateListener listener;

    public SleepingBlockEntityTicker(BlockEntityTicker<T> delegate) {
        this.delegate = delegate;
    }

    public void sleep() {
        setSleeping(true);
    }

    public void wakeUp() {
        setSleeping(false);
    }

    private void setSleeping(boolean sleeping) {
        if (this.sleeping == sleeping) return;
        this.sleeping = sleeping;
        BlockEntityTickingStateListener listener = this.listener;
        if (listener != null) listener.run();
    }

    @Override
    public boolean isSleeping() {
        return this.sleeping;
    }

    @Override
    public void setTickingStateListener(BlockEntityTickingStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, T controller) {
        if (!this.sleeping) this.delegate.tick(world, pos, state, controller);
    }
}
