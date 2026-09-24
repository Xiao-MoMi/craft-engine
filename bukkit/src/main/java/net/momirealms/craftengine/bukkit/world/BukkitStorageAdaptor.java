package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.plugin.agent.ChunkLifecycleAgent;
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
        if (Config.isBlacklistedWorld(world.name())) {
            return new NoneStorage();
        }
        return adapt(world, Config.chunkStorageType());
    }

    public @NotNull WorldDataStorage adapt(@NotNull World world, @NotNull StorageType storageType) {
        switch (storageType) {
            case NONE -> {
                return new NoneStorage();
            }
            case MCA -> {
                Path path = world.directory().resolve(CEWorld.REGION_DIRECTORY);
                return wrap(new MCARegionFileStorage(path, VersionHelper.hasFoliaPatch ? FOLIA_FACTORY : BUKKIT_FACTORY));
            }
            case LINEAR -> {
                Path path = world.directory().resolve(CEWorld.REGION_DIRECTORY);
                return wrap(new LinearRegionFileStorage(path, VersionHelper.hasFoliaPatch ? FOLIA_FACTORY : BUKKIT_FACTORY));
            }
            case PDC -> {
                if (Config.enableChunkCache()) {
                    return cache(new PersistentDataContainerStorage(world, VersionHelper.hasFoliaPatch ? FOLIA_FACTORY : BUKKIT_FACTORY));
                } else {
                    return new PersistentDataContainerStorage(world, VersionHelper.hasFoliaPatch ? FOLIA_FACTORY : BUKKIT_FACTORY);
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported chunk storage type: " + Config.chunkStorageType());
        }
    }

    private @NotNull WorldDataStorage wrap(@NotNull RegionStorage regionStorage) {
        WorldDataStorage storage = Config.enableAsyncChunkWrite() ? new AsyncStorage(regionStorage) : regionStorage;
        if (Config.enableChunkCache()) {
            return cache(storage);
        } else {
            return storage;
        }
    }

    private WorldDataStorage cache(WorldDataStorage storage) {
        if (!Config.lifecycleChunkCache() || !ChunkLifecycleAgent.installed()) {
            return new CachedStorage<>(storage);
        }
        return new LifecycleCachedStorage(storage);
    }
}
