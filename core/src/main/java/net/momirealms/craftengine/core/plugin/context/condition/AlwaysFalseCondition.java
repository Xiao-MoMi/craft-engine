package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;

public final class AlwaysFalseCondition<CTX extends Context> implements Condition<CTX> {
    public static final AlwaysFalseCondition<Context> INSTANCE = new AlwaysFalseCondition<>();

    private AlwaysFalseCondition() {}

    public static <CTX extends Context> ConditionFactory<CTX, AlwaysFalseCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, AlwaysFalseCondition<CTX>> {

        @SuppressWarnings("unchecked")
        @Override
        public AlwaysFalseCondition<CTX> create(ConfigSection arguments) {
            return (AlwaysFalseCondition<CTX>) INSTANCE;
        }
    }
}
