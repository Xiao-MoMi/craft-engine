package net.momirealms.craftengine.core.world;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.tick.TickingBlockEntity;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTickerScheduler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CEWorld {
    public static final String REGION_DIRECTORY = "craftengine";
    public final World world;
    public final WorldSettings settings;
    protected final ConcurrentChainedLong2ReferenceHashTable<CEChunk> loadedChunkMap;
    protected final WorldDataStorage worldDataStorage;
    protected final WorldHeight worldHeightAccessor;
    protected final MultiThreadedQueue<Collection<SectionPos>> pendingLightSectionBatches = new MultiThreadedQueue<>();
    protected final AtomicBoolean lightUpdateRunning = new AtomicBoolean(false);
    protected final BlockEntityTickerScheduler syncTickingBlockEntities = new BlockEntityTickerScheduler();
    protected final BlockEntityTickerScheduler asyncTickingBlockEntities = BlockEntityTickerScheduler.async();
    protected final AtomicBoolean asyncTickRunning = new AtomicBoolean(false);
    protected SchedulerTask syncTickTask;
    protected SchedulerTask asyncTickTask;
    protected boolean ticking;

    public CEWorld(World world, StorageAdaptor adaptor) {
        this(world, adaptor.adapt(world));
    }

    public CEWorld(World world, WorldDataStorage dataStorage) {
        this.world = world;
        this.loadedChunkMap = ConcurrentChainedLong2ReferenceHashTable.createWithCapacity(1024, 0.5f);
        this.worldDataStorage = dataStorage;
        this.worldHeightAccessor = world.worldHeight();
        WorldSettings worldSettings;
        try {
            worldSettings = dataStorage.readSettings();
        } catch (IOException e) {
            worldSettings = new WorldSettings();
            CraftEngine.instance().logger().warn("Failed to read settings from world " + this.name(), e);
        }
        this.settings = worldSettings;
    }

    public void setTicking(boolean ticking) {
        if (ticking) {
            if (this.syncTickTask == null || this.syncTickTask.cancelled())
                this.syncTickTask = CraftEngine.instance().scheduler().platform().runRepeating(this::syncTick, 1, 1);
            if (this.asyncTickTask == null || this.asyncTickTask.cancelled())
                this.asyncTickTask = CraftEngine.instance().scheduler().platform().runAsyncRepeating(() -> {
                    // 上一轮 asyncTick 还没跑完就跳过本轮，避免并发重入
                    if (this.asyncTickRunning.compareAndSet(false, true)) {
                        try {
                            this.asyncTick();
                        } finally {
                            this.asyncTickRunning.set(false);
                        }
                    }
                }, 1, 1);
        } else {
            if (this.syncTickTask != null && !this.syncTickTask.cancelled())
                this.syncTickTask.cancel();
            if (this.asyncTickTask != null && !this.asyncTickTask.cancelled())
                this.asyncTickTask.cancel();
        }
        this.ticking = ticking;
    }

    public boolean isTicking() {
        return this.ticking;
    }

    public String name() {
        return this.world.name();
    }

    public UUID uuid() {
        return this.world.uuid();
    }

    public void saveChunks() {
        try {
            for (ConcurrentChainedLong2ReferenceHashTable.TableEntry<CEChunk> entry : this.loadedChunkMap.entrySet()) {
                CEChunk chunk = entry.getValue();
                if (chunk.isUnsaved()) {
                    this.worldDataStorage.writeChunkAt(new ChunkPos(entry.getKey()), chunk);
                    chunk.setUnsaved(false);
                }
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to save world chunks", e);
        }
    }

    public void saveSettings() {
        try {
            this.worldDataStorage.writeSettings(this.settings);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to save world settings", e);
        }
    }

    public World world() {
        return this.world;
    }

    public boolean isChunkLoaded(final long chunkPos) {
        return this.loadedChunkMap.containsKey(chunkPos);
    }

    public void addLoadedChunk(CEChunk chunk) {
        this.loadedChunkMap.put(chunk.chunkPos.longKey, chunk);
    }

    public void removeLoadedChunk(CEChunk chunk) {
        this.loadedChunkMap.remove(chunk.chunkPos.longKey);
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(long chunkPos) {
        return this.loadedChunkMap.get(chunkPos);
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(int x, int z) {
        return getChunkAtIfLoaded(ChunkPos.asLong(x, z));
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(BlockPos pos) {
        return getChunkAtIfLoaded(pos.x >> 4, pos.z >> 4);
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(ChunkPos chunkPos) {
        return getChunkAtIfLoaded(chunkPos.longKey);
    }

    @Nullable
    public ImmutableBlockState getBlockStateAtIfLoaded(int x, int y, int z) {
        CEChunk chunk = getChunkAtIfLoaded(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(x, y, z);
    }

    @Nullable
    public ImmutableBlockState getBlockStateAtIfLoaded(BlockPos blockPos) {
        CEChunk chunk = getChunkAtIfLoaded(blockPos.x() >> 4, blockPos.z() >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(blockPos);
    }

    public boolean setBlockStateAtIfLoaded(BlockPos blockPos, ImmutableBlockState blockState) {
        if (this.worldHeightAccessor.isOutsideBuildHeight(blockPos)) {
            return false;
        }
        CEChunk chunk = getChunkAtIfLoaded(blockPos.x() >> 4, blockPos.z() >> 4);
        if (chunk == null) {
            return false;
        }
        chunk.setBlockState(blockPos, blockState);
        return true;
    }

    @Nullable
    public BlockEntity getBlockEntityAtIfLoaded(BlockPos blockPos) {
        return getBlockEntityAtIfLoaded(blockPos, true);
    }

    public BlockEntity getBlockEntityAtIfLoaded(BlockPos blockPos, boolean create) {
        if (this.worldHeightAccessor.isOutsideBuildHeight(blockPos)) {
            return null;
        }
        CEChunk chunk = getChunkAtIfLoaded(blockPos.x() >> 4, blockPos.z() >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockEntity(blockPos, create);
    }
    
    public WorldDataStorage worldDataStorage() {
        return worldDataStorage;
    }

    public void sectionLightUpdated(Collection<SectionPos> pos) {
        if (!pos.isEmpty()) {
            this.pendingLightSectionBatches.offer(pos);
        }
    }

    @Nullable
    protected LongOpenHashSet drainPendingLightSections() {
        LongOpenHashSet sections = new LongOpenHashSet(16);
        int drained = this.pendingLightSectionBatches.drain(batch -> {
            for (SectionPos section : batch) {
                sections.add(section.asLong());
            }
        });
        if (drained == 0) {
            return null;
        }
        return sections;
    }

    public WorldHeight worldHeight() {
        return this.worldHeightAccessor;
    }

    public void syncTick() {
        this.tickSyncBlockEntities();
        if (!Config.asyncLightUpdate()) {
            this.updateLight();
        }
    }

    public void asyncTick() {
        this.tickAsyncBlockEntities();
        if (Config.asyncLightUpdate()) {
            this.updateLight();
        }
    }

    public abstract void updateLight();

    public void addSyncBlockEntityTicker(TickingBlockEntity ticker) {
        this.syncTickingBlockEntities.add(ticker);
    }

    public void addAsyncBlockEntityTicker(TickingBlockEntity ticker) {
        this.asyncTickingBlockEntities.add(ticker);
    }

    protected void tickSyncBlockEntities() {
        this.syncTickingBlockEntities.tick();
    }

    protected void tickAsyncBlockEntities() {
        this.asyncTickingBlockEntities.tick();
    }

    public void blockEntityChanged(BlockPos pos) {
        CEChunk chunk = this.getChunkAtIfLoaded(pos.x >> 4, pos.z >> 4);
        if (chunk != null) {
            chunk.setUnsaved(true);
        }
    }
}
