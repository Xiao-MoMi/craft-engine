package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface ItemUpdater {

    Item update(Item item, ItemBuildContext context);
}
