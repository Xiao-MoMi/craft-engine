package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public final class OverwritableCustomModelDataProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableCustomModelDataProcessor> FACTORY = new Factory();
    private final NumberProvider argument;

    public OverwritableCustomModelDataProcessor(NumberProvider argument) {
        this.argument = argument;
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
        if (item.customModelData().isPresent()) {
            return false;
        }
        if (networkData != null) {
            this.prepareNetworkItem((NetworkItemBuildContext) context, networkData);
        }
        item.customModelData(this.argument.getInt(context));
        return true;
    }

    @Override
    public boolean isConstant() {
        return this.argument.isConstant();
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.CUSTOM_MODEL_DATA;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return new Object[]{"CustomModelData"};
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "CustomModelData";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableCustomModelDataProcessor> {

        @Override
        public OverwritableCustomModelDataProcessor create(ConfigValue value) {
            return new OverwritableCustomModelDataProcessor(value.getAsNumber());
        }
    }
}
