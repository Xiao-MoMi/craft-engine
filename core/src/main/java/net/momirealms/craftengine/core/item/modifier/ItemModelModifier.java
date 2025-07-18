package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class ItemModelModifier<I> implements ItemDataModifier<I> {
    private final Key data;

    public ItemModelModifier(Key data) {
        this.data = data;
    }

    @Override
    public String name() {
        return "item-model";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.itemModel(this.data.toString());
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        Tag previous = item.getSparrowNBTComponent(ComponentKeys.ITEM_MODEL);
        if (previous != null) {
            networkData.put(ComponentKeys.ITEM_MODEL.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
        } else {
            networkData.put(ComponentKeys.ITEM_MODEL.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
        }
        return item;
    }
}
