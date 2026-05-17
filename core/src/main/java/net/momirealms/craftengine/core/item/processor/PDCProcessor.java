package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;

public final class PDCProcessor implements ItemProcessor {
    public static final String BUKKIT_PDC = "PublicBukkitValues";
    public static final ItemProcessorFactory<PDCProcessor> FACTORY = new Factory();
    private final CompoundTag data;

    public PDCProcessor(CompoundTag data) {
        this.data = data;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_20_5) {
            CompoundTag customData = (CompoundTag) Optional.ofNullable(item.getComponentAsSparrowTag(DataComponentKeys.CUSTOM_DATA)).orElseGet(CompoundTag::new);
            customData.put(BUKKIT_PDC, this.data);
            item.setSparrowTagComponent(DataComponentKeys.CUSTOM_DATA, customData);
        } else {
            item.setTag(this.data, BUKKIT_PDC);
        }
        return item;
    }

    private static class Factory implements ItemProcessorFactory<PDCProcessor> {

        @Override
        public PDCProcessor create(ConfigValue value) {
            return new PDCProcessor((CompoundTag) CraftEngine.instance().platform().javaToSparrowNBT(value.getAsMap()));
        }
    }
}
