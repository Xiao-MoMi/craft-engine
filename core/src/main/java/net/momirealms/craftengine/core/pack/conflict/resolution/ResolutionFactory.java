package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface ResolutionFactory<T extends Resolution> {

    T create(ConfigSection section);
}
