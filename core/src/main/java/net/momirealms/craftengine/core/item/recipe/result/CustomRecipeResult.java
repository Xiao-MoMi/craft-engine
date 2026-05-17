package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public record CustomRecipeResult(BuildableItem item, int count, PostProcessor[] postProcessors) {

    public Item buildItem(ItemBuildContext context) {
        Item builtItem = this.item.buildItem(context, this.count);
        if (this.postProcessors != null) {
            for (PostProcessor postProcessor : this.postProcessors) {
                builtItem = postProcessor.process(builtItem, context);
            }
        }
        return builtItem;
    }
}