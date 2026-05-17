package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.Pair;

import java.util.List;
import java.util.function.Predicate;

public final class IfElseFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<Pair<Predicate<CTX>, Function<CTX>>> conditions;

    private IfElseFunction(List<Condition<CTX>> predicates,
                           List<Pair<Predicate<CTX>, Function<CTX>>> conditions) {
        super(predicates);
        this.conditions = conditions;
    }

    @Override
    public void runInternal(CTX ctx) {
        for (Pair<Predicate<CTX>, Function<CTX>> condition : this.conditions) {
            if (condition.left().test(ctx)) {
                condition.right().run(ctx);
                break;
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, IfElseFunction<CTX>> factory(java.util.function.Function<ConfigSection, Function<CTX>> f1, java.util.function.Function<ConfigSection, Condition<CTX>> f2) {
        return new Factory<>(f1, f2);
    }

    private static class Factory<CTX extends Context> extends AbstractFunctionalFactory<CTX, IfElseFunction<CTX>> {
        private static final String[] RULES = new String[] {"rules", "rule"};

        public Factory(java.util.function.Function<ConfigSection, Function<CTX>> functionFactory, java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory) {
            super(functionFactory, conditionFactory);
        }

        @Override
        public IfElseFunction<CTX> create(ConfigSection section) {
            List<Pair<Predicate<CTX>, Function<CTX>>> branches = section.getSectionList(RULES, s -> {
                List<Condition<CTX>> conditions = getPredicates(s);
                List<Function<CTX>> functions = getFunctions(s);
                return new Pair<>(MiscUtils.allOf(conditions), Function.allOf(functions));
            });
            return new IfElseFunction<>(getPredicates(section), branches);
        }
    }
}