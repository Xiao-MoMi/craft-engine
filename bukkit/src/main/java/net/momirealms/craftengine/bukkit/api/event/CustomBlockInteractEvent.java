package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomBlockInteractEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final CustomBlock customBlock;
    private final Block bukkitBlock;
    private final ImmutableBlockState state;
    private final Location location;
    private final Location interactionPoint;
    private final InteractionHand hand;
    private final Action action;
    private final BlockFace clickedFace;

    public CustomBlockInteractEvent(@NotNull Player player,
                                    @NotNull Location location,
                                    @Nullable Location interactionPoint,
                                    @NotNull ImmutableBlockState state,
                                    @NotNull Block bukkitBlock,
                                    @NotNull BlockFace clickedFace,
                                    @NotNull InteractionHand hand,
                                    @NotNull Action action) {
        super(player);
        this.customBlock = state.owner().value();
        this.bukkitBlock = bukkitBlock;
        this.state = state;
        this.location = location;
        this.interactionPoint = interactionPoint;
        this.hand = hand;
        this.action = action;
        this.clickedFace = clickedFace;
    }

    @NotNull
    public BlockFace clickedFace() {
        return clickedFace;
    }

    @Nullable
    public Location interactionPoint() {
        return interactionPoint;
    }

    @NotNull
    public Action action() {
        return action;
    }

    @NotNull
    public InteractionHand hand() {
        return hand;
    }

    @NotNull
    public Block bukkitBlock() {
        return bukkitBlock;
    }

    @NotNull
    public CustomBlock customBlock() {
        return this.customBlock;
    }

    @NotNull
    public Player player() {
        return getPlayer();
    }

    @NotNull
    public Location location() {
        return this.location;
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

    public enum Action {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}
