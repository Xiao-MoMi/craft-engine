package net.momirealms.craftengine.core.plugin.context.number;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.random.RandomSource;

public record ConstantNumberProvider(double value) implements NumberProvider {
    public static final NumberProviderFactory<ConstantNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(RandomSource random) {
        return (float) this.value;
    }

    @Override
    public double getDouble(RandomSource random) {
        return this.value;
    }

    public static ConstantNumberProvider constant(final double value) {
        return new ConstantNumberProvider(value);
    }

    private static class Factory implements NumberProviderFactory<ConstantNumberProvider> {

        @Override
        public ConstantNumberProvider create(ConfigSection section) {
            String plainOrExpression = section.getNonNullString("value");
            try {
                double value = Double.parseDouble(plainOrExpression);
                return new ConstantNumberProvider(value);
            } catch (NumberFormatException e) {
                Expression expression = new Expression(plainOrExpression);
                try {
                    return new ConstantNumberProvider(expression.evaluate().getNumberValue().doubleValue());
                } catch (ParseException | EvaluationException ex) {
                    throw new KnownResourceException("number.fixed.invalid_expression", section.assemblePath("value"), plainOrExpression);
                }
            }
        }
    }
}
