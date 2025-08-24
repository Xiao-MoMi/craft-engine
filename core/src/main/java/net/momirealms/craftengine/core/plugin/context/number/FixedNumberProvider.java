package net.momirealms.craftengine.core.plugin.context.number;

import com.ezylang.evalex.Expression;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class FixedNumberProvider implements NumberProvider {
    public static final FactoryImpl FACTORY = new FactoryImpl();
    private final double value;

    public FixedNumberProvider(double value) {
        this.value = value;
    }

    @Override
    public float getFloat(Context context) {
        return (float) this.value;
    }

    @Override
    public double getDouble(Context context) {
        return this.value;
    }

    @Override
    public Key type() {
        return NumberProviders.FIXED;
    }

    public static FixedNumberProvider of(final double value) {
        return new FixedNumberProvider(value);
    }

    public static class FactoryImpl implements NumberProviderFactory {

        @Override
        public NumberProvider create(Map<String, Object> arguments) {
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
