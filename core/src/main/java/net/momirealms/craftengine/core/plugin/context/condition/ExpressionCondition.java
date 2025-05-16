package net.momirealms.craftengine.core.plugin.context.condition;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class ExpressionCondition<CTX extends Context> implements Condition<CTX> {
    private final TextProvider expression;

    public ExpressionCondition(TextProvider expression) {
        this.expression = expression;
    }

    @Override
    public Key type() {
        return CommonConditions.EXPRESSION;
    }

    @Override
    public boolean test(CTX ctx) {
        String exp = expression.get(ctx);
        Expression expr = new Expression(exp);
        try {
            return expr.evaluate().getBooleanValue();
        } catch (ParseException | EvaluationException e) {
            CraftEngine.instance().logger().warn("Invalid expression " + exp, e);
            return false;
        }
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            String value = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("expression"), "warning.config.condition.expression.missing_expression");
            return new ExpressionCondition<>(TextProviders.fromString(value));
        }
    }
}
