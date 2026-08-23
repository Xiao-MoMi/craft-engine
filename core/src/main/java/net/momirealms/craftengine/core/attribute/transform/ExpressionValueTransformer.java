package net.momirealms.craftengine.core.attribute.transform;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;

public final class ExpressionValueTransformer implements ValueTransformer {
    private static final ExpressionCompiler<ValueContext> COMPILER = new ExpressionCompiler<>(name -> {
        if (!name.equals("value")) {
            throw new IllegalArgumentException("Unknown expression parameter: " + name);
        }
        return ParameterBinding.number(ValueContext::value);
    });
    public static final ValueTransformerFactory<ExpressionValueTransformer> FACTORY =
            args -> new ExpressionValueTransformer(args.getString("expression", "value"));
    private final CompiledExpression<ValueContext> expression;

    public ExpressionValueTransformer(String expression) {
        this.expression = COMPILER.compile(expression);
    }

    @Override
    public double transform(double value) {
        try {
            return this.expression.evaluate(new ValueContext(value));
        } catch (RuntimeException e) {
            CraftEngine.instance().logger().warn("Failed to evaluate value transformer expression: " + e.getMessage());
            return value;
        }
    }

    private record ValueContext(double value) {
    }
}
