package net.momirealms.craftengine.core.entity.furniture.tick;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface FurnitureTicker<T extends FurnitureController> {

    void tick(Furniture furniture, T controller);
}
