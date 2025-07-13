package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class LootConditions {

    static {
        register(CommonConditions.MATCH_ITEM, new MatchItemCondition.FactoryImpl<>());
        register(CommonConditions.MATCH_BLOCK_PROPERTY, new MatchBlockPropertyCondition.FactoryImpl<>());
        register(CommonConditions.TABLE_BONUS, new TableBonusCondition.FactoryImpl<>());
        register(CommonConditions.SURVIVES_EXPLOSION, new SurvivesExplosionCondition.FactoryImpl<>());
        register(CommonConditions.ANY_OF, new AnyOfCondition.FactoryImpl<>(LootConditions::fromMap));
        register(CommonConditions.ALL_OF, new AllOfCondition.FactoryImpl<>(LootConditions::fromMap));
        register(CommonConditions.ENCHANTMENT, new EnchantmentCondition.FactoryImpl<>());
        register(CommonConditions.INVERTED, new InvertedCondition.FactoryImpl<>(LootConditions::fromMap));
        register(CommonConditions.FALLING_BLOCK, new FallingBlockCondition.FactoryImpl<>());
        register(CommonConditions.RANDOM, new RandomCondition.FactoryImpl<>());
        register(CommonConditions.DISTANCE, new DistanceCondition.FactoryImpl<>());
        register(CommonConditions.PERMISSION, new PermissionCondition.FactoryImpl<>());
        register(CommonConditions.EQUALS, new StringEqualsCondition.FactoryImpl<>());
        register(CommonConditions.STRING_REGEX, new StringRegexCondition.FactoryImpl<>());
        register(CommonConditions.STRING_EQUALS, new StringEqualsCondition.FactoryImpl<>());
        register(CommonConditions.STRING_CONTAINS, new StringContainsCondition.FactoryImpl<>());
        register(CommonConditions.EXPRESSION, new ExpressionCondition.FactoryImpl<>());
        register(CommonConditions.IS_NULL, new IsNullCondition.FactoryImpl<>());
        register(CommonConditions.HAND, new HandCondition.FactoryImpl<>());
    }

    public static void register(Key key, ConditionFactory<LootContext> factory) {
        ((WritableRegistry<ConditionFactory<LootContext>>) BuiltInRegistries.LOOT_CONDITION_FACTORY)
                .register(ResourceKey.create(Registries.LOOT_CONDITION_FACTORY.location(), key), factory);
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

    public static List<Condition<LootContext>> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<Condition<LootContext>> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    public static Condition<LootContext> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.loot_table.condition.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        if (key.value().charAt(0) == '!') {
            ConditionFactory<LootContext> factory = BuiltInRegistries.LOOT_CONDITION_FACTORY.getValue(new Key(key.namespace(), key.value().substring(1)));
            if (factory == null) {
                throw new LocalizedResourceConfigException("warning.config.loot_table.condition.invalid_type", type);
            }
            return new InvertedCondition<>(factory.create(map));
        } else {
            ConditionFactory<LootContext> factory = BuiltInRegistries.LOOT_CONDITION_FACTORY.getValue(key);
            if (factory == null) {
                throw new LocalizedResourceConfigException("warning.config.loot_table.condition.invalid_type", type);
            }
            return factory.create(map);
        }
    }
}
