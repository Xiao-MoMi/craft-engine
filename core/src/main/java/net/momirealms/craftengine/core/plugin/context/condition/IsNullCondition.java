package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextKey;

import java.util.Optional;

public final class IsNullCondition<CTX extends Context> implements Condition<CTX> {
    private final ContextKey<?> key;

    private IsNullCondition(ContextKey<?> key) {
        this.key = key;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<?> optional = ctx.getOptionalParameter(this.key);
        return optional.isEmpty();
    }

    public static <CTX extends Context> ConditionFactory<CTX, IsNullCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, IsNullCondition<CTX>> {
        private static final String[] ARGUMENT = new String[] {"argument", "arg"};

        @Override
        public IsNullCondition<CTX> create(ConfigSection section) {
            return new IsNullCondition<>(ContextKey.chain(section.getNonNullString(ARGUMENT)));
        }
    }
}