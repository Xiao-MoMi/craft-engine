package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
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
    private final boolean checkChunkTicking;
    private boolean loggedInvalidBlockState;

    public DefaultTickingBlockEntity(CEChunk chunk, BlockEntity blockEntity, BlockEntityTicker<T> ticker) {
        this(chunk, blockEntity, ticker, true);
    }

    public DefaultTickingBlockEntity(CEChunk chunk, BlockEntity blockEntity, BlockEntityTicker<T> ticker, boolean checkChunkTicking) {
        this.blockEntity = Objects.requireNonNull(blockEntity);
        this.ticker = ticker;
        this.chunk = chunk;
        this.checkChunkTicking = checkChunkTicking;
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
        if (this.checkChunkTicking && !this.chunk.isBlockTicking()) return;
        BlockPos pos = pos();
        try {
            ImmutableBlockState state = this.blockEntity.blockState();
            if (!this.blockEntity.isValidForTick()) {
                if (!this.loggedInvalidBlockState) {
                    this.loggedInvalidBlockState = true;
                    CraftEngine.instance().logger().warn("Block entity state " + state + " is invalid for ticking at world " + this.chunk.world().name() + " " + pos);
                }
                return;
            }
            this.loggedInvalidBlockState = false;
            this.ticker.tick(this.chunk.world(), pos, state, (T) this.blockEntity.controller);
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
