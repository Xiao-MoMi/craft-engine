package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface FurnitureTintSourceConfigFactory<T extends FurnitureTintSource> {

    FurnitureTintSourceConfig<T> create(ConfigSection section);
}
