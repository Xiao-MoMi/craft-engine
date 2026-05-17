package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class FurnitureHitEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final BukkitFurniture furniture;
    private final FurnitureHitBox hitBox;
    private final Location hitPoint;
    private final ContextHolder.Builder contextBuilder;

    public FurnitureHitEvent(@NotNull Player player,
                             @NotNull BukkitFurniture furniture,
                             @NotNull Location hitPoint,
                             @NotNull FurnitureHitBox hitBox,
                             @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.furniture = furniture;
        this.hitBox = hitBox;
        this.hitPoint = hitPoint;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
    }

    @NotNull
    public FurnitureHitBox hitBox() {
        return this.hitBox;
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    @NotNull
    public Location hitPoint() {
        return this.hitPoint;
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
