package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.context.ContextHolder;

public interface BuildableItem<I> {

    I buildItemStack(ItemBuildContext context, int count);

    default I buildItemStack(ItemBuildContext context) {
        return buildItemStack(context, 1);
    }

    default I buildItemStack() {
        return buildItemStack(ItemBuildContext.EMPTY, 1);
    }

    default I buildItemStack(int count) {
        return buildItemStack(ItemBuildContext.EMPTY, count);
    }

    default I buildItemStack(Player player) {
        return this.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), 1);
    }

    default I buildItemStack(Player player, int count) {
        return this.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), count);
    }
}
