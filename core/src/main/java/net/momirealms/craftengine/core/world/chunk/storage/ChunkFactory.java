package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

public interface ChunkFactory {

    CEChunk create(CEWorld world, ChunkPos chunkPos);

    CEChunk create(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders);
}
