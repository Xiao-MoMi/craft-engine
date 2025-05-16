package net.momirealms.craftengine.core.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class CEWorld {
    public static final String REGION_DIRECTORY = "craftengine";
    protected final World world;
    protected final Map<Long, CEChunk> loadedChunkMap;
    protected final WorldDataStorage worldDataStorage;
    protected final ReentrantReadWriteLock loadedChunkMapLock = new ReentrantReadWriteLock();
    protected final WorldHeight worldHeightAccessor;
    protected final Set<SectionPos> updatedSectionSet = ConcurrentHashMap.newKeySet(128);

    private CEChunk lastChunk;
    private long lastChunkPos;

    public CEWorld(World world, StorageAdaptor adaptor) {
        this.world = world;
        this.loadedChunkMap = new Long2ObjectOpenHashMap<>(1024, 0.5f);
        this.worldDataStorage = adaptor.adapt(world);
        this.worldHeightAccessor = world.worldHeight();
        this.lastChunkPos = ChunkPos.INVALID_CHUNK_POS;
    }

    public CEWorld(World world, WorldDataStorage dataStorage) {
        this.world = world;
        this.loadedChunkMap = new Long2ObjectOpenHashMap<>(1024, 0.5f);
        this.worldDataStorage = dataStorage;
        this.worldHeightAccessor = world.worldHeight();
        this.lastChunkPos = ChunkPos.INVALID_CHUNK_POS;
    }

    public void save() {
        this.loadedChunkMapLock.readLock().lock();
        try {
            for (Map.Entry<Long, CEChunk> entry : this.loadedChunkMap.entrySet()) {
                CEChunk chunk = entry.getValue();
                if (chunk.dirty()) {
                    worldDataStorage.writeChunkAt(new ChunkPos(entry.getKey()), chunk);
                    chunk.setDirty(false);
                }
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to save world chunks", e);
        } finally {
            this.loadedChunkMapLock.readLock().unlock();
        }
    }

    public World world() {
        return world;
    }

    public boolean isChunkLoaded(final long chunkPos) {
        this.loadedChunkMapLock.readLock().lock();
        try {
            return loadedChunkMap.containsKey(chunkPos);
        } finally {
            this.loadedChunkMapLock.readLock().unlock();
        }
    }

    public void addLoadedChunk(CEChunk chunk) {
        this.loadedChunkMapLock.writeLock().lock();
        try {
            this.loadedChunkMap.put(chunk.chunkPos().longKey(), chunk);
        } finally {
            this.loadedChunkMapLock.writeLock().unlock();
        }
    }

    public void removeLoadedChunk(CEChunk chunk) {
        this.loadedChunkMapLock.writeLock().lock();
        try {
            this.loadedChunkMap.remove(chunk.chunkPos().longKey());
            if (this.lastChunk == chunk) {
                this.lastChunk = null;
                this.lastChunkPos = ChunkPos.INVALID_CHUNK_POS;
            }
        } finally {
            this.loadedChunkMapLock.writeLock().unlock();
        }
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(long chunkPos) {
        this.loadedChunkMapLock.readLock().lock();
        try {
            return getChunkAtIfLoadedMainThread(chunkPos);
        } finally {
            this.loadedChunkMapLock.readLock().unlock();
        }
    }

    @Nullable
    public CEChunk getChunkAtIfLoaded(int x, int z) {
        return getChunkAtIfLoaded(ChunkPos.asLong(x, z));
    }

    @Nullable
    public CEChunk getChunkAtIfLoadedMainThread(long chunkPos) {
        if (chunkPos == this.lastChunkPos) {
            return this.lastChunk;
        }
        CEChunk chunk = this.loadedChunkMap.get(chunkPos);
        if (chunk != null) {
            this.lastChunk = chunk;
            this.lastChunkPos = chunkPos;
        }
        return chunk;
    }

    @Nullable
    public CEChunk getChunkAtIfLoadedMainThread(int x, int z) {
        return getChunkAtIfLoadedMainThread(ChunkPos.asLong(x, z));
    }

    public WorldHeight worldHeight() {
        return worldHeightAccessor;
    }

    public ImmutableBlockState getBlockStateAtIfLoaded(int x, int y, int z) {
        CEChunk chunk = getChunkAtIfLoaded(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(x, y, z);
    }

    public ImmutableBlockState getBlockStateAtIfLoaded(BlockPos blockPos) {
        CEChunk chunk = getChunkAtIfLoaded(blockPos.x() >> 4, blockPos.z() >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(blockPos);
    }

    public boolean setBlockStateAtIfLoaded(BlockPos blockPos, ImmutableBlockState blockState) {
        if (worldHeightAccessor.isOutsideBuildHeight(blockPos)) {
            return false;
        }
        CEChunk chunk = getChunkAtIfLoaded(blockPos.x() >> 4, blockPos.z() >> 4);
        if (chunk == null) {
            return false;
        }
        chunk.setBlockState(blockPos, blockState);
        return true;
    }

    public WorldDataStorage worldDataStorage() {
        return worldDataStorage;
    }

    public void sectionLightUpdated(SectionPos pos) {
        this.updatedSectionSet.add(pos);
    }

    public void sectionLightUpdated(Collection<SectionPos> pos) {
        this.updatedSectionSet.addAll(pos);
    }

    public abstract void tick();
}
