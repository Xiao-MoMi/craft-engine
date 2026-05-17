package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

public final class TooltipStyleProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<TooltipStyleProcessor> FACTORY = new Factory();
    private final Key argument;

    public TooltipStyleProcessor(Key argument) {
        this.argument = argument;
    }

    public Key tooltipStyle() {
        return this.argument;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.tooltipStyle(argument.toString());
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.TOOLTIP_STYLE;
    }

    private static class Factory implements ItemProcessorFactory<TooltipStyleProcessor> {

        @Override
        public TooltipStyleProcessor create(ConfigValue value) {
            return new TooltipStyleProcessor(value.getAsIdentifier());
        }
    }
}
