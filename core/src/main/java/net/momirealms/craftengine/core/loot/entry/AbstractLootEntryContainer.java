package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLootEntryContainer implements LootEntryContainer, Predicate<LootContext> {
    protected final List<Condition<LootContext>> conditions;
    private final Predicate<LootContext> compositeCondition;

    protected AbstractLootEntryContainer(List<Condition<LootContext>> conditions) {
        this.conditions = conditions;
        this.compositeCondition = MiscUtils.allOf(conditions);
    }

    @Override
    public final boolean test(LootContext context) {
        return this.compositeCondition.test(context);
    }
}