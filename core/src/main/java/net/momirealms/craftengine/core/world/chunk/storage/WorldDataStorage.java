package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface WorldDataStorage {

    default CEChunk readNewChunkAt(CEWorld world, ChunkPos pos) throws IOException {
        this.clearChunkAt(pos);
        return this.readChunkAt(world, pos, null);
    }

    @NotNull
    CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) throws IOException;

    @Nullable
    CompoundTag readChunkTagAt(@NotNull ChunkPos pos) throws IOException;

    void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) throws IOException;

    void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) throws IOException;

    void clearChunkAt(@NotNull ChunkPos pos) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
