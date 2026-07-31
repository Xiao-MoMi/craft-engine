package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

public final class IsExplosionCondition<CTX extends Context> implements Condition<CTX> {
    public static final IsExplosionCondition<Context> INSTANCE = new IsExplosionCondition<>();

    private IsExplosionCondition() {}

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.EXPLOSION_RADIUS).isPresent();
    }

    public static <CTX extends Context> ConditionFactory<CTX, IsExplosionCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, IsExplosionCondition<CTX>> {
        @SuppressWarnings("unchecked")
        @Override
        public IsExplosionCondition<CTX> create(ConfigSection section) {
            return (IsExplosionCondition<CTX>) INSTANCE;
        }
    }
}