package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class OverwritableDyedColorProcessor implements NetworkItemProcessor {
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
    public void apply(ItemBuildContext context) {
        this.apply(context, null);
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        return this.apply(context, networkData);
    }

    private boolean apply(ItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        Optional<Color> previous = item.dyedColor();
        if (previous.isPresent()) {
            return false;
        }
        if (networkData != null) {
            this.prepareNetworkItem((NetworkItemBuildContext) context, networkData);
        }
        item.dyedColor(this.color);
        return true;
    }

    @Override
    public boolean isConstant() {
        return true;
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
