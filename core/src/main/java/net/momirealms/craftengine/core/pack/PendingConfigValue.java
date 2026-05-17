package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;

public final class PendingConfigValue {
    public final Pack pack;
    public final Path path;
    public final Key id;
    public final ConfigValue value;

    public PendingConfigValue(Pack pack, Path path, Key id, ConfigValue value) {
        this.pack = pack;
        this.path = path;
        this.id = id;
        this.value = value;
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

    public ConfigValue value() {
        return value;
    }
}