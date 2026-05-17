package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class InvertedCondition<CTX extends Context> implements Condition<CTX> {
    private final Predicate<CTX> condition;

    private InvertedCondition(Predicate<CTX> condition) {
        this.condition = condition;
    }

    public static <CTX extends Context> InvertedCondition<CTX> inverted(Predicate<CTX> condition) {
        return new InvertedCondition<>(condition);
    }

    @Override
    public boolean test(CTX ctx) {
        return !this.condition.test(ctx);
    }

    public static <CTX extends Context> ConditionFactory<CTX, InvertedCondition<CTX>> factory(Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private record Factory<CTX extends Context>(Function<ConfigSection, Condition<CTX>> factory) implements ConditionFactory<CTX, InvertedCondition<CTX>> {
        private static final String[] TERMS = new String[] {"terms", "term"};

        @Override
        public InvertedCondition<CTX> create(ConfigSection section) {
            List<Condition<CTX>> conditions = section.getSectionList(TERMS, this.factory);
            if (conditions.isEmpty()) {
                return new InvertedCondition<>((ctx) -> true);
            } else if (conditions.size() == 1) {
                Condition<CTX> first = conditions.getFirst();
                return new InvertedCondition<>(first);
            } else {
                return new InvertedCondition<>(MiscUtils.allOf(conditions));
            }
        }
    }
}