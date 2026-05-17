package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.nio.file.Path;

public final class CachedConfigSection {
    public final Pack pack;
    public final Path path;
    public final ConfigSection config;

    public CachedConfigSection(Pack pack, Path path, ConfigSection config) {
        this.pack = pack;
        this.path = path;
        this.config = config;
    }

    public Pack pack() {
        return pack;
    }

    public Path path() {
        return path;
    }

    public ConfigSection config() {
        return config;
    }
}