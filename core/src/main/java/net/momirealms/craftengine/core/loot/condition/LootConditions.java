package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class LootConditions {
    public static final Key MATCH_ITEM = Key.from("craftengine:match_item");
    public static final Key TABLE_BONUS = Key.from("craftengine:table_bonus");
    public static final Key SURVIVES_EXPLOSION = Key.from("craftengine:survives_explosion");
    public static final Key ANY_OF = Key.from("craftengine:any_of");
    public static final Key ALL_OF = Key.from("craftengine:all_of");
    public static final Key ENCHANTMENT = Key.from("craftengine:enchantment");
    public static final Key INVERTED = Key.from("craftengine:inverted");
    public static final Key FALLING_BLOCK = Key.from("craftengine:falling_block");

    static {
        register(MATCH_ITEM, MatchItemCondition.FACTORY);
        register(TABLE_BONUS, TableBonusCondition.FACTORY);
        register(SURVIVES_EXPLOSION, SurvivesExplosionCondition.FACTORY);
        register(ANY_OF, AnyOfCondition.FACTORY);
        register(ALL_OF, AllOfCondition.FACTORY);
        register(ENCHANTMENT, EnchantmentCondition.FACTORY);
        register(INVERTED, InvertedCondition.FACTORY);
        register(FALLING_BLOCK, FallingCondition.FACTORY);
    }

    public static void register(Key key, LootConditionFactory factory) {
        Holder.Reference<LootConditionFactory> holder = ((WritableRegistry<LootConditionFactory>) BuiltInRegistries.LOOT_CONDITION_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.LOOT_CONDITION_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static <T> Predicate<T> andConditions(List<? extends Predicate<T>> predicates) {
        List<Predicate<T>> list = List.copyOf(predicates);
        return switch (list.size()) {
            case 0 -> ctx -> true;
            case 1 -> list.get(0);
            case 2 -> list.get(0).and(list.get(1));
            default -> (ctx -> {
                for (Predicate<T> predicate : list) {
                    if (!predicate.test(ctx)) {
                        return false;
                    }
                }
                return true;
            });
        };
    }

    public static <T> Predicate<T> orConditions(List<? extends Predicate<T>> predicates) {
        List<Predicate<T>> list = List.copyOf(predicates);
        return switch (list.size()) {
            case 0 -> ctx -> false;
            case 1 -> list.get(0);
            case 2 -> list.get(0).or(list.get(1));
            default -> (ctx -> {
                for (Predicate<T> predicate : list) {
                    if (predicate.test(ctx)) {
                        return true;
                    }
                }
                return false;
            });
        };
    }

    public static List<LootCondition> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<LootCondition> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    public static LootCondition fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("condition type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        LootConditionFactory factory = BuiltInRegistries.LOOT_CONDITION_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown loot condition type: " + type);
        }
        return factory.create(map);
    }

}
