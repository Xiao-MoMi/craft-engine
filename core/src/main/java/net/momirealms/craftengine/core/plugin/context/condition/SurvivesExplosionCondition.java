package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Optional;

public final class SurvivesExplosionCondition<CTX extends Context> implements Condition<CTX> {
    public static final SurvivesExplosionCondition<Context> INSTANCE = new SurvivesExplosionCondition<>();

    private SurvivesExplosionCondition() {}

    @Override
    public boolean test(CTX ctx) {
        Optional<Float> radius = ctx.getOptionalParameter(DirectContextParameters.EXPLOSION_RADIUS);
        if (radius.isPresent()) {
            float f = 1f / radius.get();
            return RandomUtils.generateRandomFloat(0, 1) < f;
        }
        return true;
    }

    public static <CTX extends Context> ConditionFactory<CTX, SurvivesExplosionCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, SurvivesExplosionCondition<CTX>> {

        @SuppressWarnings("unchecked")
        @Override
        public SurvivesExplosionCondition<CTX> create(ConfigSection arguments) {
            return (SurvivesExplosionCondition<CTX>) INSTANCE;
        }
    }
}