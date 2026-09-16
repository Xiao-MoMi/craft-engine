package net.momirealms.craftengine.core.plugin.storage;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface StorageFactory<T extends Storage> {

    T create(ConfigSection section) throws Exception;
}
