package net.momirealms.craftengine.core.plugin.config.template.argument;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.template.ArgumentString;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
            return this.valueType.formatter().apply(new Expression(expression).evaluate());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process expression argument: " + this.expression, e);
        }
    }

    protected enum ValueType {
        INT(e -> e.getNumberValue().intValue()),
        LONG(e -> e.getNumberValue().longValue()),
        SHORT(e -> e.getNumberValue().shortValue()),
        DOUBLE(e -> e.getNumberValue().doubleValue()),
        FLOAT(e -> e.getNumberValue().floatValue()),
        BYTE(e -> e.getNumberValue().byteValue()),
        BOOLEAN(EvaluationValue::getBooleanValue),;

        private final Function<EvaluationValue, Object> formatter;

        ValueType(Function<EvaluationValue, Object> formatter) {
            this.formatter = formatter;
        }

        public Function<EvaluationValue, Object> formatter() {
            return this.formatter;
        }
    }

    private static class Factory implements TemplateArgumentFactory<ExpressionTemplateArgument> {
        private static final String[] VALUE_TYPE = new String[] {"value_type", "value-type"};

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
