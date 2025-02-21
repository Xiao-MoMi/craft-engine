package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FurnitureBreakEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final LoadedFurniture furniture;

    public FurnitureBreakEvent(@NotNull Player player,
                               @NotNull LoadedFurniture furniture) {
        super(player);
        this.furniture = furniture;
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    @NotNull
    public LoadedFurniture furniture() {
        return this.furniture;
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
