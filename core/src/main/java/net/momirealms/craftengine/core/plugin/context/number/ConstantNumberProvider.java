package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.expression.Expressions;

public record ConstantNumberProvider(double value) implements NumberProvider {
    public static final NumberProviderFactory<ConstantNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(Context context) {
        return (float) this.value;
    }

    @Override
    public double getDouble(Context context) {
        return this.value;
    }

    @Override
    public boolean isConstant() {
        return true;
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
                try {
                    return new ConstantNumberProvider(Expressions.evaluate(plainOrExpression));
                } catch (RuntimeException ex) {
                    throw new KnownResourceException("number.fixed.invalid_expression", section.assemblePath("value"), plainOrExpression);
                }
            }
        }
    }
}
