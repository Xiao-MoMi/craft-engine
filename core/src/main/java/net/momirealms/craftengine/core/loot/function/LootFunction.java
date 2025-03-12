package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface LootFunction<T> extends BiFunction<Item<T>, LootContext, Item<T>> {

    Key type();

    static <T> Consumer<Item<T>> decorate(BiFunction<Item<T>, LootContext, Item<T>> itemApplier, Consumer<Item<T>> lootConsumer, LootContext context) {
        return item -> lootConsumer.accept(itemApplier.apply(item, context));
    }
}
