package net.momirealms.craftengine.core.plugin.context.expression;

import net.momirealms.sparrow.expr.ExpressionCompiler;

public final class Expressions {
    private static final ExpressionCompiler<Void> CONSTANT_COMPILER = new ExpressionCompiler<>(name -> {
        throw new IllegalArgumentException("Unknown expression parameter: " + name);
    });

    private Expressions() {
    }

    public static double evaluate(String expression) {
        return CONSTANT_COMPILER.compile(expression).evaluate(null);
    }
}
