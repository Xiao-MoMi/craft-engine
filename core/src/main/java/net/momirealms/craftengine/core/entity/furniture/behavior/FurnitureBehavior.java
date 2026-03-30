package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@ApiStatus.Experimental
public abstract class FurnitureBehavior<D> {
    public final CustomFurniture furniture;

    protected FurnitureBehavior(CustomFurniture furniture) {
        this.furniture = furniture;
    }

    public D createData(Furniture furniture) {
        return null;
    }

    /**
     * Creates a ticker that runs on the main server thread.
     */
    public <T extends Furniture> FurnitureTicker<T> createFurnitureTicker(T furniture, D data) {
        return null;
    }

    /**
     * Creates a ticker that runs asynchronously.
     */
    public <T extends Furniture> FurnitureTicker<T> createAsyncFurnitureTicker(T furniture, D data) {
        return null;
    }

    public InteractionResult useOnFurniture(Furniture furniture, FurnitureHitBox hitBox, InteractEntityContext context, D data) {
        return InteractionResult.TRY_EMPTY_HAND;
    }

    public InteractionResult useWithoutItem(Furniture furniture, InteractEntityContext context, D data) {
        return InteractionResult.PASS;
    }

    public void createFurnitureElements(Furniture furniture, Consumer<FurnitureElement> consumer, D data) {
    }

    public void createFurnitureHitboxes(Furniture furniture, Consumer<FurnitureHitBox> consumer, D data) {
    }

    /**
     * Triggered when the furniture is broken.
     */
    public void onDestroy(Furniture furniture, D data) {
    }

    /**
     * Triggered when the furniture is first placed in the world.
     */
    public void onPlace(Furniture furniture, UseOnContext context, D data) {
    }

    /**
     * Triggered when the chunk containing the furniture is unloaded.
     */
    public void onUnload(Furniture furniture, D data) {
    }

    /**
     * Triggered when the chunk containing the furniture is loaded into the world.
     */
    public void onLoad(Furniture furniture, D data) {
    }

    @Nullable
    public Item getItemToPickup(Furniture furniture, Player player, D data) {
        return null;
    }

    public CustomFurniture furniture() {
        return this.furniture;
    }
}
