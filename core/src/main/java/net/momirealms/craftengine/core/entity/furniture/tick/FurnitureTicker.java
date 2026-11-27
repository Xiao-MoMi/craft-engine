package net.momirealms.craftengine.core.entity.furniture.tick;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;

public interface FurnitureTicker<T extends FurnitureController> {

    void tick(Furniture furniture, T controller);
}
