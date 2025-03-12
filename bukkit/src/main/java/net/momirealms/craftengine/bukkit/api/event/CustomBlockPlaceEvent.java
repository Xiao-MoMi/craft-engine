package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class CustomBlockPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final CustomBlock customBlock;
    private final ImmutableBlockState state;
    private final Location location;
    private final InteractionHand hand;
    private final Block bukkitBlock;
    private boolean cancelled;

    public CustomBlockPlaceEvent(@NotNull Player player,
                                 @NotNull Location location,
                                 @NotNull ImmutableBlockState state,
                                 @NotNull Block bukkitBlock,
                                 @NotNull InteractionHand hand) {
        super(player);
        this.customBlock = state.owner().value();
        this.state = state;
        this.location = location;
        this.hand = hand;
        this.bukkitBlock = bukkitBlock;
    }

    @NotNull
    public Block bukkitBlock() {
        return bukkitBlock;
    }

    @NotNull
    public InteractionHand hand() {
        return hand;
    }

    @NotNull
    public CustomBlock customBlock() {
        return this.customBlock;
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
