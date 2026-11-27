package net.momirealms.craftengine.core.item.processor.lore;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

public final class OverwritableLoreProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableLoreProcessor> FACTORY = new Factory();
    private final LoreProcessor loreProcessor;

    public OverwritableLoreProcessor(LoreProcessor loreProcessor) {
        this.loreProcessor = loreProcessor;
    }

    @Override
    public boolean shouldSkip(ItemPacketSource source) {
        return source.canSkipLore;
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
            if (item.hasNonDefaultComponent(DataComponentKeys.LORE)) {
                return false;
            }
        } else {
            if (item.hasTag("display", "Lore")) {
                return false;
            }
        }
        if (context instanceof NetworkItemBuildContext networkContext) {
            return this.loreProcessor.applyNetworkItem(networkContext, networkData);
        }
        this.loreProcessor.apply(context);
        return true;
    }

    @Override
    public boolean isConstant() {
        return this.loreProcessor.isConstant();
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.LORE;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return LoreProcessor.NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "display.Lore";
    }

    private static class Factory implements ItemProcessorFactory<OverwritableLoreProcessor> {
        @Override
        public OverwritableLoreProcessor create(ConfigValue value) {
            return new OverwritableLoreProcessor(LoreProcessor.createLoreModifier(value));
        }
    }
}
