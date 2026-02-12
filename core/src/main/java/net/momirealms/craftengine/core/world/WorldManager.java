package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface WorldManager extends Manageable {

    void setStorageAdaptor(@NotNull StorageAdaptor storageAdaptor);

    CEWorld getWorld(UUID uuid);

    CEWorld[] getWorlds();

    CEWorld loadWorld(World world);

    void loadWorld(CEWorld world, boolean forceInitChunks);

    CEWorld createWorld(World world, WorldDataStorage storage);

    void unloadWorld(World world);

    <T> World wrap(T world);

    ConfigParser[] parsers();
}
