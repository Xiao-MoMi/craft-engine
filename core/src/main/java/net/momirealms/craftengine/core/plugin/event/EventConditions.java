package net.momirealms.craftengine.core.plugin.event;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.condition.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class EventConditions {

    static {
        register(CommonConditions.MATCH_ITEM, new MatchItemCondition.FactoryImpl<>());
        register(CommonConditions.MATCH_BLOCK_PROPERTY, new MatchBlockPropertyCondition.FactoryImpl<>());
        register(CommonConditions.TABLE_BONUS, new TableBonusCondition.FactoryImpl<>());
        register(CommonConditions.SURVIVES_EXPLOSION, new SurvivesExplosionCondition.FactoryImpl<>());
        register(CommonConditions.ANY_OF, new AnyOfCondition.FactoryImpl<>(EventConditions::fromMap));
        register(CommonConditions.ALL_OF, new AllOfCondition.FactoryImpl<>(EventConditions::fromMap));
        register(CommonConditions.ENCHANTMENT, new EnchantmentCondition.FactoryImpl<>());
        register(CommonConditions.INVERTED, new InvertedCondition.FactoryImpl<>(EventConditions::fromMap));
        register(CommonConditions.FALLING_BLOCK, new FallingBlockCondition.FactoryImpl<>());
        register(CommonConditions.RANDOM, new RandomCondition.FactoryImpl<>());
        register(CommonConditions.DISTANCE, new DistanceCondition.FactoryImpl<>());
        register(CommonConditions.PERMISSION, new PermissionCondition.FactoryImpl<>());
        register(CommonConditions.EQUALS, new EqualsCondition.FactoryImpl<>());
        register(CommonConditions.EXPRESSION, new ExpressionCondition.FactoryImpl<>());
    }

    public static void register(Key key, ConditionFactory<PlayerOptionalContext> factory) {
        Holder.Reference<ConditionFactory<PlayerOptionalContext>> holder = ((WritableRegistry<ConditionFactory<PlayerOptionalContext>>) BuiltInRegistries.EVENT_CONDITION_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.EVENT_CONDITION_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static Condition<PlayerOptionalContext> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.event.condition.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        if (key.value().charAt(0) == '!') {
            ConditionFactory<PlayerOptionalContext> factory = BuiltInRegistries.EVENT_CONDITION_FACTORY.getValue(new Key(key.namespace(), key.value().substring(1)));
            if (factory == null) {
                throw new LocalizedResourceConfigException("warning.config.event.condition.invalid_type", type);
            }
            return new InvertedCondition<>(factory.create(map));
        } else {
            ConditionFactory<PlayerOptionalContext> factory = BuiltInRegistries.EVENT_CONDITION_FACTORY.getValue(key);
            if (factory == null) {
                throw new LocalizedResourceConfigException("warning.config.event.condition.invalid_type", type);
            }
            return factory.create(map);
        }
    }
}
