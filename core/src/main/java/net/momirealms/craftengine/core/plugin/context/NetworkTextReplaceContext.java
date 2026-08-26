package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;

public final class NetworkTextReplaceContext extends PlayerOptionalContext implements PlayerContext {

    public NetworkTextReplaceContext(@NotNull Player player) {
        super(player, ContextHolder.builder(DirectContextParameters.PLAYER, player).build());
    }

    public NetworkTextReplaceContext(@NotNull Player player, @NotNull ContextHolder contexts) {
        super(player, contexts);
    }

    public static @NotNull NetworkTextReplaceContext of(Player player) {
        return new NetworkTextReplaceContext(player);
    }

    public static @NotNull NetworkTextReplaceContext of(Player player, @NotNull ContextHolder contexts) {
        return new NetworkTextReplaceContext(player, contexts);
    }

    @Override
    public Player player() {
        return super.player;
    }

    @Override
    public boolean isPlayerPresent() {
        return true;
    }
}
