package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

public final class OverwritableItemNameProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableItemNameProcessor> FACTORY = new Factory();
    private final ItemNameProcessor modifier;

    public OverwritableItemNameProcessor(String argument) {
        this.modifier = new ItemNameProcessor(argument);
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
        if (VersionHelper.COMPONENT_RELEASE) {
            if (item.hasNonDefaultComponent(DataComponentKeys.ITEM_NAME)) {
                return false;
            }
        } else {
            if (item.hasTag("display", "Name")) {
                return false;
            }
        }
        if (networkData != null) {
            this.prepareNetworkItem((NetworkItemBuildContext) context, networkData);
        }
        this.modifier.apply(context);
        return true;
    }

    @Override
    public boolean shouldSkip(ItemPacketSource source) {
        return source.canSkipName;
    }

    @Override
    public boolean isConstant() {
        return this.modifier.isConstant();
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ITEM_NAME;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return new Object[]{"display", "Name"};
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Name";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableItemNameProcessor> {

        @Override
        public OverwritableItemNameProcessor create(ConfigValue value) {
            return new OverwritableItemNameProcessor(value.getAsString());
        }
    }
}
