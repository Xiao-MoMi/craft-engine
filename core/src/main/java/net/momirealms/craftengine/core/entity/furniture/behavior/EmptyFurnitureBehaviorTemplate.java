package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.EmptyFurnitureDefinition;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import org.jetbrains.annotations.NotNull;

public final class EmptyFurnitureBehaviorTemplate extends FurnitureBehaviorTemplate {
    public static final EmptyFurnitureBehaviorTemplate INSTANCE = new EmptyFurnitureBehaviorTemplate(EmptyFurnitureDefinition.INSTANCE);

    public EmptyFurnitureBehaviorTemplate(FurnitureDefinition furniture) {
        super(furniture);
    }

    @Override
    public FurnitureController createController(Furniture furniture) {
        return new EmptyFurnitureController(furniture);
    }

    public static final class EmptyFurnitureController extends FurnitureController {

        public EmptyFurnitureController(@NotNull Furniture furniture) {
            super(furniture);
        }
    }
}
