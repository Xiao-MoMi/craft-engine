package net.momirealms.craftengine.core.plugin.context;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

public final class NetworkTextReplaceContext extends PlayerOptionalContext implements PlayerContext {

    public NetworkTextReplaceContext(Player player) {
        super(player, ContextHolder.trustedMutable(MiscUtils.init(new Object2ObjectOpenHashMap<>(4), (m) -> {
            ContextHolder.SimpleSupplier<Object> supplier = ContextHolder.SimpleSupplier.of(player);
            m.put(DirectContextParameters.PLAYER, supplier);
            m.put(DirectContextParameters.ENTITY, supplier);
        })));
    }

    public static @NotNull NetworkTextReplaceContext of(Player player) {
        return new NetworkTextReplaceContext(player);
    }

    @Override
    public Player player() {
        return super.player;
    }
}
