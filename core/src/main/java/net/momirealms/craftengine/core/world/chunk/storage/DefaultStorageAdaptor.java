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

import java.nio.file.Path;

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
        Path regionDirectory = world.directory().resolve(CEWorld.REGION_DIRECTORY);
        RegionStorage regionStorage = Config.chunkStorageType() == StorageType.LINEAR
                ? new LinearRegionFileStorage(regionDirectory, FACTORY)
                : new MCARegionFileStorage(regionDirectory, FACTORY);
        return new CachedStorage<>(new AsyncStorage(regionStorage));
    }
}
