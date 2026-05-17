package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface LootFunction extends BiFunction<Item, LootContext, Item> {

    static Consumer<Item> decorate(BiFunction<Item, LootContext, Item> itemApplier, Consumer<Item> lootConsumer, LootContext context) {
        return item -> lootConsumer.accept(itemApplier.apply(item, context));
    }
}
