package net.momirealms.craftengine.core.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.storage.DefaultRegionFileStorage;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class CEWorld {
    public static final String REGION_DIRECTORY = "craftengine";
    protected final World world;
    protected final Map<Long, CEChunk> loadedChunkMap;
    protected final WorldDataStorage worldDataStorage;
    protected final ReentrantReadWriteLock loadedChunkMapLock = new ReentrantReadWriteLock();
    protected final WorldHeight worldHeightAccessor;
    protected final Set<SectionPos> updatedSectionPositions = Collections.synchronizedSet(new HashSet<>());

    private CEChunk lastChunk;
    private long lastChunkPos;

    public CEWorld(World world) {
        this.world = world;
        this.loadedChunkMap = new Long2ObjectOpenHashMap<>(1024, 0.5f);
        this.worldDataStorage = new DefaultRegionFileStorage(world.directory().resolve(REGION_DIRECTORY));
        this.worldHeightAccessor = world.worldHeight();
        this.lastChunkPos = ChunkPos.INVALID_CHUNK_POS;
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
    public CEChunk getLoadedChunkImmediately(int x, int z) {
        long longKey = ChunkPos.asLong(x, z);
        this.loadedChunkMapLock.readLock().lock();
        try {
            return this.loadedChunkMap.get(longKey);
        } finally {
            this.loadedChunkMapLock.readLock().unlock();
        }
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

    @Nullable
    public CEChunk getChunkAtIfLoaded(int x, int z) {
        long chunkPos = ChunkPos.asLong(x, z);
        return this.getChunkAtIfLoadedMainThread(chunkPos);
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
        this.updatedSectionPositions.add(pos);
    }

    public void sectionLightUpdated(Set<SectionPos> pos) {
        this.updatedSectionPositions.addAll(pos);
    }

    public abstract void tick();
}
