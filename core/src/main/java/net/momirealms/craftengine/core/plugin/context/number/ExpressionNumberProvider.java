package net.momirealms.craftengine.core.plugin.context.number;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

import java.util.Map;

public record ExpressionNumberProvider(String expression) implements NumberProvider {
    public static final NumberProviderFactory<ExpressionNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(Context context) {
        Component resultComponent = AdventureHelper.customMiniMessage().deserialize(this.expression, context.tagResolvers());
        String resultString = AdventureHelper.plainTextContent(resultComponent);
        Expression expression = new Expression(resultString);
        try {
            return expression.evaluate().getNumberValue().floatValue();
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException("Invalid expression: " + this.expression + " -> " + resultString + " -> Cannot parse", e);
        }
    }

    @Override
    public float getFloat(RandomSource random) {
        Expression expression = new Expression(this.expression);
        try {
            return expression.evaluate().getNumberValue().floatValue();
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException("Invalid expression: " + this.expression, e);
        }
    }

    @Override
    public double getDouble(Context context) {
        Component resultComponent = AdventureHelper.customMiniMessage().deserialize(this.expression, context.tagResolvers());
        String resultString = AdventureHelper.plainTextContent(resultComponent);
        Expression expression = new Expression(resultString);
        try {
            return expression.evaluate().getNumberValue().doubleValue();
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException("Invalid expression: " + this.expression + " -> " + resultString + " -> Cannot parse", e);
        }
    }

    @Override
    public double getDouble(RandomSource random) {
        Expression expression = new Expression(this.expression);
        try {
            return expression.evaluate().getNumberValue().doubleValue();
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException("Invalid expression: " + this.expression, e);
        }
    }

    private static class Factory implements NumberProviderFactory<ExpressionNumberProvider> {

        @Override
        public ExpressionNumberProvider create(Map<String, Object> arguments) {
            String value = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("expression"), "warning.config.number.expression.missing_expression");
            return new ExpressionNumberProvider(value);
        }
    }
}
