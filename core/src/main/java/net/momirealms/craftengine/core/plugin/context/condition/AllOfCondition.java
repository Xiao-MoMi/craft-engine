package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AllOfCondition<CTX extends Context> implements Condition<CTX> {
    private final Predicate<CTX> condition;

    private AllOfCondition(List<? extends Condition<CTX>> conditions) {
        this.condition = MiscUtils.allOf(conditions);
    }

    private AllOfCondition(Predicate<CTX> condition) {
        this.condition = condition;
    }

    public static AllOfCondition<Context> allOf(List<? extends Condition<Context>> conditions) {
        return new AllOfCondition<>(conditions);
    }

    @Override
    public boolean test(CTX ctx) {
        return this.condition.test(ctx);
    }

    public static <CTX extends Context> ConditionFactory<CTX, AllOfCondition<CTX>> factory(Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private record Factory<CTX extends Context>(Function<ConfigSection, Condition<CTX>> factory) implements ConditionFactory<CTX, AllOfCondition<CTX>> {
        private static final String[] TERMS = new String[]{"terms", "term"};

        @Override
        public AllOfCondition<CTX> create(ConfigSection section) {
            List<Condition<CTX>> conditions = section.getSectionList(TERMS, this.factory);
            if (conditions.isEmpty()) {
                return new AllOfCondition<>((ctx) -> true);
            } else if (conditions.size() == 1) {
                Condition<CTX> first = conditions.getFirst();
                return new AllOfCondition<>(first);
            } else {
                return new AllOfCondition<>(conditions);
            }
        }
    }
}
