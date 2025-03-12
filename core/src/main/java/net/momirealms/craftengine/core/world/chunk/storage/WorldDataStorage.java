package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface WorldDataStorage {

    @Nullable
    CompoundTag readChunkTagAt(ChunkPos pos) throws IOException;

    void writeChunkTagAt(ChunkPos pos, @Nullable CompoundTag nbt) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
