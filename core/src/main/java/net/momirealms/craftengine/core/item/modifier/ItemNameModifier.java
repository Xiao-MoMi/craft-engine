package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class ItemNameModifier<I> implements ItemDataModifier<I> {
    private final String argument;

    public ItemNameModifier(String argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "item-name";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.itemNameComponent(AdventureHelper.miniMessage().deserialize(this.argument, context.tagResolvers()));
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.ITEM_NAME);
            if (previous != null) {
                networkData.put(ComponentKeys.ITEM_NAME.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.ITEM_NAME.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getNBTTag("display", "Name");
            if (previous != null) {
                networkData.put("display.Name", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("display.Name", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
