package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLootConditionalFunction implements LootFunction {
    private final Predicate<LootContext> compositePredicates;

    protected AbstractLootConditionalFunction(List<Condition<LootContext>> predicates) {
        this.compositePredicates = MiscUtils.allOf(predicates);
    }

    @Override
    public Item apply(Item item, LootContext lootContext) {
        return this.compositePredicates.test(lootContext) ? this.applyInternal(item, lootContext) : item;
    }

    protected abstract Item applyInternal(Item item, LootContext context);
}
