package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.sparrow.nbt.CompoundTag;

public interface ItemProcessor {

    void apply(ItemBuildContext context);

    default void prepareNetworkItem(NetworkItemBuildContext context, CompoundTag networkData) {
    }
}
