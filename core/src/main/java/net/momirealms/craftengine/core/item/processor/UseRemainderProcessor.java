package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

public final class UseRemainderProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<UseRemainderProcessor> FACTORY = new Factory();
    private final Key data;
    private final int count;

    public UseRemainderProcessor(Key data, int count) {
        this.data = data;
        this.count = count;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        Item wrappedItem = CraftEngine.instance().itemManager().createWrappedItem(data, context.player());
        if (wrappedItem != null) {
            return item.useRemainder(wrappedItem, count);
        } else {
            return item;
        }
    }

    private static class Factory implements ItemProcessorFactory<UseRemainderProcessor> {

        @Override
        public UseRemainderProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            return new UseRemainderProcessor(
                    section.getNonNullIdentifier("id"),
                    section.getInt("count", 1)
            );
        }
    }
}
