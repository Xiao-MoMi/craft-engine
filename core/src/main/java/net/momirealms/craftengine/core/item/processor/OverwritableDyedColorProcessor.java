package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

public final class OverwritableDyedColorProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableDyedColorProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"display", "color"};
    private final Color color;

    public OverwritableDyedColorProcessor(Color color) {
        this.color = color;
    }

    public Color dyedColor() {
        return color;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        Optional<Color> previous = item.dyedColor();
        if (previous.isPresent()) return item;
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

    private static class Factory implements ItemProcessorFactory<OverwritableDyedColorProcessor> {

        @Override
        public OverwritableDyedColorProcessor create(ConfigValue value) {
            return new OverwritableDyedColorProcessor(value.getAsColor());
        }
    }
}
