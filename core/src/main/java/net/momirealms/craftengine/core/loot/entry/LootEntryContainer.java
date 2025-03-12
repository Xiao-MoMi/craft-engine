package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;

import java.util.function.Consumer;

public interface LootEntryContainer<T> {

    static <T> LootEntryContainer<T> alwaysFalse() {
        return (context, choiceConsumer) -> false;
    }

    static <T> LootEntryContainer<T> alwaysTrue() {
        return (context, choiceConsumer) -> true;
    }

    boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer);

    default LootEntryContainer<T> and(LootEntryContainer<T> other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) && other.expand(context, lootChoiceExpander);
    }

    default LootEntryContainer<T> or(LootEntryContainer<T> other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) || other.expand(context, lootChoiceExpander);
    }
}
