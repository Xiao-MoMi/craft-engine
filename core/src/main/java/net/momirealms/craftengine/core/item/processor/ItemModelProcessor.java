package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

public final class ItemModelProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<ItemModelProcessor> FACTORY = new Factory();
    private final Key data;

    public ItemModelProcessor(Key data) {
        this.data = data;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        return item.itemModel(this.data.asString());
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_MODEL;
    }

    private static class Factory implements ItemProcessorFactory<ItemModelProcessor> {

        @Override
        public ItemModelProcessor create(ConfigValue value) {
            return new ItemModelProcessor(value.getAsIdentifier());
        }
    }
}
