package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IdConfigParser extends AbstractConfigParser {
    protected final Map<Key, Path> loadedConfigs;

    protected IdConfigParser() {
        this.loadedConfigs = this.checkDuplicated() ? (async() ? new ConcurrentHashMap<>(128, 0.5f) : new HashMap<>(128, 0.5f)) : Map.of();
    }

    protected boolean checkDuplicated() {
        return true;
    }

    protected boolean isDuplicate(final Key id, final Path filePath, String currentNode) {
        Path duplicatedPath = this.loadedConfigs.put(id, filePath);
        if (duplicatedPath != null) {
            error(new KnownResourceException(filePath, "resource.duplicated_id", currentNode, duplicatedPath.toString()));
            return true;
        }
        return false;
    }

    @Override
    public void clearConfigs() {
        super.clearConfigs();
        this.loadedConfigs.clear();
    }
}
