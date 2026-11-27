package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkItemBuildContext extends ItemBuildContext {
    private final ItemPacketSource source;

    public NetworkItemBuildContext(@Nullable Player player, Item item, @NotNull ContextHolder contexts) {
        this(player, item, contexts, ItemPacketSource.GENERIC);
    }

    public NetworkItemBuildContext(@Nullable Player player, Item item, @NotNull ContextHolder contexts, ItemPacketSource source) {
        super(player, contexts);
        super.item = item;
        this.source = source;
    }

    public ItemPacketSource source() {
        return this.source;
    }

    public static NetworkItemBuildContext of(Player player, Item item) {
        return of(player, item, ItemPacketSource.GENERIC);
    }

    public static NetworkItemBuildContext of(@Nullable Player player, Item item, ItemPacketSource source) {
        return new NetworkItemBuildContext(player, item, ContextHolder.builder(
                DirectContextParameters.PLAYER, player,
                DirectContextParameters.ENTITY, player,
                DirectContextParameters.ITEM, item
        ).build(), source);
    }
}
