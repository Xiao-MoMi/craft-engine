package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

public final class IdProcessor implements ItemProcessor {
    public static final String CRAFT_ENGINE_ID = "craftengine:id";
    public static final ItemProcessorFactory<IdProcessor> FACTORY = new Factory();
    private final Key argument;

    public IdProcessor(Key argument) {
        this.argument = argument;
    }

    public Key identifier() {
        return this.argument;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.customId(this.argument);
        return item;
    }

    private static class Factory implements ItemProcessorFactory<IdProcessor> {

        @Override
        public IdProcessor create(ConfigValue value) {
            return new IdProcessor(value.getAsIdentifier());
        }
    }
}
