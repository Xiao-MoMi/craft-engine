package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FurniturePlaceEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Location location;
    private final LoadedFurniture furniture;

    public FurniturePlaceEvent(@NotNull Player player, @NotNull LoadedFurniture furniture, @NotNull Location location) {
        super(player);
        this.player = player;
        this.location = location;
        this.furniture = furniture;
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
        return this.player;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
