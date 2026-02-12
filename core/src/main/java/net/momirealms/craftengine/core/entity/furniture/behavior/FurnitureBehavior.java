package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public abstract class FurnitureBehavior {
    public final CustomFurniture furniture;

    protected FurnitureBehavior(CustomFurniture furniture) {
        this.furniture = furniture;
    }

    public <T extends Furniture> FurnitureTicker<T> createSyncFurnitureTicker(T furniture) {
        return null;
    }

    public <T extends Furniture> FurnitureTicker<T> createAsyncBlockEntityTicker(T furniture) {
        return null;
    }

    public InteractionResult useOnFurniture(InteractEntityContext context, Furniture state) {
        return InteractionResult.TRY_EMPTY_HAND;
    }

    public InteractionResult useWithoutItem(InteractEntityContext context, Furniture state) {
        return InteractionResult.PASS;
    }

    public void onRemove(Furniture furniture) {
    }

    public void onAdd(Furniture furniture) {
    }

    @Nullable
    public Item<?> itemToPickup(Furniture furniture, Player player) {
        return null;
    }

    public CustomFurniture furniture() {
        return this.furniture;
    }
}
