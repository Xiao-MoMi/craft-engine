package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Optional;

public class SurvivesExplosionCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private static final SurvivesExplosionCondition INSTANCE = new SurvivesExplosionCondition();

    @Override
    public Key type() {
        return LootConditions.SURVIVES_EXPLOSION;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Optional<Float> radius = lootContext.getOptionalParameter(LootParameters.EXPLOSION_RADIUS);
        if (radius.isPresent()) {
            float f = 1f / radius.get();
            return lootContext.randomSource().nextFloat() < f;
        }
        return true;
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
