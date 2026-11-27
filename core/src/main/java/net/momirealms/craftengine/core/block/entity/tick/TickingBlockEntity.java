package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.world.BlockPos;

public interface TickingBlockEntity {

    void tick();

    boolean isValid();

    BlockPos pos();

    default boolean isSleeping() {
        return false;
    }

    default void setTickingStateListener(BlockEntityTickingStateListener listener) {
    }
}
