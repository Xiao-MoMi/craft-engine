package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.minimessage.*;
import net.momirealms.craftengine.core.util.AdventureHelper;

public class ItemNameModifier<I> implements ItemModifier<I> {
    private final String argument;

    public ItemNameModifier(String argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "item-name";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        item.itemName(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(this.argument, context.tagResolvers())));
    }
}
