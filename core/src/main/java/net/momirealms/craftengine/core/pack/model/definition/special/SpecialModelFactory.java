package net.momirealms.craftengine.core.pack.model.definition.special;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface SpecialModelFactory<T extends SpecialModel> {

    T create(ConfigSection section);
}
