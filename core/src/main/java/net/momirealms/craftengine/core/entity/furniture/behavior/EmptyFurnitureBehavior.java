package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.EmptyFurniture;

public final class EmptyFurnitureBehavior extends FurnitureBehavior<Object> {
    public static final FurnitureBehavior<Object> INSTANCE = new EmptyFurnitureBehavior(EmptyFurniture.INSTANCE);

    public EmptyFurnitureBehavior(CustomFurniture furniture) {
        super(furniture);
    }
}
