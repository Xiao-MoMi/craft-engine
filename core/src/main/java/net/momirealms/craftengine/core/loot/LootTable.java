package net.momirealms.craftengine.core.loot;

import com.google.common.collect.Lists;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainer;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainers;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.loot.provider.NumberProvider;
import net.momirealms.craftengine.core.loot.provider.NumberProviders;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class LootTable<T> {
    private final List<LootPool<T>> pools;
    private final List<LootFunction<T>> functions;
    private final BiFunction<Item<T>, LootContext, Item<T>> compositeFunction;

    public LootTable(List<LootPool<T>> pools, List<LootFunction<T>> functions) {
        this.pools = pools;
        this.functions = functions;
        this.compositeFunction = LootFunctions.compose(functions);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> LootTable<T> fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        List<Map<String, Object>> poolList = (List<Map<String, Object>>) map.get("pools");
        List<LootPool<T>> lootPools = new ArrayList<>();
        for (Map<String, Object> pool : poolList) {
            NumberProvider rolls = NumberProviders.fromObject(pool.getOrDefault("rolls", 1));
            NumberProvider bonus_rolls = NumberProviders.fromObject(pool.getOrDefault("bonus_rolls", 0));
            List<LootCondition> conditions = Optional.ofNullable(pool.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Lists.newArrayList());
            List<LootEntryContainer<T>> containers = Optional.ofNullable(pool.get("entries"))
                    .map(it -> (List<LootEntryContainer<T>>) new ArrayList<LootEntryContainer<T>>(LootEntryContainers.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Lists.newArrayList());
            List<LootFunction<T>> functions = Optional.ofNullable(pool.get("functions"))
                    .map(it -> (List<LootFunction<T>>) new ArrayList<LootFunction<T>>(LootFunctions.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Lists.newArrayList());
            lootPools.add(new LootPool<>(containers, conditions, functions, rolls, bonus_rolls));
        }
        return new LootTable<>(lootPools,
                Optional.ofNullable(map.get("functions"))
                        .map(it -> (List<LootFunction<T>>) new ArrayList<LootFunction<T>>(LootFunctions.fromMapList((List<Map<String, Object>>) it)))
                        .orElse(Lists.newArrayList())
        );
    }

    public ArrayList<Item<T>> getRandomItems(ContextHolder parameters, World world) {
        return this.getRandomItems(new LootContext(world, parameters, ThreadLocalRandom.current(), 1));
    }

    private ArrayList<Item<T>> getRandomItems(LootContext context) {
        ArrayList<Item<T>> list = new ArrayList<>();
        this.getRandomItems(context, list::add);
        return list;
    }

    public void getRandomItems(LootContext context, Consumer<Item<T>> lootConsumer) {
        this.getRandomItemsRaw(context, LootTable.createStackSplitter(lootConsumer));
    }

    public static <T> Consumer<Item<T>> createStackSplitter(Consumer<Item<T>> consumer) {
        return (item) -> {
            if (item.count() < item.maxStackSize()) {
                consumer.accept(item);
            } else {
                int remaining = item.count();
                while (remaining > 0) {
                    Item<T> splitItem = item.copyWithCount(Math.min(item.maxStackSize(), remaining));
                    remaining -= splitItem.count();
                    consumer.accept(splitItem);
                }
            }
        };
    }

    public void getRandomItemsRaw(LootContext context, Consumer<Item<T>> lootConsumer) {
        Consumer<Item<T>> consumer = LootFunction.decorate(this.compositeFunction, lootConsumer, context);
        for (LootPool<T> pool : this.pools) {
            pool.addRandomItems(consumer, context);
        }
    }
}
