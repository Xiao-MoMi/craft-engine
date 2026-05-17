package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;

public final class HasPlayerCondition<CTX extends Context> implements Condition<CTX> {
    public static final HasPlayerCondition<Context> INSTANCE = new HasPlayerCondition<>();

    private HasPlayerCondition() {
    }

    @Override
    public boolean test(CTX ctx) {
        if (ctx instanceof PlayerOptionalContext context) {
            return context.isPlayerPresent();
        }
        return false;
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
