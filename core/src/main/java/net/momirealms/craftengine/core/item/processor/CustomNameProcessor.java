package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

public final class CustomNameProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<CustomNameProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"display", "Name"};
    private final String argument;
    private final FormattedLine line;

    public CustomNameProcessor(String argument) {
        argument = AdventureHelper.legacyToMiniMessage(argument);
        if (Config.addNonItalicTag()) {
            if (argument.startsWith("<!i>")) {
                this.argument = argument;
            } else {
                this.argument  = "<!i>" + argument;
            }
        } else {
            this.argument = argument;
        }
        this.line = FormattedLine.create(this.argument);
    }

    public String customName() {
        return this.argument;
    }

    @Override
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.customNameComponent(this.line.parse(context));
        return item;
    }

    @Override
    public <I> Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.CUSTOM_NAME;
    }

    @Override
    public <I> Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public <I> String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Name";
    }

    private static class Factory implements ItemProcessorFactory<CustomNameProcessor> {

        @Override
        public CustomNameProcessor create(Object arg) {
            String name = arg.toString();
            return new CustomNameProcessor(name);
        }
    }
}
