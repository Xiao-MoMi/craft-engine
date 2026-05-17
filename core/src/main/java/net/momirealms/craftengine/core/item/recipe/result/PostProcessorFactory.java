package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface PostProcessorFactory<T extends PostProcessor> {

    T create(ConfigSection section);
}
