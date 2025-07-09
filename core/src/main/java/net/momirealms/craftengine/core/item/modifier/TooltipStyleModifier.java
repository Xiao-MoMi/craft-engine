package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class TooltipStyleModifier<I> implements ItemDataModifier<I> {
    private final Key argument;

    public TooltipStyleModifier(Key argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "tooltip-style";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.tooltipStyle(argument.toString());
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        Tag previous = item.getSparrowNBTComponent(ComponentKeys.TOOLTIP_STYLE);
        if (previous != null) {
            networkData.put(ComponentKeys.TOOLTIP_STYLE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
        } else {
            networkData.put(ComponentKeys.TOOLTIP_STYLE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
        }
        return item;
    }
}
