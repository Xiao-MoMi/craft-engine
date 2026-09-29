package net.momirealms.craftengine.bukkit.world.chunk;

import net.momirealms.craftengine.bukkit.world.FoliaCEWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTickerScheduler;
import net.momirealms.craftengine.core.block.entity.tick.DefaultTickingBlockEntity;
import net.momirealms.craftengine.core.block.entity.tick.ReplaceableTickingBlockEntity;
import net.momirealms.craftengine.core.block.entity.tick.TickingBlockEntity;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

public final class FoliaCEChunk extends BukkitCEChunk {
    private final BlockEntityTickerScheduler tickingBlockEntities = new BlockEntityTickerScheduler();

    public FoliaCEChunk(CEWorld world, ChunkPos chunkPos) {
        super(world, chunkPos);
    }

    public FoliaCEChunk(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
        super(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
    }

    // folia 同步 ticker 在区域线程执行，异步 ticker 交给世界异步 tick 列表
    @Override
    public void replaceOrCreateTickingBlockEntity(BlockEntity blockEntity) {
        if (!this.activated) return;
        ImmutableBlockState blockState = blockEntity.blockState();
        BlockEntityController controller = blockEntity.controller;
        BlockEntityTicker<BlockEntityController> syncTicker = controller.createBlockEntityTicker(this.world, blockState);
        if (syncTicker != null) {
            super.tickingSyncBlockEntitiesByPos.compute(blockEntity.pos(), ((pos, previousTicker) -> {
                TickingBlockEntity newTicker = new DefaultTickingBlockEntity<>(this, blockEntity, syncTicker);
                if (previousTicker != null) {
                    previousTicker.setTicker(newTicker);
                    return previousTicker;
                } else {
                    ReplaceableTickingBlockEntity replaceableTicker = new ReplaceableTickingBlockEntity(newTicker);
                    this.addBlockEntityTicker(replaceableTicker);
                    return replaceableTicker;
                }
            }));
            FoliaCEWorld foliaWorld = (FoliaCEWorld) this.world;
            foliaWorld.replaceOrCreateTickingChunk(this);
        } else {
            this.removeSyncBlockEntityTicker(blockEntity.pos());
        }
        BlockEntityTicker<BlockEntityController> asyncTicker = controller.createAsyncBlockEntityTicker(this.world, blockState);
        if (asyncTicker != null) {
            super.tickingAsyncBlockEntitiesByPos.compute(blockEntity.pos(), ((pos, previousTicker) -> {
                TickingBlockEntity newTicker = new DefaultTickingBlockEntity<>(this, blockEntity, asyncTicker);
                if (previousTicker != null) {
                    previousTicker.setTicker(newTicker);
                    return previousTicker;
                } else {
                    ReplaceableTickingBlockEntity replaceableTicker = new ReplaceableTickingBlockEntity(newTicker);
                    this.world.addAsyncBlockEntityTicker(replaceableTicker);
                    return replaceableTicker;
                }
            }));
        } else {
            this.removeAsyncBlockEntityTicker(blockEntity.pos());
        }
    }

    public void tickBlockEntities() {
        this.tickingBlockEntities.tick();
    }

    private void addBlockEntityTicker(TickingBlockEntity ticker) {
        this.tickingBlockEntities.add(ticker);
    }
}
