package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public class ItemModelModifier<I> implements ItemModifier<I> {
    private final Key data;

    public ItemModelModifier(Key data) {
        this.data = data;
    }

    @Override
    public String name() {
        return "item-model";
    }

    @Override
    public void apply(Item<I> item, Player player) {
            item.setComponent(ComponentKeys.ITEM_MODEL, this.data.toString());
    }
}
