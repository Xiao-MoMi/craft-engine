package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;

public interface ItemProcessorFactory<T extends ItemProcessor> {

    T create(ConfigValue value);
}
