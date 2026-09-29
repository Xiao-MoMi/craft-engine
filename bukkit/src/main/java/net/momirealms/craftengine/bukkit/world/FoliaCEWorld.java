package net.momirealms.craftengine.bukkit.world;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.world.chunk.FoliaCEChunk;
import net.momirealms.craftengine.core.util.TickersList;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class FoliaCEWorld extends BukkitCEWorld {
    private final Map<ChunkPos, TickingChunk> tickingChunkByPos = new ConcurrentHashMap<>(128, 0.5f);
    private final TickersList<TickingChunk> tickingChunks = new TickersList<>();
    private final Queue<TickingChunk> pendingAsyncTickingChunks = new MultiThreadedQueue<>();

    public FoliaCEWorld(World world, StorageAdaptor adaptor) {
        super(world, adaptor);
    }

    public FoliaCEWorld(World world, WorldDataStorage dataStorage) {
        super(world, dataStorage);
    }

    @Override
    protected void tickSyncBlockEntities() {
        TickingChunk pending;
        while ((pending = this.pendingAsyncTickingChunks.poll()) != null) {
            this.tickingChunks.add(pending);
        }

        if (!this.tickingChunks.isEmpty()) {
            Object[] chunks = this.tickingChunks.elements();
            for (int i = 0, size = this.tickingChunks.size(); i < size; i++ ) {
                TickingChunk chunk = (TickingChunk) chunks[i];
                if (chunk.isValid()) {
                    ChunkPos chunkPos = chunk.chunkPos();
                    BukkitCraftEngine.instance().scheduler().platform().run(chunk::tick, this.world, chunkPos.x, chunkPos.z);
                } else {
                    this.tickingChunks.markAsRemoved(i);
                    this.tickingChunkByPos.remove(chunk.chunkPos(), chunk);
                }
            }
            this.tickingChunks.removeMarkedEntries();
        }
    }

    public void replaceOrCreateTickingChunk(FoliaCEChunk chunk) {
        this.tickingChunkByPos.compute(chunk.chunkPos, (pos, tickingChunk) -> {
            if (tickingChunk != null && tickingChunk.chunk == chunk && tickingChunk.isValid()) {
                return tickingChunk;
            }
            if (tickingChunk != null) tickingChunk.retired = true;
            TickingChunk newTickingChunk = new TickingChunk(chunk);
            this.pendingAsyncTickingChunks.add(newTickingChunk);
            return newTickingChunk;
        });
    }

    @Override
    public void removeLoadedChunk(CEChunk chunk) {
        this.tickingChunkByPos.computeIfPresent(chunk.chunkPos, (pos, tickingChunk) -> {
            if (tickingChunk.chunk != chunk) return tickingChunk;
            // A cached CEChunk can load again; its old registration must stay retired.
            tickingChunk.retired = true;
            return null;
        });
        super.removeLoadedChunk(chunk);
    }

    public static class TickingChunk {
        private final FoliaCEChunk chunk;
        private volatile boolean retired;

        public TickingChunk(FoliaCEChunk chunk) {
            this.chunk = chunk;
        }

        public void tick() {
            if (isValid()) {
                this.chunk.tickBlockEntities();
            }
        }

        public boolean isValid() {
            return !this.retired && this.chunk.isLoaded();
        }

        public ChunkPos chunkPos() {
            return this.chunk.chunkPos();
        }
    }
}
