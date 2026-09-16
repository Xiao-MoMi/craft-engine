package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.sparrow.nbt.CompoundTag;

public interface ItemProcessor {

    void apply(ItemBuildContext context);

    default boolean isConstant() {
        return false;
    }

    default void prepareNetworkItem(NetworkItemBuildContext context, CompoundTag networkData) {
    }

    default boolean shouldSkip(ItemPacketSource source) {
        return false;
    }
}
