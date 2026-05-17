package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;

public final class ItemVersionProcessor implements ItemProcessor {
    public static final String VERSION_TAG = "craftengine:version";
    private final int version;

    public ItemVersionProcessor(int version) {
        this.version = version;
    }

    public int version() {
        return this.version;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_20_5) {
            CompoundTag customData = (CompoundTag) Optional.ofNullable(item.getComponentAsSparrowTag(DataComponentKeys.CUSTOM_DATA)).orElseGet(CompoundTag::new);
            customData.putInt(VERSION_TAG, this.version);
            item.setSparrowTagComponent(DataComponentKeys.CUSTOM_DATA, customData);
        } else {
            item.setTag(this.version, VERSION_TAG);
        }
        return item;
    }
}
