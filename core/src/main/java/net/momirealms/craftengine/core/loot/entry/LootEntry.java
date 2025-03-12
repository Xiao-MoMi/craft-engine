package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;

import java.util.function.Consumer;

public interface LootEntry<T> {

    int getWeight(float luck);

    void createItem(Consumer<Item<T>> lootConsumer, LootContext context);
}
