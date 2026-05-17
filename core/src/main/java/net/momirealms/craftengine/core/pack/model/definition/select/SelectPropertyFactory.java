package net.momirealms.craftengine.core.pack.model.definition.select;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface SelectPropertyFactory<T extends SelectProperty> {

    T create(ConfigSection section);
}
