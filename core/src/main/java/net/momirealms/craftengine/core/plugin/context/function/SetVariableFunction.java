package net.momirealms.craftengine.core.plugin.context.function;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;

import java.util.List;

public final class SetVariableFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Either<TextProvider, NumberProvider> either;
    private final String variableName;
    private final boolean asInt;

    private SetVariableFunction(List<Condition<CTX>> predicates,
                                String variableName,
                                Either<TextProvider, NumberProvider> either,
                                boolean asInt) {
        super(predicates);
        this.either = either;
        this.variableName = variableName;
        this.asInt = asInt;
    }

    @Override
    public void runInternal(CTX ctx) {
        ContextHolder contexts = ctx.contexts();
        if (contexts.immutable()) return;
        this.either.ifLeft(text -> contexts.withParameter(ContextKey.direct("var_" + this.variableName), text.get(ctx)))
                .ifRight(number -> contexts.withParameter(ContextKey.direct("var_" + this.variableName), asInt ? number.getInt(ctx) : number.getDouble(ctx)));
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetVariableFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetVariableFunction<CTX>> {
        private static final String[] AS_INT = new String[]{"as_int", "as-int"};
        private static final String[] NAME = new String[]{"name", "var"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetVariableFunction<CTX> create(ConfigSection section) {
            String variableName = section.getNonNullString(NAME);
            if (section.containsKey("number")) {
                return new SetVariableFunction<>(
                        getPredicates(section),
                        variableName,
                        Either.right(section.getNumber("number")),
                        section.getBoolean(AS_INT)
                );
            } else {
                String text = section.getNonNullString("text");
                return new SetVariableFunction<>(
                        getPredicates(section),
                        variableName,
                        Either.left(TextProviders.fromString(text)),
                        false
                );
            }
        }
    }
}