package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;

import java.util.function.Consumer;

public interface LootEntryContainer {

    static LootEntryContainer alwaysFalse() {
        return (context, choiceConsumer) -> false;
    }

    static LootEntryContainer alwaysTrue() {
        return (context, choiceConsumer) -> true;
    }

    boolean expand(LootContext context, Consumer<LootEntry> choiceConsumer);

    default LootEntryContainer and(LootEntryContainer other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) && other.expand(context, lootChoiceExpander);
    }

    default LootEntryContainer or(LootEntryContainer other) {
        return (context, lootChoiceExpander) -> this.expand(context, lootChoiceExpander) || other.expand(context, lootChoiceExpander);
    }
}
