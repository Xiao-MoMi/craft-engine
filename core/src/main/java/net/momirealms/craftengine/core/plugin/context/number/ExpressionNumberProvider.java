package net.momirealms.craftengine.core.plugin.context.number;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class ExpressionNumberProvider implements NumberProvider {
    public static final FactoryImpl FACTORY = new FactoryImpl();
    private final String expr;

    public ExpressionNumberProvider(String expr) {
        this.expr = expr;
    }

    @Override
    public float getFloat(Context context) {
        Component resultComponent = AdventureHelper.customMiniMessage().deserialize(this.expr, context.tagResolvers());
        String resultString = AdventureHelper.plainTextContent(resultComponent);
        Expression expression = new Expression(resultString);
        try {
            return expression.evaluate().getNumberValue().floatValue();
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException("Invalid expression: " + this.expr + " -> " + resultString + " -> Cannot parse", e);
        }
    }

    @Override
    public Key type() {
        return NumberProviders.EXPRESSION;
    }

    public String expression() {
        return this.expr;
    }

    public static class FactoryImpl implements NumberProviderFactory {

        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            String value = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("expression"), "warning.config.number.expression.missing_expression");
            return new ExpressionNumberProvider(value);
        }
    }
}
