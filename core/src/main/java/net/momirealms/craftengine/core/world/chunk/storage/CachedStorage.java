package net.momirealms.craftengine.core.world.chunk.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class CachedStorage<T extends WorldDataStorage> implements WorldDataStorage {
    private final T storage;
    private final Cache<ChunkPos, CEChunk> chunkCache;

    public CachedStorage(T storage) {
        this.storage = storage;
        this.chunkCache = Caffeine.newBuilder()
                .executor(CraftEngine.instance().scheduler().async())
                .scheduler(Scheduler.systemScheduler())
                .initialCapacity(2048)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public CEChunk readNewChunkAt(CEWorld world, ChunkPos pos) throws IOException {
        return this.storage.readNewChunkAt(world, pos);
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos) throws IOException {
        CEChunk chunk = this.chunkCache.getIfPresent(pos);
        if (chunk != null) {
            return chunk;
        }
        chunk = this.storage.readChunkAt(world, pos);
        this.chunkCache.put(pos, chunk);
        return chunk;
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException {
        this.storage.writeChunkAt(pos, chunk);
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) throws IOException {
        this.chunkCache.invalidate(pos);
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