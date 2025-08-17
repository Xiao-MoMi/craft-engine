package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface ItemUpdater<I> {

    Item<I> update(Item<I> item, ItemBuildContext context);
}
