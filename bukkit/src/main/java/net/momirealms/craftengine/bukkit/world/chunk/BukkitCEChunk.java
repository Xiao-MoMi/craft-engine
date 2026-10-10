package net.momirealms.craftengine.bukkit.world.chunk;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

public class BukkitCEChunk extends CEChunk {

    public BukkitCEChunk(CEWorld world, ChunkPos chunkPos) {
        super(world, chunkPos);
    }

    public BukkitCEChunk(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
        super(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
    }
}
