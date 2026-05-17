package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractCompositeLootEntryContainer extends AbstractLootEntryContainer {
    protected final List<LootEntryContainer> children;
    private final LootEntryContainer composedChildren;

    protected AbstractCompositeLootEntryContainer(List<Condition<LootContext>> conditions, List<LootEntryContainer> children) {
        super(conditions);
        this.children = children;
        this.composedChildren = compose(children);
    }

    protected abstract LootEntryContainer compose(List<? extends LootEntryContainer> children);

    @Override
    public final boolean expand(LootContext context, Consumer<LootEntry> choiceConsumer) {
        return this.test(context) && this.composedChildren.expand(context, choiceConsumer);
    }
}
