package net.momirealms.craftengine.core.attribute;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;

public interface AttributeOperation {

    Key id();

    double apply(double phaseBase, double current, double amount);

    record OperationContext(double base, double current, double amount) {
    }

    static AttributeOperation of(Key id, ApplyFunction function) {
        return new AttributeOperation() {
            @Override
            public Key id() {
                return id;
            }

            @Override
            public double apply(double phaseBase, double current, double amount) {
                return function.apply(phaseBase, current, amount);
            }

            @Override
            public String toString() {
                return "AttributeOperation{" + id.asString() + "}";
            }
        };
    }

    static AttributeOperation expression(Key id, String rawExpression) {
        final CompiledExpression<OperationContext> compiled;
        try {
            compiled = new ExpressionCompiler<>(name -> switch (name) {
                case "base" -> ParameterBinding.number(OperationContext::base);
                case "current" -> ParameterBinding.number(OperationContext::current);
                case "amount" -> ParameterBinding.number(OperationContext::amount);
                default -> throw new IllegalArgumentException("Unknown expression parameter: " + name);
            }).compile(rawExpression);
        } catch (RuntimeException e) {
            throw new KnownResourceException("attribute.operation.invalid_expression", id.asString(), rawExpression);
        }
        return of(id, (base, current, amount) -> {
            try {
                return compiled.evaluate(new OperationContext(base, current, amount));
            } catch (RuntimeException e) {
                CraftEngine.instance().logger().warn("Failed to evaluate attribute operation '" + id.asString() + "': " + rawExpression + " (" + e.getMessage() + ")");
                return current;
            }
        });
    }

    @FunctionalInterface
    interface ApplyFunction {

        double apply(double phaseBase, double current, double amount);
    }
}
