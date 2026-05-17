package net.momirealms.craftengine.core.entity.furniture.tick;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class TickingFurnitureImpl<T extends FurnitureController> implements TickingFurniture {
    private final Furniture furniture;
    private final FurnitureTicker<T> ticker;

    public TickingFurnitureImpl(Furniture furniture, FurnitureTicker<T> ticker) {
        this.furniture = furniture;
        this.ticker = ticker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void tick() {
        this.ticker.tick(this.furniture, (T) this.furniture.controller);
    }

    @Override
    public boolean isValid() {
        return this.furniture.isValid();
    }

    @Override
    public int entityId() {
        return this.furniture.entityId();
    }
}
