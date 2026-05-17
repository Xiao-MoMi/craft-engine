package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.selector.AllPlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.context.selector.SelfPlayerSelector;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractConditionalFunction<CTX extends Context> implements Function<CTX> {
    protected final List<Condition<CTX>> predicates;
    private final Predicate<CTX> compositePredicates;

    protected AbstractConditionalFunction(List<Condition<CTX>> predicates) {
        this.predicates = predicates;
        this.compositePredicates = MiscUtils.allOf(predicates);
    }

    @Override
    public void run(CTX ctx) {
        if (this.compositePredicates.test(ctx)) {
            this.runInternal(ctx);
        }
    }

    protected abstract void runInternal(CTX ctx);

    public static abstract class AbstractFactory<CTX extends Context, T extends Function<CTX>> implements FunctionFactory<CTX, T> {
        protected final java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory;

        public AbstractFactory(java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory) {
            this.conditionFactory = conditionFactory;
        }

        public java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory() {
            return this.conditionFactory;
        }

        protected PlayerSelector<CTX> getPlayerSelector(ConfigSection section) {
            Object target = section.get("target");
            if (target == null) return null;
            if (target instanceof Map<?,?>) {
                return PlayerSelectors.fromConfig(section.getNonNullSection("target"), conditionFactory);
            } else {
                String selector = target.toString();
                if (selector.equals("all") || selector.equals("@a")) {
                    return AllPlayerSelector.all();
                } else if (selector.equals("self") || selector.equals("@s")) {
                    return SelfPlayerSelector.self();
                }
            }
            return null;
        }

        protected List<Condition<CTX>> getPredicates(ConfigSection section) {
            if (section == null) return List.of();
            return section.getSectionList("conditions", this.conditionFactory);
        }
    }

    public static abstract class AbstractFunctionalFactory<CTX extends Context, T extends Function<CTX>> extends AbstractFactory<CTX, T> {
        protected final java.util.function.Function<ConfigSection, Function<CTX>> functionFactory;

        public AbstractFunctionalFactory(java.util.function.Function<ConfigSection, Function<CTX>> functionFactory, java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
            this.functionFactory = functionFactory;
        }

        public java.util.function.Function<ConfigSection, Function<CTX>> functionFactory() {
            return this.functionFactory;
        }

        protected List<Function<CTX>> getFunctions(ConfigSection section) {
            if (section == null) return List.of();
            return section.getSectionList("functions", this.functionFactory);
        }
    }
}
