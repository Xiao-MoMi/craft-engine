package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class LootFunctions {
    public static final Key APPLY_BONUS = Key.from("craftengine:apply_bonus");
    public static final Key SET_COUNT = Key.from("craftengine:set_count");
    public static final Key EXPLOSION_DECAY = Key.from("craftengine:explosion_decay");
    public static final Key DROP_EXP = Key.from("craftengine:drop_exp");

    static {
        register(SET_COUNT, SetCountFunction.FACTORY);
        register(EXPLOSION_DECAY, ExplosionDecayFunction.FACTORY);
        register(APPLY_BONUS, ApplyBonusCountFunction.FACTORY);
        register(DROP_EXP, DropExpFunction.FACTORY);
    }

    public static <T> void register(Key key, LootFunctionFactory<T> factory) {
        Holder.Reference<LootFunctionFactory<?>> holder = ((WritableRegistry<LootFunctionFactory<?>>) BuiltInRegistries.LOOT_FUNCTION_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.LOOT_FUNCTION_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static <T> BiFunction<Item<T>, LootContext, Item<T>> identity() {
        return (item, context) -> item;
    }

    public static <T> BiFunction<Item<T>, LootContext, Item<T>> compose(List<? extends BiFunction<Item<T>, LootContext, Item<T>>> terms) {
        List<BiFunction<Item<T>, LootContext, Item<T>>> list = List.copyOf(terms);
        return switch (list.size()) {
            case 0 -> identity();
            case 1 -> list.get(0);
            case 2 -> {
                BiFunction<Item<T>, LootContext, Item<T>> f1 = list.get(0);
                BiFunction<Item<T>, LootContext, Item<T>> f2 = list.get(1);
                yield (item, context) -> f2.apply(f1.apply(item, context), context);
            }
            default -> (item, context) -> {
                for (BiFunction<Item<T>, LootContext, Item<T>> function : list) {
                    item = function.apply(item, context);
                }
                return item;
            };
        };
    }

    public static <T> List<LootFunction<T>> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<LootFunction<T>> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    @SuppressWarnings("unchecked")
    public static <T> LootFunction<T> fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("function type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        LootFunctionFactory<T> factory = (LootFunctionFactory<T>) BuiltInRegistries.LOOT_FUNCTION_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown function type: " + type);
        }
        return factory.create(map);
    }
}
