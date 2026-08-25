package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;

public class NetworkItemBuildContext extends ItemBuildContext {

    public NetworkItemBuildContext(@NotNull Player player, Item item, @NotNull ContextHolder contexts) {
        super(player, contexts);
        super.item = item;
    }

    public static NetworkItemBuildContext of(Player player, Item item) {
        return new NetworkItemBuildContext(player, item, ContextHolder.builder()
                .withParameter(DirectContextParameters.PLAYER, player)
                .withParameter(DirectContextParameters.ENTITY, player)
                .withParameter(DirectContextParameters.ITEM, item)
                .build());
    }
}
