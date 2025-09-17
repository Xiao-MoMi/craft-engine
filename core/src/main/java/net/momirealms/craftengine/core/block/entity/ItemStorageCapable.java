package net.momirealms.craftengine.core.block.entity;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;

public interface ItemStorageCapable {

    Key storageKey();

    default boolean hasPermission(Player player) {
        return player.isOp();
    }

    default void saveCustomDataToItem(Item<?> item) {
        if (storageKey() == null) return;
        CompoundTag tag = new CompoundTag();
        if (this instanceof BlockEntity blockEntity) {
            blockEntity.saveCustomData(tag);
        }
        item.setTag(tag, storageKey());
    }

    default void loadCustomDataFromItem(Item<?> item) {
        if (storageKey() == null) return;
        if (!(item.getTag(storageKey()) instanceof CompoundTag storageData)) return;
        if (this instanceof BlockEntity blockEntity) {
            blockEntity.loadCustomData(storageData);
        }
    }
}
