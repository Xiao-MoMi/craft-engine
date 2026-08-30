package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.expression.ContextExpression;

public final class ExpressionCondition<CTX extends Context> implements Condition<CTX> {
    private final String source;
    private final ContextExpression<Context> expression;

    private ExpressionCondition(String source, ContextExpression<Context> expression) {
        this.source = source;
        this.expression = expression;
    }

    public static <CTX extends Context> ConditionFactory<CTX, ExpressionCondition<CTX>> factory() {
        return new Factory<>();
    }

    @Override
    public boolean test(CTX ctx) {
        try {
            return this.expression.test(ctx);
        } catch (Throwable t) {
            CraftEngine.instance().logger().warn("Error evaluating expression: " + this.source, t);
            return false;
        }
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, ExpressionCondition<CTX>> {
        private static final String[] EXPR = ConfigKeys.of("expression|expr");

        @Override
        public ExpressionCondition<CTX> create(ConfigSection section) {
            ConfigValue expression = section.getNonNullValue(EXPR, ConfigConstants.ARGUMENT_STRING);
            return new ExpressionCondition<>(expression.getAsString(), ContextExpression.precompile(expression.path(), expression.getAsString()));
        }
    }
}
