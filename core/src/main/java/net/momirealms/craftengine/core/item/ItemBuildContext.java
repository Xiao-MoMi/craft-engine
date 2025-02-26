package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemBuildContext {
    public static final ItemBuildContext EMPTY = new ItemBuildContext(null, ContextHolder.EMPTY);
    private final Player player;
    private final ContextHolder contexts;

    public ItemBuildContext(@Nullable Player player, @NotNull ContextHolder contexts) {
        this.player = player;
        this.contexts = contexts;
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new ItemBuildContext(player, contexts);
    }

    @Nullable
    public Player player() {
        return player;
    }

    @NotNull
    public ContextHolder contexts() {
        return contexts;
    }
}
