package net.momirealms.craftengine.core.pack.model.definition.tint;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface TintFactory<T extends Tint> {

    T create(ConfigSection section);
}
