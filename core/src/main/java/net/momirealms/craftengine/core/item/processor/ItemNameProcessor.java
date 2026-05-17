package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
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
    public Item apply(Item item, ItemBuildContext context) {
        item.itemNameComponent(this.line.parse(context));
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_NAME;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Name";
    }

    private static class Factory implements ItemProcessorFactory<ItemNameProcessor> {

        @Override
        public ItemNameProcessor create(ConfigValue value) {
            return new ItemNameProcessor(value.getAsString());
        }
    }
}
