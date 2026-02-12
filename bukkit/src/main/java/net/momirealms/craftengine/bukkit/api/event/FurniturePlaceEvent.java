package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class FurniturePlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Location location;
    private final BukkitFurniture furniture;
    private final InteractionHand hand;
    private final ContextHolder.Builder contextBuilder;
    private boolean cancelled;

    @ApiStatus.Internal
    public FurniturePlaceEvent(@NotNull Player player,
                               @NotNull BukkitFurniture furniture,
                               @NotNull Location location,
                               @NotNull InteractionHand hand,
                               @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.location = location;
        this.furniture = furniture;
        this.hand = hand;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
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
        return this.location.clone();
    }

    @NotNull
    public Player player() {
        return getPlayer();
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
