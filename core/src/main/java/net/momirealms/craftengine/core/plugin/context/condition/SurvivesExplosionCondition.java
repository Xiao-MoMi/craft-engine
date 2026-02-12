package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;
import java.util.Optional;

public final class SurvivesExplosionCondition<CTX extends Context> implements Condition<CTX> {

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

        @Override
        public SurvivesExplosionCondition<CTX> create(Map<String, Object> arguments) {
            return new SurvivesExplosionCondition<>();
        }
    }
}