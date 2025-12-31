package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public abstract class FurnitureBehavior {
    public final CustomFurniture furniture;

    public FurnitureBehavior(CustomFurniture furniture) {
        this.furniture = furniture;
    }

    public <T extends Furniture> FurnitureTicker<T> createSyncFurnitureTicker(T furniture) {
        return null;
    }

    public <T extends Furniture> FurnitureTicker<T> createAsyncBlockEntityTicker(T furniture) {
        return null;
    }

    public void asyncShow(Furniture furniture, Player player) {
    }

    public void asyncHide(Furniture furniture, Player player) {
    }

    public boolean onBreak(Furniture furniture, Player player, FurnitureHitBox hitBox, boolean usingSecondaryAction) {
        return true;
    }

    public boolean onInteract(Furniture furniture, Player player, InteractionHand hand, WorldPosition interactionPosition, FurnitureHitBox hitBox, boolean usingSecondaryAction) {
        return true;
    }

    public InteractionResult onPlace(Furniture furniture, UseOnContext context, WorldPosition placePosition) {
        return InteractionResult.SUCCESS;
    }

    public void onCreate(Furniture furniture) {
    }

    public void onDestroy(Furniture furniture) {
    }

    public @Nullable LootTable<?> getLootTable(Furniture furniture, @Nullable LootTable<?> lootTable) {
        return lootTable;
    }

    public <I> @Nullable Item<I> getPickItem(Furniture furniture, Player player, Item<I> item) {
        return item;
    }
}
