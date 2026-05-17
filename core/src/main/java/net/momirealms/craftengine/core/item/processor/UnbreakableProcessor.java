package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

public final class UnbreakableProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<UnbreakableProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"Unbreakable"};
    private final boolean argument;

    public UnbreakableProcessor(boolean argument) {
        this.argument = argument;
    }

    public boolean unbreakable() {
        return argument;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.unbreakable(this.argument);
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.UNBREAKABLE;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "Unbreakable";
    }

    private static class Factory implements ItemProcessorFactory<UnbreakableProcessor> {

        @Override
        public UnbreakableProcessor create(ConfigValue value) {
            return new UnbreakableProcessor(value.getAsBoolean());
        }
    }
}
