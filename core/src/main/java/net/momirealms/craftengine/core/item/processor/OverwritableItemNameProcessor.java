package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class OverwritableItemNameProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableItemNameProcessor> FACTORY = new Factory();
    private final ItemNameProcessor modifier;

    public OverwritableItemNameProcessor(String argument) {
        this.modifier = new ItemNameProcessor(argument);
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(DataComponentKeys.ITEM_NAME)) {
                return item;
            }
        } else {
            if (item.hasTag("display", "Name")) {
                return item;
            }
        }
        return this.modifier.apply(item, context);
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_NAME;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return new Object[]{"display", "Name"};
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Name";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableItemNameProcessor> {

        @Override
        public OverwritableItemNameProcessor create(ConfigValue value) {
            return new OverwritableItemNameProcessor(value.getAsString());
        }
    }
}
