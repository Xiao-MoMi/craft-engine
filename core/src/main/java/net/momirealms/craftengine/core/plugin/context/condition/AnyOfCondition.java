package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class AnyOfCondition<CTX extends Context> implements Condition<CTX> {
    protected final Predicate<CTX> condition;

    public AnyOfCondition(List<? extends Condition<CTX>> conditions) {
        this.condition = MCUtils.anyOf(conditions);
    }

    @Override
    public boolean test(CTX ctx) {
        return this.condition.test(ctx);
    }

    @Override
    public Key type() {
        return CommonConditions.ANY_OF;
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {
        private final Function<Map<String, Object>, Condition<CTX>> factory;

        public FactoryImpl(Function<Map<String, Object>, Condition<CTX>> factory) {
            this.factory = factory;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            Object termsArg = ResourceConfigUtils.requireNonNullOrThrow(
                    ResourceConfigUtils.get(arguments, "terms", "term"),
                    "warning.config.condition.any_of.missing_terms"
            );
            if (termsArg instanceof Map<?, ?> map) {
                return new AnyOfCondition<>(List.of(factory.apply(MiscUtils.castToMap(map, false))));
            } else if (termsArg instanceof List<?> list) {
                List<Condition<CTX>> conditions = new ArrayList<>();
                for (Map<String, Object> term : (List<Map<String, Object>>) list) {
                    conditions.add(factory.apply(term));
                }
                return new AnyOfCondition<>(conditions);
            } else {
                throw new LocalizedResourceConfigException("warning.config.condition.any_of.invalid_terms_type", termsArg.getClass().getSimpleName());
            }
        }
    }
}
