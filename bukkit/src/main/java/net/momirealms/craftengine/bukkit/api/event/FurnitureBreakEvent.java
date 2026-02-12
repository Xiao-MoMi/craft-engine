package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class FurnitureBreakEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final BukkitFurniture furniture;
    private final ContextHolder.Builder contextBuilder;
    private boolean cancelled;
    private boolean dropItems = true;

    @ApiStatus.Internal
    public FurnitureBreakEvent(@NotNull Player player,
                               @NotNull BukkitFurniture furniture,
                               @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.furniture = furniture;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    public boolean dropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
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
