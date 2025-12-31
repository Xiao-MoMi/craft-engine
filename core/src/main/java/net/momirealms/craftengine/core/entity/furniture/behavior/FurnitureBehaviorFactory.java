package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;

import java.util.Map;

public interface FurnitureBehaviorFactory<T extends FurnitureBehavior> {

    T create(CustomFurniture furniture, Map<String, Object> properties);
}
