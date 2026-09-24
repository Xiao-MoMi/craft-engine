package net.momirealms.craftengine.core.world.chunk.storage;

import ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.WorldSettings;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class LifecycleCachedStorage implements WorldDataStorage {
    private final WorldDataStorage storage;
    private final ConcurrentChainedLong2ReferenceHashTable<Entry> chunks = ConcurrentChainedLong2ReferenceHashTable.createWithCapacity(4096);
    private volatile boolean closed;

    public LifecycleCachedStorage(WorldDataStorage storage) {
        this.storage = storage;
    }

    public Entry bind(ChunkPos pos, Object owner) {
        return this.chunks.compute(pos.longKey, (key, previous) -> {
            if (previous != null && (previous.owner == null || previous.owner == owner)) {
                previous.owner = owner;
                return previous;
            }
            if (previous != null) previous.retired = true;
            return new Entry(owner);
        });
    }

    public void release(ChunkPos pos, Object owner) {
        this.chunks.computeIfPresent(pos.longKey, (key, entry) -> {
            if (entry.owner != null && entry.owner != owner) return entry;
            entry.retired = true;
            return null;
        });
    }

    public void preload(Entry entry, CEWorld world, ChunkPos pos, @Nullable Chunk access) throws IOException {
        if (!entry.retired && !this.closed) this.read(entry, world, pos, access);
    }

    private CEChunk read(Entry entry, CEWorld world, ChunkPos pos, @Nullable Chunk access) throws IOException {
        CEChunk chunk = entry.chunk;
        if (chunk != null) return chunk;
        synchronized (entry) {
            chunk = entry.chunk;
            if (chunk == null) {
                if (this.closed || entry.retired) throw new IOException("Chunk storage lifetime has ended at " + pos);
                chunk = this.storage.readChunkAt(world, pos, access);
                // An in-flight read may finish after cancellation and holder removal.
                if (!entry.retired && !this.closed) entry.chunk = chunk;
            }
        }
        return chunk;
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk access) throws IOException {
        if (this.closed) throw new IOException("Chunk storage is closed");
        Entry entry = this.chunks.get(pos.longKey);
        if (entry == null) {
            // Startup chunks can predate agent callbacks; adopt them on their next load task.
            entry = this.chunks.computeIfAbsent(pos.longKey, key -> new Entry(null));
        }
        return this.read(entry, world, pos, access);
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) throws IOException {
        Entry entry = this.chunks.get(pos.longKey);
        if (entry == null) {
            this.storage.clearChunkAt(pos);
        } else {
            synchronized (entry) {
                this.storage.clearChunkAt(pos);
                entry.chunk = null;
            }
        }
    }

    @Override
    public WorldSettings readSettings() throws IOException { return this.storage.readSettings(); }

    @Override
    public void writeSettings(WorldSettings settings) throws IOException { this.storage.writeSettings(settings); }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException { this.storage.writeChunkAt(pos, chunk); }

    @Override
    public @Nullable CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException { return this.storage.readChunkTagAt(pos); }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag tag) throws IOException { this.storage.writeChunkTagAt(pos, tag); }

    @Override
    public void flush() throws IOException { this.storage.flush(); }

    @Override
    public void close() throws IOException {
        this.closed = true;
        for (var entry : this.chunks.entrySet()) entry.getValue().retired = true;
        this.chunks.clear();
        this.storage.close();
    }

    public static final class Entry {
        private Object owner;
        private volatile CEChunk chunk;
        private volatile boolean retired;

        private Entry(Object owner) { this.owner = owner; }
    }
}
