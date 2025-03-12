package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface ItemModifier<I> {

    String name();

    void apply(Item<I> item, ItemBuildContext context);
}
