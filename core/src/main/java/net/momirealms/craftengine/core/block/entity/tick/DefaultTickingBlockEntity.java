package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

import java.util.Objects;

public final class DefaultTickingBlockEntity<T extends BlockEntityController> implements TickingBlockEntity {
    private final BlockEntity blockEntity;
    private final BlockEntityTicker<T> ticker;
    private final CEChunk chunk;

    public DefaultTickingBlockEntity(CEChunk chunk, BlockEntity blockEntity, BlockEntityTicker<T> ticker) {
        this.blockEntity = Objects.requireNonNull(blockEntity);
        this.ticker = ticker;
        this.chunk = chunk;
    }

    @Override
    public BlockPos pos() {
        return this.blockEntity.pos();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void tick() {
        // 还没加载完全
        if (this.blockEntity.world == null) return;
        BlockPos pos = pos();
        try {
            this.ticker.tick(this.chunk.world(), pos, this.blockEntity.blockState, (T) this.blockEntity.controller);
        } catch (Throwable t) {
            CraftEngine.instance().logger().warn("Failed to tick block entity(" + this.blockEntity.getClass().getSimpleName() + ") at world " + this.chunk.world().name() + " " + pos, t);
        }
    }

    @Override
    public boolean isValid() {
        return this.blockEntity.isValid();
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
