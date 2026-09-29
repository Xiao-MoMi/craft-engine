package net.momirealms.craftengine.core.block.entity;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.ChunkLoadSubscriptions;
import net.momirealms.craftengine.core.world.chunk.ChunkSubscription;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public final class BlockEntity {
    public final BlockPos pos;
    public final DynamicBlockEntityRenderer renderer;
    public ImmutableBlockState blockState;
    public CEWorld world;
    public BlockEntityController controller;
    private boolean valid;
    private final Holder<BlockDefinition> blockOwner;
    private boolean validForTick;
    private ChunkLoadSubscriptions.Owner chunkLoadSubscriptions;

    public BlockEntity(BlockPos pos, ImmutableBlockState blockState) {
        this.pos = pos;
        this.blockOwner = blockState.owner();
        this.blockState = blockState;
        this.validForTick = isValidBlockState(blockState);
        this.controller = ((EntityBlock) blockState.behavior()).createBlockEntityController(this);
        if (this.controller.hasElement()) {
            List<BlockEntityElement> elements = new ArrayList<>(4);
            this.controller.gatherElements(elements::add);
            this.renderer = new DynamicBlockEntityRenderer(elements.toArray(new BlockEntityElement[0]));
        } else {
            this.renderer = null;
        }
    }

    private BlockEntity(BlockPos pos, ImmutableBlockState blockState, CompoundTag tag) {
        this.pos = pos;
        this.blockOwner = blockState.owner();
        this.blockState = blockState;
        this.validForTick = isValidBlockState(blockState);
        this.controller = new InactiveBlockEntityController(this, tag);
        this.renderer = null;
        this.valid = true;
    }

    public static BlockEntity inactive(BlockPos pos, ImmutableBlockState blockState, CompoundTag tag) {
        return new BlockEntity(pos, blockState, tag);
    }

    public static BlockPos readPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    public static BlockPos readPosAndVerify(CompoundTag tag, ChunkPos chunkPos) {
        int x = tag.getInt("x", 0);
        int y = tag.getInt("y", 0);
        int z = tag.getInt("z", 0);
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        if (sectionX != chunkPos.x || sectionZ != chunkPos.z) {
            x = chunkPos.x * 16 + SectionPos.sectionRelative(x);
            z = chunkPos.z * 16 + SectionPos.sectionRelative(z);
        }
        return new BlockPos(x, y, z);
    }

    public CompoundTag saveAsTag() {
        CompoundTag tag = new CompoundTag();
        this.saveCustomData(tag);
        this.savePos(tag);
        return tag;
    }

    public void setBlockState(ImmutableBlockState blockState) {
        boolean changed = this.blockState != blockState;
        if (changed) {
            this.controller.preBlockStateChange(blockState);
        }
        this.blockState = blockState;
        this.validForTick = isValidBlockState(blockState);
    }

    public boolean isValidForTick() {
        return this.validForTick;
    }

    public ImmutableBlockState blockState() {
        return this.blockState;
    }

    public CEWorld world() {
        return this.world;
    }

    public void setWorld(CEWorld world) {
        this.world = world;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
        if (!valid) cancelChunkLoadSubscriptions();
    }

    ChunkSubscription subscribeChunkLoad(BlockPos target, Runnable callback) {
        if (this.world == null || !this.valid) {
            throw new IllegalStateException("Subscribe to chunk loads from onLoad or while the block entity is valid");
        }
        if (this.chunkLoadSubscriptions == null) {
            this.chunkLoadSubscriptions = this.world.chunkLoadSubscriptions().createOwner(
                    task -> {
                        if (VersionHelper.hasFoliaPatch) {
                            CraftEngine.instance().scheduler().platform().run(task, this.world.world(), this.pos.x() >> 4, this.pos.z() >> 4);
                        } else {
                            task.run();
                        }
                    },
                    task -> CraftEngine.instance().scheduler().platform().runDelayed(task, this.world.world(), this.pos.x() >> 4, this.pos.z() >> 4)
            );
        }
        return this.chunkLoadSubscriptions.subscribe(ChunkPos.asLong(target.x() >> 4, target.z() >> 4), callback);
    }

    private void cancelChunkLoadSubscriptions() {
        if (this.chunkLoadSubscriptions != null) this.chunkLoadSubscriptions.cancelAll();
    }

    private void savePos(CompoundTag tag) {
        tag.putInt("x", this.pos.x());
        tag.putInt("y", this.pos.y());
        tag.putInt("z", this.pos.z());
    }

    public void saveCustomData(CompoundTag tag) {
        this.controller.saveCustomData(tag);
    }

    public void loadCustomData(CompoundTag tag) {
        this.controller.loadCustomData(tag);
    }

    public void preRemove() {
        try {
            this.controller.onRemove();
        } finally {
            cancelChunkLoadSubscriptions();
        }
    }

    public BlockPos pos() {
        return this.pos;
    }

    public boolean isValidBlockState(ImmutableBlockState blockState) {
        return blockState.hasBlockEntity() && blockState.owner() == this.blockOwner;
    }

    public DynamicBlockEntityRenderer dynamicRenderer() {
        return this.renderer;
    }

    public void updateConstantRenderers() {
        CEChunk ceChunk = this.world.getChunkAtIfLoaded(this.pos);
        if (ceChunk != null) {
            ConstantBlockEntityRenderer renderer = ceChunk.getConstantBlockEntityRenderer(this.pos);
            if (renderer != null) {
                for (Player player : ceChunk.getTrackedBy()) {
                    renderer.update(player);
                }
            }
        }
    }
}
