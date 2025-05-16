package net.momirealms.craftengine.core.pack;

import java.util.Map;

public class CachedConfigFile {
    private final Map<String, Object> config;
    private final long lastModified;
    private final long size;
    private final Pack pack;

    public CachedConfigFile(Map<String, Object> config, Pack pack, long lastModified, long size) {
        this.config = config;
        this.size = size;
        this.lastModified = lastModified;
        this.pack = pack;
    }

    public Pack pack() {
        return pack;
    }

    public Map<String, Object> config() {
        return config;
    }

    public long lastModified() {
        return lastModified;
    }

    public long size() {
        return size;
    }
}
