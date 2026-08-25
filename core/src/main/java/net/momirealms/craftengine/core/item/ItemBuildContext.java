package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemBuildContext extends PlayerOptionalContext {
    public static final ItemBuildContext EMPTY = new ItemBuildContext(null, ContextHolder.empty());
    protected Item item;

    public ItemBuildContext(@Nullable Player player, @NotNull ContextHolder contexts) {
        super(player, contexts);
    }

    public void setItem(@NotNull Item item) {
        this.item = item;
        this.contexts.withParameter(DirectContextParameters.ITEM, item);
    }

    public Item item() {
        return this.item;
    }

    @NotNull
    public static ItemBuildContext empty() {
        return new ItemBuildContext(null, ContextHolder.empty());
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new ItemBuildContext(player, contexts);
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player, @NotNull ContextHolder.Builder builder) {
        if (player != null) {
            builder.withParameter(DirectContextParameters.PLAYER, player);
            builder.withParameter(DirectContextParameters.ENTITY, player);
        }
        return new ItemBuildContext(player, builder.build());
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player) {
        if (player == null) {
            return new ItemBuildContext(null, ContextHolder.empty());
        }
        return new ItemBuildContext(player, ContextHolder.builder()
                .withParameter(DirectContextParameters.PLAYER, player)
                .withParameter(DirectContextParameters.ENTITY, player)
                .build());
    }
}
