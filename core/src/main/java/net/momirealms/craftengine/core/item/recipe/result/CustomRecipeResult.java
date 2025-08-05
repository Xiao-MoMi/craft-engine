package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public record CustomRecipeResult<T>(BuildableItem<T> item, int count, PostProcessor<T>[] postProcessors) {

    public T buildItemStack(ItemBuildContext context) {
        return buildItem(context).getItem();
    }

    public Item<T> buildItem(ItemBuildContext context) {
        Item<T> builtItem = this.item.buildItem(context, this.count);
        if (this.postProcessors != null) {
            for (PostProcessor<T> postProcessor : this.postProcessors) {
                builtItem = postProcessor.process(builtItem, context);
            }
        }
        return builtItem;
    }
}