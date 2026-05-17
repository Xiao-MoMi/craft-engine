package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface NumberProviderFactory<T extends NumberProvider> {

    T create(ConfigSection args);
}
