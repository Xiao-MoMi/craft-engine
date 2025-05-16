package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractConditionalFunction<CTX extends Context> implements Function<CTX> {
    protected final List<Condition<CTX>> predicates;
    private final Predicate<CTX> compositePredicates;

    public AbstractConditionalFunction(List<Condition<CTX>> predicates) {
        this.predicates = predicates;
        this.compositePredicates = MCUtils.allOf(predicates);
    }

    @Override
    public void run(CTX ctx) {
        if (this.compositePredicates.test(ctx)) {
            this.runInternal(ctx);
        }
    }

    protected abstract void runInternal(CTX ctx);

    public static abstract class AbstractFactory<CTX extends Context> implements FunctionFactory<CTX> {
        private final java.util.function.Function<Map<String, Object>, Condition<CTX>> factory;

        public AbstractFactory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            this.factory = factory;
        }

        public java.util.function.Function<Map<String, Object>, Condition<CTX>> conditionFactory() {
            return factory;
        }

        protected List<Condition<CTX>> getPredicates(Map<String, Object> arguments) {
            Object predicates = arguments.get("conditions");
            if (predicates == null) return List.of();
            if (predicates instanceof List<?> list) {
                List<Condition<CTX>> conditions = new ArrayList<>(list.size());
                for (Object o : list) {
                    conditions.add(factory.apply(MiscUtils.castToMap(o, false)));
                }
                return conditions;
            } else if (predicates instanceof Map<?,?> map) {
                return List.of(factory.apply(MiscUtils.castToMap(map, false)));
            }
            throw new IllegalArgumentException("Unsupported condition type: " + predicates.getClass().getSimpleName());
        }
    }
}
