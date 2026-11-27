package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public final class OverwritableItemModelProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableItemModelProcessor> FACTORY = new Factory();
    private final Key data;

    public OverwritableItemModelProcessor(Key data) {
        this.data = data;
    }

    public Key data() {
        return data;
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
        if (item.hasNonDefaultComponent(DataComponentKeys.ITEM_MODEL)) {
            return false;
        }
        if (networkData != null) {
            this.prepareNetworkItem((NetworkItemBuildContext) context, networkData);
        }
        item.itemModel(this.data.asString());
        return true;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_MODEL;
    }

    private static class Factory implements ItemProcessorFactory<OverwritableItemModelProcessor> {

        @Override
        public OverwritableItemModelProcessor create(ConfigValue value) {
            return new OverwritableItemModelProcessor(value.getAsIdentifier());
        }
    }
}
