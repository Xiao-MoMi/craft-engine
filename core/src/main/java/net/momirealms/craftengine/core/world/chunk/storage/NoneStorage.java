package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class NoneStorage implements WorldDataStorage {

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos, @Nullable Chunk chunkAccess) {
        return new CEChunk(world, pos);
    }

    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) {
    }

    @Override
    public @Nullable CompoundTag readChunkTagAt(@NotNull ChunkPos pos) {
        return null;
    }

    @Override
    public void writeChunkTagAt(@NotNull ChunkPos pos, @Nullable CompoundTag nbt) {
    }

    @Override
    public void clearChunkAt(@NotNull ChunkPos pos) {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
