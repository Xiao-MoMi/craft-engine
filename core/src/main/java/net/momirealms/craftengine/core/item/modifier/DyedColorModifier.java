package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class DyedColorModifier<I> implements ItemDataModifier<I> {
    private final int color;

    public DyedColorModifier(int color) {
        this.color = color;
    }

    @Override
    public String name() {
        return "dyed-color";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.dyedColor(this.color);
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.DYED_COLOR);
            if (previous != null) {
                networkData.put(ComponentKeys.DYED_COLOR.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.DYED_COLOR.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getTag("display", "color");
            if (previous != null) {
                networkData.put("display.color", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("display.color", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
