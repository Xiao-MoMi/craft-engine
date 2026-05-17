package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public abstract class FurnitureBehaviorTemplate {
    public final FurnitureDefinition furniture;
    protected FurnitureBehaviorTemplate(FurnitureDefinition furniture) {
        this.furniture = furniture;
    }

    public FurnitureDefinition furniture() {
        return this.furniture;
    }

    public abstract FurnitureController createController(Furniture furniture);
}
