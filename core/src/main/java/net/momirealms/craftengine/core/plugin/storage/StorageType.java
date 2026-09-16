package net.momirealms.craftengine.core.plugin.storage;

import net.momirealms.craftengine.core.util.Key;

public record StorageType<T extends Storage>(Key id, StorageFactory<T> factory) {
}
