package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.sparrow.nbt.CompoundTag;

public interface ItemProcessor {

    Item apply(Item item, ItemBuildContext context);

    default Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        return item;
    }
}
