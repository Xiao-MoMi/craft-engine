package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSettings;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Experimental
public final class EmptyFurnitureBehavior extends FurnitureBehavior {
    public static final EmptyFurnitureBehavior INSTANCE = new EmptyFurnitureBehavior();
    private static final CustomFurniture EMPTY_FURNITURE;

    static {
        EMPTY_FURNITURE = CustomFurniture.builder()
                .id(Key.withDefaultNamespace("empty"))
                .settings(FurnitureSettings.of())
                .variants(Map.of())
                .events(Map.of())
                .behavior(EmptyFurnitureBehavior.INSTANCE)
                .build();
    }

    private EmptyFurnitureBehavior() {
        super(EMPTY_FURNITURE);
    }
}
