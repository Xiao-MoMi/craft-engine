package net.momirealms.craftengine.core.plugin.context.expression;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.StringTag;
import net.momirealms.craftengine.core.plugin.context.text.StringTags;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.message.internal.parser.Token;
import net.momirealms.sparrow.message.internal.parser.TokenParser;
import net.momirealms.sparrow.message.internal.parser.TokenType;
import net.momirealms.sparrow.message.internal.parser.node.TagPart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class ContextExpression<C> {
    private final CompiledExpression<C> expression;

    private ContextExpression(CompiledExpression<C> expression) {
        this.expression = expression;
    }

    public static ContextExpression<Context> compile(String source) {
        return compile(source, Function.identity(), name -> null);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <C> ContextExpression<C> compile(
            String source,
            Function<C, Context> contextMapper,
            Function<String, ToDoubleFunction<C>> variableBinder
    ) {
        StringBuilder substituted = new StringBuilder(source.length());
        Map<String, Snippet> snippets = new HashMap<>(2);
        for (Token token : TokenParser.tokenize(source, true)) {
            TokenType type = token.type();
            if ((type == TokenType.OPEN_TAG || type == TokenType.OPEN_CLOSE_TAG) && !token.childTokens().isEmpty()) {
                List<Token> children = token.childTokens();
                String name = TokenParser.TagProvider.sanitizePlaceholderName(
                        children.getFirst().get(source).toString());
                StringTag tag = StringTags.get(name);
                if (tag != null) {
                    String[] args = new String[children.size() - 1];
                    for (int i = 1; i < children.size(); i++) {
                        Token child = children.get(i);
                        args[i - 1] = TagPart.unquoteAndEscape(source, child.startIndex(), child.endIndex());
                    }
                    String variable = "__context_tag_" + snippets.size();
                    snippets.put(variable, new Snippet(tag.precompile(args), args));
                    substituted.append(variable);
                    continue;
                }
            }
            substituted.append(source, token.startIndex(), token.endIndex());
        }
        CompiledExpression<C> expression = new ExpressionCompiler<>(name -> {
            Snippet snippet = snippets.get(name);
            if (snippet != null) {
                return ParameterBinding.auto(
                        context -> number(snippet.resolve(contextMapper.apply(context))),
                        context -> string(snippet.resolve(contextMapper.apply(context)))
                );
            }
            ToDoubleFunction<C> variable = variableBinder.apply(name);
            if (variable == null) {
                throw new IllegalArgumentException("Unknown expression parameter: " + name);
            }
            return ParameterBinding.number(variable::applyAsDouble);
        }).compile(substituted.toString());
        return new ContextExpression<>(expression);
    }

    public double evaluate(C context) {
        return this.expression.evaluate(context);
    }

    public boolean test(C context) {
        return this.expression.test(context);
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1D : 0D;
        }
        if (value == null) {
            throw new IllegalArgumentException("Expression parameter is null");
        }
        String string = value.toString();
        return switch (string) {
            case "true", "yes", "TRUE", "YES" -> 1D;
            case "false", "no", "FALSE", "NO" -> 0D;
            default -> Double.parseDouble(string);
        };
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private record Snippet(StringTag tag, String[] args) {

        private Object resolve(Context context) {
            return this.tag.resolve(this.args, context);
        }
    }
}
