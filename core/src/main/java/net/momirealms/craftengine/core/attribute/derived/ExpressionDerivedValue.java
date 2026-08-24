package net.momirealms.craftengine.core.attribute.derived;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.expression.Expressions;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ExpressionDerivedValue implements DerivedValue {
    public static final DerivedValueFactory<ExpressionDerivedValue> FACTORY = args -> compile(args.assemblePath("expression"), args.getNonNullString("expression"));
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
        List<VariableRef> variables = new ArrayList<>();
        CompiledExpression<Function<Attribute, Double>> expression = Expressions.precompile(
                path,
                rawExpression,
                () -> new ExpressionCompiler<Function<Attribute, Double>>(name -> {
                    VariableRef variable = new VariableRef(Key.of(name.replaceFirst("_", ":")));
                    variables.add(variable);
                    return ParameterBinding.number(resolver -> resolver.apply(variable.attribute));
                }).compile(rawExpression)
        );
        return new ExpressionDerivedValue(path, rawExpression, expression, variables);
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
}
