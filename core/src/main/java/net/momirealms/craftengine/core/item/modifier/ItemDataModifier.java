package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;

public interface ItemDataModifier<I> {

    Key type();

    Item<I> apply(Item<I> item, ItemBuildContext context);

    default Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        return item;
    }
}
