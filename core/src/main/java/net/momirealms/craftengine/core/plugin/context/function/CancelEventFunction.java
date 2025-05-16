package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CancelEventFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {

    public CancelEventFunction(List<Condition<CTX>> predicates) {
        super(predicates);
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Cancellable> cancellable = ctx.getOptionalParameter(DirectContextParameters.EVENT);
        cancellable.ifPresent(value -> value.setCancelled(true));
    }

    @Override
    public Key type() {
        return CommonFunctions.CANCEL_EVENT;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            return new CancelEventFunction<>(getPredicates(arguments));
        }
    }
}
