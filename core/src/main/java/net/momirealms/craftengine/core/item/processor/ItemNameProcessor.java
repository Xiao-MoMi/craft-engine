package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

public final class ItemNameProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<ItemNameProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"display", "Name"};
    private final String argument;
    private final FormattedLine line;

    public ItemNameProcessor(String argument) {
        this.argument = AdventureHelper.legacyToMiniMessage(argument);
        this.line = FormattedLine.create(this.argument);
    }

    public String itemName() {
        return this.argument;
    }

    @Override
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.itemNameComponent(this.line.parse(context));
        return item;
    }

    @Override
    public <I> Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_NAME;
    }

    @Override
    public <I> Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public <I> String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Name";
    }

    private static class Factory implements ItemProcessorFactory<ItemNameProcessor> {

        @Override
        public ItemNameProcessor create(Object arg) {
            String name = arg.toString();
            return new ItemNameProcessor(name);
        }
    }
}
