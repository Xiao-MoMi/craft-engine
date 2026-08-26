package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;

import java.util.List;

public final class SetVariableFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String variableName;
    private final java.util.function.Function<CTX, Object> valueProvider;

    private SetVariableFunction(List<Condition<CTX>> predicates,
                                String variableName,
                                java.util.function.Function<CTX, Object> valueProvider) {
        super(predicates);
        this.variableName = variableName;
        this.valueProvider = valueProvider;
    }

    @Override
    public void runInternal(CTX ctx) {
        ctx.setVariable(this.variableName, this.valueProvider.apply(ctx));
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetVariableFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetVariableFunction<CTX>> {
        private static final String[] NAME = ConfigKeys.of("name|var");
        private static final String[] VALUE_TYPE = ConfigKeys.of("value_type");
        private static final String[] NUMBER = ConfigKeys.of("number|value");
        private static final String[] TEXT = ConfigKeys.of("text|value");

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetVariableFunction<CTX> create(ConfigSection section) {
            String variableName = section.getNonNullString(NAME);
            ValueType defaultType = section.containsKey("number") ? ValueType.DOUBLE : ValueType.STRING;
            ValueType valueType = section.getEnum(VALUE_TYPE, ValueType.class, defaultType);
            java.util.function.Function<CTX, Object> valueProvider = switch (valueType) {
                case INT -> {
                    NumberProvider number = section.getNonNullNumber(NUMBER);
                    yield number::getInt;
                }
                case DOUBLE -> {
                    NumberProvider number = section.getNonNullNumber(NUMBER);
                    yield number::getDouble;
                }
                case STRING -> {
                    TextProvider text = TextProviders.fromString(section.getNonNullString(TEXT));
                    yield text::get;
                }
            };
            return new SetVariableFunction<>(getPredicates(section), variableName, valueProvider);
        }
    }

    private enum ValueType {
        INT,
        DOUBLE,
        STRING
    }
}
