package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.expression.ContextExpression;

public final class ExpressionNumberProvider implements NumberProvider {
    public static final NumberProviderFactory<ExpressionNumberProvider> FACTORY = new Factory();

    private final String expression;
    private final ContextExpression<Context> compiled;

    public ExpressionNumberProvider(String expression) {
        this.expression = expression;
        this.compiled = ContextExpression.compile(expression);
    }

    public static ExpressionNumberProvider expression(String expression) {
        return new ExpressionNumberProvider(expression);
    }

    public String expression() {
        return this.expression;
    }

    @Override
    public float getFloat(Context context) {
        return (float) this.compiled.evaluate(context);
    }

    @Override
    public double getDouble(Context context) {
        return this.compiled.evaluate(context);
    }

    private static class Factory implements NumberProviderFactory<ExpressionNumberProvider> {

        @Override
        public ExpressionNumberProvider create(ConfigSection section) {
            return new ExpressionNumberProvider(section.getNonNullString("expression"));
        }
    }
}
