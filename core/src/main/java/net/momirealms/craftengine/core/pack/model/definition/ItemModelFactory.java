package net.momirealms.craftengine.core.pack.model.definition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface ItemModelFactory<T extends ItemModel> {

    T create(ConfigSection section);
}
