package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.util.ExpiringLong2ObjectCache;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.WorldSettings;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class CachedStorage<T extends WorldDataStorage> implements WorldDataStorage {
    private static final int LOAD_LOCK_STRIPES = 256;

    private final T storage;
    private final ExpiringLong2ObjectCache<CEChunk> chunkCache;
    private final Object[] loadLocks;

    public CachedStorage(T storage) {
        this.storage = storage;
        this.chunkCache = new ExpiringLong2ObjectCache<>(30, TimeUnit.SECONDS, 4096);
        this.loadLocks = new Object[LOAD_LOCK_STRIPES];
        for (int i = 0; i < LOAD_LOCK_STRIPES; i++) {
            this.loadLocks[i] = new Object();
        }
    }

    private Object loadLock(long key) {
        return this.loadLocks[Long.hashCode(key) & (LOAD_LOCK_STRIPES - 1)];
    }

    private @NotNull CEChunk loadChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException {
        long key = pos.longKey;
        CEChunk chunk = this.chunkCache.getIfPresent(key);
        if (chunk != null) {
            return chunk;
        }
        synchronized (this.loadLock(key)) {
            chunk = this.chunkCache.getIfPresent(key);
            if (chunk != null) {
                return chunk;
            }
            chunk = this.storage.readChunkAt(world, pos, chunkAccess);
            this.chunkCache.put(key, chunk);
            return chunk;
        }
    }

    @Override
    public WorldSettings readSettings() throws IOException {
        return this.storage.readSettings();
    }

    @Override
    public void writeSettings(WorldSettings settings) throws IOException {
        this.storage.writeSettings(settings);
    }

    @Override
    public CEChunk readNewChunkAt(CEWorld world, ChunkPos pos) throws IOException {
        return this.storage.readNewChunkAt(world, pos);
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException {
        return this.loadChunkAt(world, pos, chunkAccess);
    }

    @Override
    public void preloadChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException {
        this.loadChunkAt(world, pos, chunkAccess);
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException {
        this.storage.writeChunkAt(pos, chunk);
    }

    @Override
    public @Nullable CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException {
        return this.storage.readChunkTagAt(pos);
    }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) throws IOException {
        this.storage.writeChunkTagAt(pos, nbt);
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) throws IOException {
        this.chunkCache.invalidate(pos.longKey);
        this.storage.clearChunkAt(pos);
    }

    @Override
    public void close() throws IOException {
        this.storage.close();
    }

    @Override
    public void flush() throws IOException {
        this.storage.flush();
    }
}
