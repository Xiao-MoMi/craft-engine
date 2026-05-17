package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultStorageAdaptor implements StorageAdaptor {
    private static final ChunkFactory FACTORY = new ChunkFactory() {
        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos) {
            return new CEChunk(world, chunkPos);
        }

        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
            return new CEChunk(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
        }
    };

    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        if (Config.chunkStorageType() == StorageType.NONE) {
            return new NoneStorage();
        }
        if (Config.enableChunkCache()) {
            return new CachedStorage<>(new DefaultRegionFileStorage(world.directory().resolve(CEWorld.REGION_DIRECTORY), FACTORY));
        } else {
            return new DefaultRegionFileStorage(world.directory().resolve(CEWorld.REGION_DIRECTORY), FACTORY);
        }
    }
}
