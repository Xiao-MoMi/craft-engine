package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class FurnitureAttemptPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final CustomFurniture furniture;
    private final Location location;
    private final FurnitureVariant variant;
    private final Block clickedBlock;
    private final InteractionHand hand;
    private final ContextHolder.Builder contextBuilder;

    @ApiStatus.Internal
    public FurnitureAttemptPlaceEvent(@NotNull Player player,
                                      @NotNull CustomFurniture furniture,
                                      @NotNull FurnitureVariant variant,
                                      @NotNull Location location,
                                      @NotNull InteractionHand hand,
                                      @NotNull Block clickedBlock,
                                      @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.furniture = furniture;
        this.location = location;
        this.variant = variant;
        this.clickedBlock = clickedBlock;
        this.hand = hand;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
    }

    @NotNull
    public Block clickedBlock() {
        return this.clickedBlock;
    }

    @NotNull
    public InteractionHand hand() {
        return this.hand;
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    @NotNull
    public FurnitureVariant variant() {
        return this.variant;
    }

    @NotNull
    public Location location() {
        return this.location.clone();
    }

    @NotNull
    public CustomFurniture furniture() {
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
