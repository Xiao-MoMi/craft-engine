package net.momirealms.craftengine.core.plugin.context.expression;

import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.ExpressionParseException;

import java.util.function.Supplier;

public final class Expressions {
    private static final String INVALID_EXPRESSION = "resource.expression.invalid";
    private static final String INVALID_EXPRESSION_SYNTAX = "resource.expression.invalid_syntax";
    private static final String INVALID_FUNCTION_ARGUMENT = "resource.expression.invalid_function_argument";
    private static final String UNKNOWN_PARAMETER = "resource.expression.unknown_parameter";
    private static final ExpressionCompiler<Void> CONSTANT_COMPILER = new ExpressionCompiler<>(name -> {
        throw unknownParameter(name);
    });

    private Expressions() {
    }

    public static double evaluate(String expression) {
        return CONSTANT_COMPILER.compile(expression).evaluate(null);
    }

    public static double evaluate(String node, String expression) {
        return precompile(node, expression, () -> CONSTANT_COMPILER.compile(expression)).evaluate(null);
    }

    public static <T> T precompile(String node, String expression, Supplier<T> compiler) {
        try {
            return compiler.get();
        } catch (KnownResourceException e) {
            throw e;
        } catch (ExpressionParseException e) {
            throw invalidSyntax(
                    node,
                    expression,
                    e.startPosition(),
                    e.endPosition(),
                    e.token(),
                    e.argumentIndex(),
                    e
            );
        } catch (UnknownParameterException e) {
            throw new KnownResourceException(
                    UNKNOWN_PARAMETER,
                    node,
                    e,
                    e.parameter,
                    expression
            );
        } catch (RuntimeException e) {
            String detail = e.getMessage();
            if (detail == null || detail.isEmpty()) {
                detail = e.getClass().getSimpleName();
            }
            throw new KnownResourceException(INVALID_EXPRESSION, node, e, expression, detail);
        }
    }

    public static IllegalArgumentException unknownParameter(String parameter) {
        return new UnknownParameterException(parameter);
    }

    static KnownResourceException invalidSyntax(
            String node,
            String expression,
            int startPosition,
            int endPosition,
            String token,
            int argumentIndex,
            ExpressionParseException cause
    ) {
        String position = position(startPosition, endPosition);
        String displayToken = token.isEmpty() ? "EOF" : token;
        if (argumentIndex >= 0) {
            return new KnownResourceException(
                    INVALID_FUNCTION_ARGUMENT,
                    node,
                    cause,
                    expression,
                    position,
                    displayToken,
                    Integer.toString(argumentIndex + 1)
            );
        }
        return new KnownResourceException(
                INVALID_EXPRESSION_SYNTAX,
                node,
                cause,
                expression,
                position,
                displayToken
        );
    }

    private static String position(int startPosition, int endPosition) {
        int start = startPosition + 1;
        int end = Math.max(start, endPosition);
        return start == end ? Integer.toString(start) : start + "-" + end;
    }

    private static final class UnknownParameterException extends IllegalArgumentException {
        private final String parameter;

        private UnknownParameterException(String parameter) {
            super("Unknown expression parameter: " + parameter);
            this.parameter = parameter;
        }
    }
}
