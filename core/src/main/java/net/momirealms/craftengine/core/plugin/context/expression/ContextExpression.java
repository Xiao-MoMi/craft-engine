package net.momirealms.craftengine.core.plugin.context.expression;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.StringTag;
import net.momirealms.craftengine.core.plugin.context.text.StringTags;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.ExpressionParseException;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.message.internal.parser.Token;
import net.momirealms.sparrow.message.internal.parser.TokenParser;
import net.momirealms.sparrow.message.internal.parser.TokenType;
import net.momirealms.sparrow.message.internal.parser.node.TagPart;

import java.util.ArrayList;
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

    public static ContextExpression<Context> precompile(String node, String source) {
        return precompile(node, source, Function.identity(), name -> null);
    }

    public static <C> ContextExpression<C> compile(
            String source,
            Function<C, Context> contextMapper,
            Function<String, ToDoubleFunction<C>> variableBinder
    ) {
        return compile(null, source, contextMapper, variableBinder);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static <C> ContextExpression<C> compile(
            String node,
            String source,
            Function<C, Context> contextMapper,
            Function<String, ToDoubleFunction<C>> variableBinder
    ) {
        StringBuilder substituted = new StringBuilder(source.length());
        Map<String, Snippet> snippets = new HashMap<>(2);
        List<Replacement> replacements = new ArrayList<>(2);
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
                    int generatedStart = substituted.length();
                    substituted.append(variable);
                    replacements.add(new Replacement(
                            generatedStart,
                            substituted.length(),
                            token.startIndex(),
                            token.endIndex()
                    ));
                    continue;
                }
            }
            substituted.append(source, token.startIndex(), token.endIndex());
        }
        final CompiledExpression<C> expression;
        try {
            expression = new ExpressionCompiler<C>(name -> {
                Snippet snippet = snippets.get(name);
                if (snippet != null) {
                    return ParameterBinding.auto(
                            context -> number(snippet.resolve(contextMapper.apply(context))),
                            context -> string(snippet.resolve(contextMapper.apply(context)))
                    );
                }
                ToDoubleFunction<C> variable = variableBinder.apply(name);
                if (variable == null) {
                    throw Expressions.unknownParameter(name);
                }
                return ParameterBinding.number(variable::applyAsDouble);
            }).compile(substituted.toString());
        } catch (ExpressionParseException e) {
            if (node == null) {
                throw e;
            }
            throw Expressions.invalidSyntax(
                    node,
                    source,
                    remapPosition(e.startPosition(), replacements),
                    remapPosition(e.endPosition(), replacements),
                    remapToken(e, source, replacements),
                    e.argumentIndex(),
                    e
            );
        }
        return new ContextExpression<>(expression);
    }

    public static <C> ContextExpression<C> precompile(
            String node,
            String source,
            Function<C, Context> contextMapper,
            Function<String, ToDoubleFunction<C>> variableBinder
    ) {
        return Expressions.precompile(
                node,
                source,
                () -> compile(node, source, contextMapper, variableBinder)
        );
    }

    private static int remapPosition(int position, List<Replacement> replacements) {
        int delta = 0;
        for (Replacement replacement : replacements) {
            if (position < replacement.generatedStart) {
                break;
            }
            if (position <= replacement.generatedEnd) {
                return position == replacement.generatedEnd
                        ? replacement.sourceEnd
                        : replacement.sourceStart;
            }
            delta += replacement.sourceLength() - replacement.generatedLength();
        }
        return position + delta;
    }

    private static String remapToken(
            ExpressionParseException exception,
            String source,
            List<Replacement> replacements
    ) {
        for (Replacement replacement : replacements) {
            if (exception.startPosition() < replacement.generatedEnd
                    && exception.endPosition() > replacement.generatedStart) {
                return source.substring(replacement.sourceStart, replacement.sourceEnd);
            }
        }
        return exception.token();
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

    public double evaluate(C context) {
        return this.expression.evaluate(context);
    }

    public boolean isConstant() {
        return this.expression.isConstant();
    }

    public boolean test(C context) {
        return this.expression.test(context);
    }

    private record Snippet(StringTag tag, String[] args) {

        private Object resolve(Context context) {
            return this.tag.resolve(this.args, context);
        }
    }

    private record Replacement(int generatedStart, int generatedEnd, int sourceStart, int sourceEnd) {

        private int generatedLength() {
            return this.generatedEnd - this.generatedStart;
        }

        private int sourceLength() {
            return this.sourceEnd - this.sourceStart;
        }
    }
}
