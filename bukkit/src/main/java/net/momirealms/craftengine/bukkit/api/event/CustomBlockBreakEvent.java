package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class CustomBlockBreakEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final CustomBlock customBlock;
    private final ImmutableBlockState state;
    private final Location location;
    private final Block bukkitBlock;
    private final BukkitServerPlayer player;
    private boolean cancelled;
    private boolean dropItems = true;
    private final ContextHolder.Builder contextBuilder;

    @ApiStatus.Internal
    public CustomBlockBreakEvent(@NotNull BukkitServerPlayer player,
                                 @NotNull Location location,
                                 @NotNull Block bukkitBlock,
                                 @NotNull ImmutableBlockState state,
                                 boolean dropItems,
                                 @NotNull ContextHolder.Builder contextBuilder) {
        super(player.platformPlayer());
        this.customBlock = state.owner().value();
        this.state = state;
        this.bukkitBlock = bukkitBlock;
        this.location = location;
        this.player = player;
        this.dropItems = dropItems;
        this.contextBuilder = contextBuilder;
    }

    @NotNull
    public ContextHolder.Builder contextBuilder() {
        return this.contextBuilder;
    }

    public BukkitServerPlayer player() {
        return this.player;
    }

    public boolean dropItems() {
        return this.dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    @NotNull
    public Block bukkitBlock() {
        return this.bukkitBlock;
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
