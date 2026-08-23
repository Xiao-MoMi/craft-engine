package net.momirealms.craftengine.core.attribute.sync;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;

public final class ExpressionSyncValueProvider implements SyncValueProvider {
    private static final ExpressionCompiler<SyncContext> COMPILER = new ExpressionCompiler<>(name -> switch (name) {
        case "value" -> ParameterBinding.number(SyncContext::value);
        case "base" -> ParameterBinding.number(SyncContext::base);
        default -> throw new IllegalArgumentException("Unknown expression parameter: " + name);
    });
    public static final SyncValueProviderFactory<ExpressionSyncValueProvider> FACTORY =
            args -> new ExpressionSyncValueProvider(args.getNonNullString("expression"));
    public static final ExpressionSyncValueProvider DEFAULT = new ExpressionSyncValueProvider("value");
    private final CompiledExpression<SyncContext> expression;

    public ExpressionSyncValueProvider(String expression) {
        this.expression = COMPILER.compile(expression);
    }

    @Override
    public double resolve(double value, double base) {
        try {
            return this.expression.evaluate(new SyncContext(value, base));
        } catch (RuntimeException e) {
            CraftEngine.instance().logger().warn("Failed to evaluate sync value expression: " + e.getMessage());
            return 0;
        }
    }

    private record SyncContext(double value, double base) {
    }
}
