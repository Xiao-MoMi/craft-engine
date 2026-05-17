package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;

public final class RemoveEntityFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {

    private RemoveEntityFunction(List<Condition<CTX>> predicates) {
        super(predicates);
    }

    @Override
    public void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.ENTITY).ifPresent(Entity::remove);
    }

    public static <CTX extends Context> FunctionFactory<CTX, RemoveEntityFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RemoveEntityFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public RemoveEntityFunction<CTX> create(ConfigSection section) {
            return new RemoveEntityFunction<>(getPredicates(section));
        }
    }
}