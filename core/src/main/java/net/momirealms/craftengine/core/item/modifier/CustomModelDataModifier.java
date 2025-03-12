package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public class CustomModelDataModifier<I> implements ItemModifier<I> {
    private final int argument;

    public CustomModelDataModifier(int argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "custom-model-data";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        item.customModelData(argument);
    }
}
