package net.momirealms.craftengine.core.item.recipe.result;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

import java.util.Map;

public interface PostProcessor<T> {

    Item<T> process(Item<T> item, ItemBuildContext context);

    interface Type<T> {

        PostProcessor<T> create(Map<String, Object> args);
    }
}
