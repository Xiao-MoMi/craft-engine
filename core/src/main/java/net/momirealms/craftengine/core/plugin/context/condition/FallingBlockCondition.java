package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

public final class FallingBlockCondition<CTX extends Context> implements Condition<CTX> {
    private FallingBlockCondition() {}

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.FALLING_BLOCK).orElse(false);
    }

    public static <CTX extends Context> ConditionFactory<CTX, FallingBlockCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, FallingBlockCondition<CTX>> {

        @Override
        public FallingBlockCondition<CTX> create(ConfigSection section) {
            return new FallingBlockCondition<>();
        }
    }
}