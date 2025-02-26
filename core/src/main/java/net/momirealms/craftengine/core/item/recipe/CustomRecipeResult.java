package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public record CustomRecipeResult<T>(BuildableItem<T> item, int count) {

    public T buildItemStack(ItemBuildContext context) {
        return item.buildItemStack(context, count);
    }
}
