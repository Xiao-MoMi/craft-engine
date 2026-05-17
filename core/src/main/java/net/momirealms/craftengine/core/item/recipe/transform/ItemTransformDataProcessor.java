package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.TriConsumer;

public interface ItemTransformDataProcessor extends TriConsumer<Item, Item, Item> {

    interface Factory<T extends ItemTransformDataProcessor> {
        T create(ConfigSection section);
    }

    record Type<T extends ItemTransformDataProcessor>(Key id, Factory<T> factory) {}
}
