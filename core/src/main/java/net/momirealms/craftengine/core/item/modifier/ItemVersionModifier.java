package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;

public class ItemVersionModifier<I> implements ItemDataModifier<I> {
    public static final String VERSION_TAG = "craftengine:version";
    private final int version;

    public ItemVersionModifier(int version) {
        this.version = version;
    }

    public int version() {
        return this.version;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.VERSION;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_20_5()) {
            CompoundTag customData = (CompoundTag) Optional.ofNullable(item.getSparrowNBTComponent(ComponentKeys.CUSTOM_DATA)).orElseGet(CompoundTag::new);
            customData.putInt(VERSION_TAG, this.version);
            item.setNBTComponent(ComponentKeys.CUSTOM_DATA, customData);
        } else {
            item.setTag(this.version, VERSION_TAG);
        }
        return item;
    }
}
