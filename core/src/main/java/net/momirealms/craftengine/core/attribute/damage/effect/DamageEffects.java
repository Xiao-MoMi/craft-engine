package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.attribute.damage.EntityDamageContext;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;

public final class DamageEffects {
    private static final String[] CONDITIONS = ConfigKeys.of("condition(s)");
    private static final String[] FUNCTIONS = ConfigKeys.of("function(s)");

    public static final DamageEffectType<LifeStealDamageEffect> LIFE_STEAL = register(Key.ce("life_steal"), LifeStealDamageEffect.FACTORY);
    public static final DamageEffectType<PotionDamageEffect> POTION_EFFECT = register(Key.ce("potion_effect"), PotionDamageEffect.FACTORY);
    public static final DamageEffectType<FunctionDamageEffect> FUNCTION = register(Key.ce("function"), FunctionDamageEffect.FACTORY);

    private DamageEffects() {
    }

    public static <T extends DamageEffect> DamageEffectType<T> register(Key key, DamageEffectFactory<T> factory) {
        DamageEffectType<T> type = new DamageEffectType<>(key, factory);
        ((WritableRegistry<DamageEffectType<? extends DamageEffect>>) BuiltInRegistries.DAMAGE_EFFECT_TYPE)
                .register(ResourceKey.create(Registries.DAMAGE_EFFECT_TYPE.location(), key), type);
        return type;
    }

    public static DamageEffect fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static DamageEffect fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        DamageEffectType<? extends DamageEffect> effectType = BuiltInRegistries.DAMAGE_EFFECT_TYPE.getValue(key);
        if (effectType == null) {
            throw new KnownResourceException("attribute.damage_effect.unknown_type", section.assemblePath("type"), type);
        }
        DamageEffect effect = effectType.factory().create(section);
        List<Condition<EntityDamageContext>> conditions = section.getSectionList(CONDITIONS, CommonConditions::fromConfig);
        List<Function<Context>> functions = section.getList(FUNCTIONS, CommonFunctions::fromConfig);
        if (conditions.isEmpty() && functions.isEmpty()) {
            return effect;
        }
        return event -> {
            for (Condition<EntityDamageContext> condition : conditions) {
                if (!condition.test(event.context())) {
                    return;
                }
            }
            effect.apply(event);
            for (Function<Context> function : functions) {
                function.run(event.context());
            }
        };
    }
}
