package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.condition.*;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class CommonConditions {
    public static final CommonConditionType<HasPlayerCondition<Context>> HAS_PLAYER = register(Key.ce("has_player"), HasPlayerCondition.factory());
    public static final CommonConditionType<HasItemCondition<Context>> HAS_ITEM = register(Key.ce("has_item"), HasItemCondition.factory());
    public static final CommonConditionType<MatchItemCondition<Context>> MATCH_ITEM = register(Key.ce("match_item"), MatchItemCondition.factory());
    public static final CommonConditionType<MatchEntityCondition<Context>> MATCH_ENTITY = register(Key.ce("match_entity"), MatchEntityCondition.factory());
    public static final CommonConditionType<MatchBlockCondition<Context>> MATCH_BLOCK = register(Key.ce("match_block"), MatchBlockCondition.factory());
    public static final CommonConditionType<MatchBlockPropertyCondition<Context>> MATCH_BLOCK_PROPERTY = register(Key.ce("match_block_property"), MatchBlockPropertyCondition.factory());
    public static final CommonConditionType<TableBonusCondition<Context>> TABLE_BONUS = register(Key.ce("table_bonus"), TableBonusCondition.factory());
    public static final CommonConditionType<SurvivesExplosionCondition<Context>> SURVIVES_EXPLOSION = register(Key.ce("survives_explosion"), SurvivesExplosionCondition.factory());
    public static final CommonConditionType<AnyOfCondition<Context>> ANY_OF = register(Key.ce("any_of"), AnyOfCondition.factory(CommonConditions::fromConfig));
    public static final CommonConditionType<AllOfCondition<Context>> ALL_OF = register(Key.ce("all_of"), AllOfCondition.factory(CommonConditions::fromConfig));
    public static final CommonConditionType<EnchantmentCondition<Context>> ENCHANTMENT = register(Key.ce("enchantment"), EnchantmentCondition.factory());
    public static final CommonConditionType<InvertedCondition<Context>> INVERTED = register(Key.ce("inverted"), InvertedCondition.factory(CommonConditions::fromConfig));
    public static final CommonConditionType<FallingBlockCondition<Context>> FALLING_BLOCK = register(Key.ce("falling_block"), FallingBlockCondition.factory());
    public static final CommonConditionType<RandomCondition<Context>> RANDOM = register(Key.ce("random"), RandomCondition.factory());
    public static final CommonConditionType<DistanceCondition<Context>> DISTANCE = register(Key.ce("distance"), DistanceCondition.factory());
    public static final CommonConditionType<PermissionCondition<Context>> PERMISSION = register(Key.ce("permission"), PermissionCondition.factory());
    public static final CommonConditionType<StringEqualsCondition<Context>> EQUALS = register(Key.ce("equals"), StringEqualsCondition.factory());
    public static final CommonConditionType<StringRegexCondition<Context>> REGEX = register(Key.ce("regex"), StringRegexCondition.factory());
    public static final CommonConditionType<StringEqualsCondition<Context>> STRING_EQUALS = register(Key.ce("string_equals"), StringEqualsCondition.factory());
    public static final CommonConditionType<StringContainsCondition<Context>> STRING_CONTAINS = register(Key.ce("string_contains"), StringContainsCondition.factory());
    public static final CommonConditionType<ExpressionCondition<Context>> EXPRESSION = register(Key.ce("expression"), ExpressionCondition.factory());
    public static final CommonConditionType<IsNullCondition<Context>> IS_NULL = register(Key.ce("is_null"), IsNullCondition.factory());
    public static final CommonConditionType<HandCondition<Context>> HAND = register(Key.ce("hand"), HandCondition.factory());
    public static final CommonConditionType<OnCooldownCondition<Context>> ON_COOLDOWN = register(Key.ce("on_cooldown"), OnCooldownCondition.factory());
    public static final CommonConditionType<InventoryHasItemCondition<Context>> INVENTORY_HAS_ITEM = register(Key.ce("inventory_has_item"), InventoryHasItemCondition.factory());
    public static final CommonConditionType<MatchFurnitureVariantCondition<Context>> MATCH_FURNITURE_VARIANT = register(Key.ce("match_furniture_variant"), MatchFurnitureVariantCondition.factory());
    public static final CommonConditionType<IsBedrockPlayerCondition<Context>> IS_BEDROCK_PLAYER = register(Key.ce("is_bedrock_player"), IsBedrockPlayerCondition.factory());

    private CommonConditions() {}

    public static <T extends Condition<Context>> CommonConditionType<T> register(Key key, ConditionFactory<Context, T> factory) {
        CommonConditionType<T> type = new CommonConditionType<>(key, factory);
        ((WritableRegistry<CommonConditionType<?>>) BuiltInRegistries.COMMON_CONDITION_TYPE)
                .register(ResourceKey.create(Registries.COMMON_CONDITION_TYPE.location(), key), type);
        return type;
    }

    public static <CTX extends Context> Condition<CTX> fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    @SuppressWarnings("unchecked")
    public static <CTX extends Context> Condition<CTX> fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        boolean inverted = type.charAt(0) == '!';
        if (inverted) {
            type = type.substring(1);
        }
        Key key = Key.ce(type);
        CommonConditionType<? extends Condition<Context>> conditionType = BuiltInRegistries.COMMON_CONDITION_TYPE.getValue(key);
        if (conditionType == null) {
            throw new KnownResourceException("condition.unknown_type", section.assemblePath("type"), type);
        }
        return inverted ? InvertedCondition.inverted((Condition<CTX>) conditionType.factory().create(section)) : (Condition<CTX>) conditionType.factory().create(section);
    }
}
