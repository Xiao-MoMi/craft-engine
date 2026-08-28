package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface CategorySourceFactory<T extends CategorySource> {

    T create(ConfigSection section);
}
