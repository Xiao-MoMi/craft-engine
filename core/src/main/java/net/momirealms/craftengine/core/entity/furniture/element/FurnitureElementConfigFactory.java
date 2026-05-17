package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface FurnitureElementConfigFactory<E extends FurnitureElement>  {

    FurnitureElementConfig<E> create(ConfigSection section);
}
