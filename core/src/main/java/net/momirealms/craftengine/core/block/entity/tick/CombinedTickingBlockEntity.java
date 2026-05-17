package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

import java.util.Objects;

@SuppressWarnings("DuplicatedCode")
public final class CombinedTickingBlockEntity<T extends BlockEntityController> implements TickingBlockEntity {
    private final BlockEntity blockEntity;
    private final BlockEntityTicker<T> ticker1;
    private final BlockEntityTicker<T> ticker2;
    private final CEChunk chunk;

    public CombinedTickingBlockEntity(CEChunk chunk, BlockEntity blockEntity, BlockEntityTicker<T> ticker1, BlockEntityTicker<T> ticker2) {
        this.blockEntity = Objects.requireNonNull(blockEntity);
        this.ticker1 = ticker1;
        this.ticker2 = ticker2;
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
        ImmutableBlockState state = this.chunk.getBlockState(pos);
        // 不是合法方块
        if (!this.blockEntity.isValidBlockState(state)) {
            this.chunk.removeBlockEntity(pos);
            Debugger.BLOCK.warn(() -> "Invalid block entity(" + this.blockEntity.getClass().getSimpleName() + ") with state " + state + " found at world " + this.chunk.world().name() + " " + pos, null);
            return;
        }
        try {
            CEWorld world = this.chunk.world();
            this.ticker1.tick(world, pos, state, (T) this.blockEntity.controller);
            this.ticker2.tick(world, pos, state, (T) this.blockEntity.controller);
        } catch (Throwable t) {
            CraftEngine.instance().logger().warn("Failed to tick block entity(" + this.blockEntity.getClass().getSimpleName() + ") at world " + this.chunk.world().name() + " " + pos, t);
        }
    }

    @Override
    public boolean isValid() {
        return this.blockEntity.isValid();
    }
}
