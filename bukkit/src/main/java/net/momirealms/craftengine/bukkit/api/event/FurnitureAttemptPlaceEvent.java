package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FurnitureAttemptPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final CustomFurniture furniture;
    private final Location location;
    private final AnchorType anchorType;
    private final BlockFace clickedFace;
    private final Block clickedBlock;
    private final InteractionHand hand;

    public FurnitureAttemptPlaceEvent(@NotNull Player player,
                                      @NotNull CustomFurniture furniture,
                                      @NotNull AnchorType anchorType,
                                      @NotNull Location location,
                                      @NotNull BlockFace clickedFace,
                                      @NotNull InteractionHand hand,
                                      @NotNull Block clickedBlock) {
        super(player);
        this.furniture = furniture;
        this.location = location;
        this.anchorType = anchorType;
        this.clickedFace = clickedFace;
        this.clickedBlock = clickedBlock;
        this.hand = hand;
    }

    @NotNull
    public Block clickedBlock() {
        return clickedBlock;
    }

    @NotNull
    public InteractionHand hand() {
        return hand;
    }

    @NotNull
    public BlockFace clickedFace() {
        return clickedFace;
    }

    @NotNull
    public Player player() {
        return getPlayer();
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
    public CustomFurniture furniture() {
        return furniture;
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
