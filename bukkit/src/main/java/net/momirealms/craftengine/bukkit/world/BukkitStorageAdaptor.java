package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.world.chunk.BukkitCEChunk;
import net.momirealms.craftengine.bukkit.world.chunk.FoliaCEChunk;
import net.momirealms.craftengine.bukkit.world.chunk.storage.PersistentDataContainerStorage;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.storage.*;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class BukkitStorageAdaptor implements StorageAdaptor {
    public static final ChunkFactory BUKKIT_FACTORY = new ChunkFactory() {
        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos) {
            return new BukkitCEChunk(world, chunkPos);
        }

        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
            return new BukkitCEChunk(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
        }
    };
    public static final ChunkFactory FOLIA_FACTORY = new ChunkFactory() {
        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos) {
            return new FoliaCEChunk(world, chunkPos);
        }

        @Override
        public CEChunk create(CEWorld world, ChunkPos chunkPos, CESection[] sections, @Nullable ListTag blockEntitiesTag, @Nullable ListTag blockEntityRenders) {
            return new FoliaCEChunk(world, chunkPos, sections, blockEntitiesTag, blockEntityRenders);
        }
    };

    public BukkitStorageAdaptor() {
    }

    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        return adapt(world, Config.chunkStorageType());
    }

    public @NotNull WorldDataStorage adapt(@NotNull World world, @NotNull StorageType storageType) {
        switch (storageType) {
            case NONE -> {
                return new NoneStorage();
            }
            case MCA -> {
                Path path = world.directory().resolve(CEWorld.REGION_DIRECTORY);
                if (Config.enableChunkCache()) {
                    return new CachedStorage<>(new DefaultRegionFileStorage(path, VersionHelper.isFolia ? FOLIA_FACTORY : BUKKIT_FACTORY));
                } else {
                    return new DefaultRegionFileStorage(path, VersionHelper.isFolia ? FOLIA_FACTORY : BUKKIT_FACTORY);
                }
            }
            case PDC -> {
                if (Config.enableChunkCache()) {
                    return new CachedStorage<>(new PersistentDataContainerStorage(world, VersionHelper.isFolia ? FOLIA_FACTORY : BUKKIT_FACTORY));
                } else {
                    return new PersistentDataContainerStorage(world, VersionHelper.isFolia ? FOLIA_FACTORY : BUKKIT_FACTORY);
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported chunk storage type: " + Config.chunkStorageType());
        }
    }
}
