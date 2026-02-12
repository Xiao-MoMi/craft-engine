package net.momirealms.craftengine.core.plugin.context.number;

import com.ezylang.evalex.Expression;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

import java.util.Map;

public record FixedNumberProvider(double value) implements NumberProvider {
    public static final NumberProviderFactory<FixedNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(RandomSource random) {
        return (float) this.value;
    }

    @Override
    public double getDouble(RandomSource random) {
        return this.value;
    }

    public static FixedNumberProvider of(final double value) {
        return new FixedNumberProvider(value);
    }

    private static class Factory implements NumberProviderFactory<FixedNumberProvider> {

        @Override
        public FixedNumberProvider create(Map<String, Object> arguments) {
            String plainOrExpression = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("value"), "warning.config.number.fixed.missing_value");
            try {
                double value = Double.parseDouble(plainOrExpression);
                return new FixedNumberProvider(value);
            } catch (NumberFormatException e) {
                Expression expression = new Expression(plainOrExpression);
                try {
                    return new FixedNumberProvider(expression.evaluate().getNumberValue().doubleValue());
                } catch (Exception e1) {
                    throw new LocalizedResourceConfigException("warning.config.number.fixed.invalid_value", e1, plainOrExpression);
                }
            }
        }
    }
}
