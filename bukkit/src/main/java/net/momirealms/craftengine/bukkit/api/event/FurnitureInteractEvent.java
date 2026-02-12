package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class FurnitureInteractEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final BukkitFurniture furniture;
    private final InteractionHand hand;
    private final Location interactionPoint;
    private final FurnitureHitBox furnitureHitBox;
    private final ContextHolder.Builder contextBuilder;

    @ApiStatus.Internal
    public FurnitureInteractEvent(@NotNull Player player,
                                  @NotNull BukkitFurniture furniture,
                                  @NotNull InteractionHand hand,
                                  @NotNull Location interactionPoint,
                                  @NotNull FurnitureHitBox furnitureHitBox,
                                  @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.furniture = furniture;
        this.hand = hand;
        this.interactionPoint = interactionPoint;
        this.furnitureHitBox = furnitureHitBox;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
    }

    @NotNull
    public FurnitureHitBox hitBox() {
        return furnitureHitBox;
    }

    @NotNull
    public Location interactionPoint() {
        return this.interactionPoint.clone();
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    @NotNull
    public InteractionHand hand() {
        return hand;
    }

    @NotNull
    public BukkitFurniture furniture() {
        return this.furniture;
    }

    @NotNull
    public Location location() {
        return this.furniture.location();
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
