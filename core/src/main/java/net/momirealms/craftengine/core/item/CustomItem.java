package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemModifier;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CustomItem<I> extends BuildableItem<I> {

    Key id();

    Key material();

    List<ItemModifier<I>> modifiers();

    default I buildItemStack(Player player) {
        return this.buildItemStack(player, 1);
    }

    I buildItemStack(Player player, int count);

    default I buildItemStack() {
        return buildItemStack(null);
    }

    default I buildItemStack(int count) {
        return buildItemStack(null, count);
    }

    ItemSettings settings();

    Item<I> buildItem(Player player);

    @NotNull
    ItemBehavior behavior();

    interface Builder<I> {
        Builder<I> id(Key id);

        Builder<I> material(Key material);

        Builder<I> modifiers(List<ItemModifier<I>> modifiers);

        Builder<I> modifier(ItemModifier<I> modifier);

        Builder<I> behavior(ItemBehavior behavior);

        Builder<I> settings(ItemSettings settings);

        CustomItem<I> build();
    }
}
