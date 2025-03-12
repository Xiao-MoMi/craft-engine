package net.momirealms.craftengine.core.pack;

import java.nio.file.Path;
import java.util.Map;

public class CachedConfig {
    private final Pack pack;
    private final Path filePath;
    private final Map<String, Object> config;

    public CachedConfig(Map<String, Object> config, Path filePath, Pack pack) {
        this.config = config;
        this.filePath = filePath;
        this.pack = pack;
    }

    public Map<String, Object> config() {
        return config;
    }

    public Path filePath() {
        return filePath;
    }

    public Pack pack() {
        return pack;
    }
}
