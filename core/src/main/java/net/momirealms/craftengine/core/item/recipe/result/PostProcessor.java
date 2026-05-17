package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface PostProcessor {

    Item process(Item item, ItemBuildContext context);
}
