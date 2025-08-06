package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;

public class ApplyItemDataPostProcessor<T> implements PostProcessor<T> {
    private final ItemDataModifier<T>[] modifiers;

    public ApplyItemDataPostProcessor(ItemDataModifier<T>[] modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public Item<T> process(Item<T> item, ItemBuildContext context) {
        for (ItemDataModifier<T> modifier : this.modifiers) {
            item.apply(modifier, context);
        }
        return item;
    }
}