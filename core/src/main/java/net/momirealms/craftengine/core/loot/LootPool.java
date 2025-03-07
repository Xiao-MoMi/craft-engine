package net.momirealms.craftengine.core.loot;

import com.google.common.collect.Lists;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.entry.LootEntry;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainer;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.loot.number.NumberProvider;
import net.momirealms.craftengine.core.util.MCUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LootPool<T> {
    private final List<LootEntryContainer<T>> entryContainers;
    private final List<LootCondition> conditions;
    private final Predicate<LootContext> compositeCondition;
    private final List<LootFunction<T>> functions;
    private final BiFunction<Item<T>, LootContext, Item<T>> compositeFunction;
    private final NumberProvider rolls;
    private final NumberProvider bonusRolls;

    public LootPool(List<LootEntryContainer<T>> entryContainers, List<LootCondition> conditions, List<LootFunction<T>> functions, NumberProvider rolls, NumberProvider bonusRolls) {
        this.entryContainers = entryContainers;
        this.conditions = conditions;
        this.functions = functions;
        this.rolls = rolls;
        this.bonusRolls = bonusRolls;
        this.compositeCondition = LootConditions.andConditions(conditions);
        this.compositeFunction = LootFunctions.compose(functions);
    }

    public void addRandomItems(Consumer<Item<T>> lootConsumer, LootContext context) {
        for (LootCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return;
            }
        }
        if (this.compositeCondition.test(context)) {
            Consumer<Item<T>> consumer = LootFunction.decorate(this.compositeFunction, lootConsumer, context);
            int i = this.rolls.getInt(context) + MCUtils.fastFloor(this.bonusRolls.getFloat(context) * context.luck());
            for (int j = 0; j < i; ++j) {
                this.addRandomItem(createFunctionApplier(consumer, context), context);
            }
        }
    }

    private Consumer<Item<T>> createFunctionApplier(Consumer<Item<T>> lootConsumer, LootContext context) {
        return (item -> {
            for (LootFunction<T> function : this.functions) {
                function.apply(item, context);
            }
            lootConsumer.accept(item);
        });
    }

    private void addRandomItem(Consumer<Item<T>> lootConsumer, LootContext context) {
        Random randomSource = context.randomSource();
        List<LootEntry<T>> list = Lists.newArrayList();
        MutableInt mutableInt = new MutableInt();
        for (LootEntryContainer<T> lootPoolEntryContainer : this.entryContainers) {
            lootPoolEntryContainer.expand(context, (choice) -> {
                int i = choice.getWeight(context.luck());
                if (i > 0) {
                    list.add(choice);
                    mutableInt.add(i);
                }
            });
        }
        int i = list.size();
        if (mutableInt.intValue() != 0 && i != 0) {
            if (i == 1) {
                list.get(0).createItem(lootConsumer, context);
            } else {
                int j = randomSource.nextInt(mutableInt.intValue());
                for (LootEntry<T> loot : list) {
                    j -= loot.getWeight(context.luck());
                    if (j < 0) {
                        loot.createItem(lootConsumer, context);
                        return;
                    }
                }
            }
        }
    }
}
