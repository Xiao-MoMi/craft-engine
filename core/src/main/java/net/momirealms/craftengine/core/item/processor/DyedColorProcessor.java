package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;

public final class DyedColorProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<DyedColorProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"display", "color"};
    private final Color color;

    public DyedColorProcessor(Color color) {
        this.color = color;
    }

    public Color dyedColor() {
        return color;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        return item.dyedColor(this.color);
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.DYED_COLOR;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.color";
    }

    private static class Factory implements ItemProcessorFactory<DyedColorProcessor> {

        @Override
        public DyedColorProcessor create(ConfigValue value) {
            return new DyedColorProcessor(value.getAsColor());
        }
    }
}
