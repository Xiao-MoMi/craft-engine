package net.momirealms.craftengine.core.item.processor.lore;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

public final class OverwritableLoreProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableLoreProcessor> FACTORY = new Factory();
    private final LoreProcessor loreProcessor;

    public OverwritableLoreProcessor(LoreProcessor loreProcessor) {
        this.loreProcessor = loreProcessor;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(DataComponentKeys.LORE)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Lore")) {
                return item;
            }
        }
        return this.loreProcessor.apply(item, context);
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.LORE;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return LoreProcessor.NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Lore";
    }

    @Override
    public Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(DataComponentKeys.LORE)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Lore")) {
                return item;
            }
        }
        return SimpleNetworkItemProcessor.super.prepareNetworkItem(item, context, networkData);
    }

    private static class Factory implements ItemProcessorFactory<OverwritableLoreProcessor> {
        @Override
        public OverwritableLoreProcessor create(ConfigValue value) {
            return new OverwritableLoreProcessor(LoreProcessor.createLoreModifier(value));
        }
    }
}
