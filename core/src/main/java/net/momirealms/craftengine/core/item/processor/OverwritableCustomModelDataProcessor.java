package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;

public final class OverwritableCustomModelDataProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableCustomModelDataProcessor> FACTORY = new Factory();
    private final NumberProvider argument;

    public OverwritableCustomModelDataProcessor(NumberProvider argument) {
        this.argument = argument;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (item.customModelData().isPresent()) return item;
        item.customModelData(this.argument.getInt(context));
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.CUSTOM_MODEL_DATA;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return new Object[]{"CustomModelData"};
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "CustomModelData";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableCustomModelDataProcessor> {

        @Override
        public OverwritableCustomModelDataProcessor create(ConfigValue value) {
            return new OverwritableCustomModelDataProcessor(value.getAsNumber());
        }
    }
}
