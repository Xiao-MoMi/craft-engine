package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.world.ChunkPos;

public interface Chunk {

    ChunkPos pos();

    Object minecraftChunk();

    default Object platformChunk() {
        return minecraftChunk();
    }
}
