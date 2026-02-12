package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class CustomBlockAttemptPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final CustomBlock customBlock;
    private final ImmutableBlockState state;
    private final Location location;
    private final BlockFace clickedFace;
    private final Block clickedBlock;
    private final InteractionHand hand;
    private final ContextHolder.Builder contextBuilder;

    @ApiStatus.Internal
    public CustomBlockAttemptPlaceEvent(@NotNull Player player,
                                        @NotNull Location location,
                                        @NotNull ImmutableBlockState state,
                                        @NotNull BlockFace clickedFace,
                                        @NotNull Block clickedBlock,
                                        @NotNull InteractionHand hand,
                                        @NotNull ContextHolder.Builder contextBuilder) {
        super(player);
        this.customBlock = state.owner().value();
        this.state = state;
        this.location = location;
        this.clickedFace = clickedFace;
        this.clickedBlock = clickedBlock;
        this.hand = hand;
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

    @NotNull
    public InteractionHand hand() {
        return this.hand;
    }

    @NotNull
    public Block clickedBlock() {
        return this.clickedBlock;
    }

    @NotNull
    public BlockFace clickedFace() {
        return this.clickedFace;
    }

    @NotNull
    public CustomBlock customBlock() {
        return this.customBlock;
    }

    @NotNull
    public Location location() {
        return this.location.clone();
    }

    @NotNull
    public ImmutableBlockState blockState() {
        return this.state;
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
