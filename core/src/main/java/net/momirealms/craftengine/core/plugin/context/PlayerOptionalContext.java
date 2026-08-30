package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerOptionalContext extends AbstractChainParameterContext implements PlayerContext {
    public static final PlayerOptionalContext EMPTY = new PlayerOptionalContext(null, ContextHolder.emptyImmutable());
    protected final Player player;

    public PlayerOptionalContext(@Nullable Player player,
                                 @NotNull ContextHolder contexts) {
        super(contexts);
        this.player = player;
    }

    @NotNull
    public static PlayerOptionalContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new PlayerOptionalContext(player, contexts);
    }

    @NotNull
    public static PlayerOptionalContext of(@Nullable Player player, @NotNull ContextHolder.Builder builder) {
        if (player != null) {
            builder.withParameter(DirectContextParameters.PLAYER, player);
        }
        return new PlayerOptionalContext(player, builder.build());
    }

    @NotNull
    public static PlayerOptionalContext of(@Nullable Player player) {
        if (player == null) return empty();
        return new PlayerOptionalContext(player, ContextHolder.builder(DirectContextParameters.PLAYER, player).build());
    }

    @NotNull
    public static PlayerOptionalContext empty() {
        return new PlayerOptionalContext(null, ContextHolder.empty());
    }

    public static PlayerOptionalContext emptyImmutable() {
        return EMPTY;
    }

    @Override
    @Nullable
    public Player player() {
        return this.player;
    }

    public boolean isPlayerPresent() {
        return this.player != null;
    }
}
