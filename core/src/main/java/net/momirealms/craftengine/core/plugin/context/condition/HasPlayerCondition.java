package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

public final class HasPlayerCondition<CTX extends Context> implements Condition<CTX> {
    public static final HasPlayerCondition<Context> INSTANCE = new HasPlayerCondition<>();

    private HasPlayerCondition() {
    }

    @Override
    public boolean test(CTX ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.PLAYER).isPresent();
    }

    public static <CTX extends Context> ConditionFactory<CTX, HasPlayerCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, HasPlayerCondition<CTX>> {

        @SuppressWarnings("unchecked")
        @Override
        public HasPlayerCondition<CTX> create(ConfigSection arguments) {
            return (HasPlayerCondition<CTX>) INSTANCE;
        }
    }
}
