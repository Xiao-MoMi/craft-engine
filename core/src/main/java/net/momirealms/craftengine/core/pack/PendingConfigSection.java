package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;

public final class PendingConfigSection {
    public final Pack pack;
    public final Path path;
    public final Key id;
    public final ConfigSection section;

    public PendingConfigSection(Pack pack, Path path, Key id, ConfigSection section) {
        this.pack = pack;
        this.path = path;
        this.id = id;
        this.section = section;
    }

    public Pack pack() {
        return pack;
    }

    public Path path() {
        return path;
    }

    public Key id() {
        return id;
    }

    public ConfigSection section() {
        return section;
    }
}