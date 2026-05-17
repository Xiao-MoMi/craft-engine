package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AnyOfCondition<CTX extends Context> implements Condition<CTX> {
    private final Predicate<CTX> condition;

    private AnyOfCondition(List<? extends Condition<CTX>> conditions) {
        this.condition = MiscUtils.anyOf(conditions);
    }

    private AnyOfCondition(Predicate<CTX> condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(CTX ctx) {
        return this.condition.test(ctx);
    }

    public static <CTX extends Context> ConditionFactory<CTX, AnyOfCondition<CTX>> factory(Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private record Factory<CTX extends Context>(Function<ConfigSection, Condition<CTX>> factory) implements ConditionFactory<CTX, AnyOfCondition<CTX>> {
        private static final String[] TERMS = new String[] {"terms", "term"};

        @Override
        public AnyOfCondition<CTX> create(ConfigSection section) {
            List<Condition<CTX>> conditions = section.getSectionList(TERMS, this.factory);
            if (conditions.isEmpty()) {
                return new AnyOfCondition<>((ctx) -> true);
            } else if (conditions.size() == 1) {
                Condition<CTX> first = conditions.getFirst();
                return new AnyOfCondition<>(first);
            } else {
                return new AnyOfCondition<>(conditions);
            }
        }
    }
}