package net.momirealms.craftengine.core.world.chunk.storage;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;

public class DefaultStorageAdaptor implements StorageAdaptor {

    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        if (Config.chunkStorageType() == StorageType.NONE) {
            return new NoneStorage();
        }
        if (Config.enableChunkCache()) {
            return new CachedStorage<>(new DefaultRegionFileStorage(world.directory().resolve(CEWorld.REGION_DIRECTORY)));
        } else {
            return new DefaultRegionFileStorage(world.directory().resolve(CEWorld.REGION_DIRECTORY));
        }
    }
}
