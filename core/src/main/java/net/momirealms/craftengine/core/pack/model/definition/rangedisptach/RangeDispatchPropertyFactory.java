package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface RangeDispatchPropertyFactory<T extends RangeDispatchProperty> {

    T create(ConfigSection section);
}
