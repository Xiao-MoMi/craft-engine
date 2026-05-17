package net.momirealms.craftengine.core.pack.model.definition.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface ConditionPropertyFactory<T extends ConditionProperty> {

    T create(ConfigSection section);
}
