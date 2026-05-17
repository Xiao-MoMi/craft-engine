package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;

public final class MaxDamageProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<MaxDamageProcessor> FACTORY = new Factory();
    private final NumberProvider argument;

    public MaxDamageProcessor(NumberProvider argument) {
        this.argument = argument;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.maxDamage(this.argument.getInt(context));
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.MAX_DAMAGE;
    }

    private static class Factory implements ItemProcessorFactory<MaxDamageProcessor> {

        @Override
        public MaxDamageProcessor create(ConfigValue value) {
            return new MaxDamageProcessor(value.getAsNumber());
        }
    }
}
