package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.item.behavior.FurnitureItemBehavior;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FurnitureAttemptPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final FurnitureItemBehavior behavior;
    private final Location location;
    private final AnchorType anchorType;

    public FurnitureAttemptPlaceEvent(@NotNull Player player, @NotNull FurnitureItemBehavior behavior, @NotNull AnchorType anchorType, @NotNull Location location) {
        super(player);
        this.behavior = behavior;
        this.location = location;
        this.anchorType = anchorType;
    }

    @NotNull
    public AnchorType anchorType() {
        return anchorType;
    }

    @NotNull
    public Location location() {
        return location;
    }

    @NotNull
    public FurnitureItemBehavior behavior() {
        return behavior;
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
