package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class CustomModelDataModifier<I> implements ItemDataModifier<I> {
    private final int argument;

    public CustomModelDataModifier(int argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "custom-model-data";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.customModelData(argument);
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.CUSTOM_MODEL_DATA);
            if (previous != null) {
                networkData.put(ComponentKeys.CUSTOM_MODEL_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.CUSTOM_MODEL_DATA.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getNBTTag("CustomModelData");
            if (previous != null) {
                networkData.put("CustomModelData", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("CustomModelData", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
