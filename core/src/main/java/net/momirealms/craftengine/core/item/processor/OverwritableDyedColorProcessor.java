package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.joml.Vector3f;

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
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        Optional<Color> previous = item.dyedColor();
        if (previous.isPresent()) return item;
        return item.dyedColor(this.color);
    }

    @Override
    public <I> Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.DYED_COLOR;
    }

    @Override
    public <I> Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public <I> String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.color";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableDyedColorProcessor> {

        @Override
        public OverwritableDyedColorProcessor create(Object arg) {
            if (arg instanceof Integer integer) {
                return new OverwritableDyedColorProcessor(Color.fromDecimal(integer));
            } else {
                Vector3f vector3f = ResourceConfigUtils.getAsVector3f(arg, "dyed-color");
                return new OverwritableDyedColorProcessor(Color.fromVector3f(vector3f));
            }
        }
    }
}
