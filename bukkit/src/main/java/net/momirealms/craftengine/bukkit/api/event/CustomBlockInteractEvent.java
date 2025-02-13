package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomBlockInteractEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    private final CustomBlock block;
    private final ImmutableBlockState state;
    private final Location location;
    private final Location interactionPoint;
    private final InteractionHand hand;
    private final Action action;

    public CustomBlockInteractEvent(@NotNull Player player,
                                    @NotNull Location location,
                                    @Nullable Location interactionPoint,
                                    @NotNull ImmutableBlockState state,
                                    @NotNull InteractionHand hand,
                                    @NotNull Action action) {
        super(player);
        this.block = state.owner().value();
        this.state = state;
        this.location = location;
        this.interactionPoint = interactionPoint;
        this.hand = hand;
        this.action = action;
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
    public CustomBlock block() {
        return this.block;
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
