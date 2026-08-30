package net.momirealms.craftengine.core.attribute.derived;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.expression.Expressions;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpressionDerivedValue implements DerivedValue {
    public static final DerivedValueFactory<ExpressionDerivedValue> FACTORY = args -> compile(args.assemblePath("expression"), args.getNonNullString("expression"));
    private static final Pattern ATTRIBUTE_ID_PATTERN = Pattern.compile(
            "(?<![a-z0-9_./-])([a-z0-9_.-]+:[a-z0-9_./-]+)(?![a-z0-9_./-])"
    );
    private final String path;
    private final String rawExpression;
    private final CompiledExpression<Function<Attribute, Double>> expression;
    private final List<VariableRef> variables;

    private ExpressionDerivedValue(String path, String rawExpression, CompiledExpression<Function<Attribute, Double>> expression, List<VariableRef> variables) {
        this.path = path;
        this.rawExpression = rawExpression;
        this.expression = expression;
        this.variables = variables;
    }

    public static ExpressionDerivedValue compile(String path, String rawExpression) {
        Substitution substitution = substituteAttributeIds(rawExpression);
        CompiledExpression<Function<Attribute, Double>> expression = Expressions.precompile(
                path,
                rawExpression,
                () -> new ExpressionCompiler<Function<Attribute, Double>>(name -> {
                    VariableRef variable = substitution.variables.get(name);
                    if (variable == null) {
                        throw Expressions.unknownParameter(name);
                    }
                    return ParameterBinding.number(resolver -> resolver.apply(variable.attribute));
                }).compile(substitution.expression)
        );
        return new ExpressionDerivedValue(
                path,
                rawExpression,
                expression,
                new ArrayList<>(substitution.variables.values())
        );
    }

    private static Substitution substituteAttributeIds(String source) {
        String maskedSource = maskStringLiterals(source);
        Matcher matcher = ATTRIBUTE_ID_PATTERN.matcher(maskedSource);
        StringBuilder substituted = new StringBuilder(source.length());
        Map<String, String> aliasesById = new LinkedHashMap<>();
        Map<String, VariableRef> variables = new LinkedHashMap<>();
        int aliasSeed = 0;
        int lastEnd = 0;
        while (matcher.find()) {
            String attributeId = source.substring(matcher.start(1), matcher.end(1));
            String alias = aliasesById.get(attributeId);
            if (alias == null) {
                do {
                    alias = createAlias(attributeId.length(), aliasSeed++);
                } while (variables.containsKey(alias) || containsIdentifier(maskedSource, alias));
                aliasesById.put(attributeId, alias);
                variables.put(alias, new VariableRef(Key.of(attributeId)));
            }
            substituted.append(source, lastEnd, matcher.start(1));
            substituted.append(alias);
            lastEnd = matcher.end(1);
        }
        substituted.append(source, lastEnd, source.length());
        return new Substitution(substituted.toString(), variables);
    }

    private static String maskStringLiterals(String source) {
        char[] masked = source.toCharArray();
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < masked.length; i++) {
            char current = masked[i];
            if (quote == 0) {
                if (current == '\'' || current == '"') {
                    quote = current;
                    masked[i] = ' ';
                }
                continue;
            }
            masked[i] = ' ';
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                quote = 0;
            }
        }
        return new String(masked);
    }

    private static String createAlias(int length, int seed) {
        String index = Integer.toString(seed, 36);
        if (index.length() >= length) {
            throw new IllegalArgumentException("Too many derived attributes in one expression");
        }
        return "_" + index + "_".repeat(length - index.length() - 1);
    }

    private static boolean containsIdentifier(String source, String identifier) {
        int fromIndex = 0;
        while ((fromIndex = source.indexOf(identifier, fromIndex)) >= 0) {
            int endIndex = fromIndex + identifier.length();
            boolean validStart = fromIndex == 0 || !isIdentifierPart(source.charAt(fromIndex - 1));
            boolean validEnd = endIndex == source.length() || !isIdentifierPart(source.charAt(endIndex));
            if (validStart && validEnd) {
                return true;
            }
            fromIndex++;
        }
        return false;
    }

    private static boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    @Override
    public void bind(Function<Key, Attribute> resolver) {
        for (VariableRef variable : this.variables) {
            Attribute attribute = resolver.apply(variable.attributeId);
            if (attribute == null) {
                throw new KnownResourceException("attribute.derived.unknown_attribute", this.path, variable.attributeId.asString(), this.rawExpression);
            }
            variable.attribute = attribute;
        }
    }

    @Override
    public double evaluate(Function<Attribute, Double> resolver) {
        try {
            return this.expression.evaluate(resolver);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to evaluate derived attribute expression: " + this.rawExpression, e);
        }
    }

    private static final class VariableRef {
        private final Key attributeId;
        private Attribute attribute;

        private VariableRef(Key attributeId) {
            this.attributeId = attributeId;
        }
    }

    private record Substitution(String expression, Map<String, VariableRef> variables) {
    }
}
