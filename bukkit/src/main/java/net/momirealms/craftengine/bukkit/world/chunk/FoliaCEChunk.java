package net.momirealms.craftengine.bukkit.world.chunk;

import net.momirealms.craftengine.bukkit.world.FoliaCEWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.*;
import net.momirealms.craftengine.core.util.TickersList;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FoliaCEChunk extends BukkitCEChunk {
    private final TickersList<TickingBlockEntity> tickingBlockEntities = VersionHelper.isFolia ? new TickersList<>() : null;
    private final List<TickingBlockEntity> pendingTickingBlockEntities = VersionHelper.isFolia ? new ArrayList<>() : null;
    private volatile boolean isTickingBlockEntities = false;

    public FoliaCEChunk(CEWorld world, ChunkPos chunkPos) {
        super(world, chunkPos);
    }

    public FoliaCEChunk(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
        super(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
    }

    // folia 将同步和异步的tick的任务合二为一
    @Override
    public void replaceOrCreateTickingBlockEntity(BlockEntity blockEntity) {
        ImmutableBlockState blockState = blockEntity.blockState();
        BlockEntityController controller = blockEntity.controller;
        BlockEntityTicker<BlockEntityController> syncTicker = controller.createBlockEntityTicker(this.world, blockState);
        BlockEntityTicker<BlockEntityController> asyncTicker = controller.createAsyncBlockEntityTicker(this.world, blockState);
        if (syncTicker != null || asyncTicker != null) {
            super.tickingSyncBlockEntitiesByPos.compute(blockEntity.pos(), ((pos, previousTicker) -> {
                TickingBlockEntity newTicker;
                if (syncTicker != null && asyncTicker != null) {
                    newTicker = new CombinedTickingBlockEntity<>(this, blockEntity, syncTicker, asyncTicker);
                } else {
                    newTicker = new DefaultTickingBlockEntity<>(this, blockEntity, Objects.requireNonNullElse(syncTicker, asyncTicker));
                }
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
    }

    @SuppressWarnings("DuplicatedCode")
    public void tickBlockEntities() {
        this.isTickingBlockEntities = true;
        if (!this.pendingTickingBlockEntities.isEmpty()) {
            this.tickingBlockEntities.addAll(this.pendingTickingBlockEntities);
            this.pendingTickingBlockEntities.clear();
        }
        if (!this.tickingBlockEntities.isEmpty()) {
            Object[] entities = this.tickingBlockEntities.elements();
            for (int i = 0, size = this.tickingBlockEntities.size(); i < size; i++) {
                TickingBlockEntity entity = (TickingBlockEntity) entities[i];
                if (entity.isValid()) {
                    entity.tick();
                } else {
                    this.tickingBlockEntities.markAsRemoved(i);
                }
            }
            this.tickingBlockEntities.removeMarkedEntries();
        }
        this.isTickingBlockEntities = false;
    }

    private void addBlockEntityTicker(TickingBlockEntity ticker) {
        if (this.isTickingBlockEntities) {
            this.pendingTickingBlockEntities.add(ticker);
        } else {
            this.tickingBlockEntities.add(ticker);
        }
    }
}
