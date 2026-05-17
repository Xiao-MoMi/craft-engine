package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface FurnitureBehaviorFactory<T extends FurnitureBehaviorTemplate> {

    T create(FurnitureDefinition furniture, ConfigSection section);
}
