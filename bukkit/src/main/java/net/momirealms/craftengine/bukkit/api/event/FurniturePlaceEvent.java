package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FurniturePlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Location location;
    private final LoadedFurniture furniture;
    private final InteractionHand hand;
    private boolean cancelled;

    public FurniturePlaceEvent(@NotNull Player player,
                               @NotNull LoadedFurniture furniture,
                               @NotNull Location location,
                               @NotNull InteractionHand hand) {
        super(player);
        this.location = location;
        this.furniture = furniture;
        this.hand = hand;
    }

    @NotNull
    public InteractionHand hand() {
        return hand;
    }

    @NotNull
    public LoadedFurniture furniture() {
        return furniture;
    }

    @NotNull
    public Location location() {
        return location;
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
