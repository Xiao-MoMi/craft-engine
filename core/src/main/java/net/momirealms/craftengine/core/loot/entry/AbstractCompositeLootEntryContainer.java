package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractCompositeLootEntryContainer<T> extends AbstractLootEntryContainer<T> {
    protected final List<LootEntryContainer<T>> children;
    private final LootEntryContainer<T> composedChildren;

    protected AbstractCompositeLootEntryContainer(List<LootCondition> conditions, List<LootEntryContainer<T>> children) {
        super(conditions);
        this.children = children;
        this.composedChildren = compose(children);
    }

    protected abstract LootEntryContainer<T> compose(List<? extends LootEntryContainer<T>> children);

    @Override
    public final boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer) {
        return this.test(context) && this.composedChildren.expand(context, choiceConsumer);
    }
}
