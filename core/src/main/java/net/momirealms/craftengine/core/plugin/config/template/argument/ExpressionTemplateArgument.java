package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.template.ArgumentString;
import net.momirealms.craftengine.core.plugin.context.expression.Expressions;

import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleFunction;

// TODO 存在设计缺陷
public final class ExpressionTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<ExpressionTemplateArgument> FACTORY = new Factory();
    private final ArgumentString expression;
    private final ValueType valueType;

    private ExpressionTemplateArgument(String node, String expression, ValueType valueType) {
        this.expression = ArgumentString.preParse(node, expression);
        this.valueType = valueType;
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        String expression = Optional.ofNullable(this.expression.get(node, arguments)).map(String::valueOf).orElse(null);
        if (expression == null) return null;
        try {
            return this.valueType.format(Expressions.evaluate(expression));
        } catch (Exception e) {
            throw new RuntimeException("Failed to process expression argument: " + this.expression, e);
        }
    }

    private enum ValueType {
        INT(value -> (int) value),
        LONG(value -> (long) value),
        SHORT(value -> (short) value),
        DOUBLE(value -> value),
        FLOAT(value -> (float) value),
        BYTE(value -> (byte) value),
        BOOLEAN(value -> value != 0D),;

        private final DoubleFunction<Object> formatter;

        ValueType(DoubleFunction<Object> formatter) {
            this.formatter = formatter;
        }

        private Object format(double value) {
            return this.formatter.apply(value);
        }
    }

    private static class Factory implements TemplateArgumentFactory<ExpressionTemplateArgument> {
        private static final String[] VALUE_TYPE = ConfigKeys.of("value_type");

        @Override
        public ExpressionTemplateArgument create(ConfigSection section) {
            return new ExpressionTemplateArgument(
                    section.assemblePath("expression"),
                    section.getNonEmptyString("expression"),
                    section.getEnum(VALUE_TYPE, ValueType.class, ValueType.DOUBLE)
            );
        }
    }
}
