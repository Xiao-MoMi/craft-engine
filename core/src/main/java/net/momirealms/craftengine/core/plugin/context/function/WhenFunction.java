package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WhenFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final TextProvider source;
    private final Map<String, Function<CTX>> whenMap;
    private final Function<CTX> fallback;

    private WhenFunction(List<Condition<CTX>> predicates, TextProvider source, Map<String, Function<CTX>> whenMap, Function<CTX> fallback) {
        super(predicates);
        this.whenMap = whenMap;
        this.source = source;
        this.fallback = fallback;
    }

    @Override
    public void runInternal(CTX ctx) {
        String text = this.source.get(ctx);
        Function<CTX> function = this.whenMap.getOrDefault(text, this.fallback);
        function.run(ctx);
    }

    public static <CTX extends Context> FunctionFactory<CTX, WhenFunction<CTX>> factory(java.util.function.Function<ConfigSection, Function<CTX>> f1, java.util.function.Function<ConfigSection, Condition<CTX>> f2) {
        return new Factory<>(f1, f2);
    }

    private static class Factory<CTX extends Context> extends AbstractFunctionalFactory<CTX, WhenFunction<CTX>> {
        private static final String[] CASES = new String[]{"cases", "case"};

        public Factory(java.util.function.Function<ConfigSection, Function<CTX>> functionFactory, java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory) {
            super(functionFactory, conditionFactory);
        }

        @Override
        public WhenFunction<CTX> create(ConfigSection section) {
            TextProvider source = TextProviders.fromString(section.getNonNullString("source"));
            List<Pair<List<String>, Function<CTX>>> pairs = section.getSectionList(CASES, s -> {
                List<String> when = s.getStringList("when");
                List<Function<CTX>> functions = getFunctions(s);
                return Pair.of(when, Function.allOf(functions));
            });
            Map<String, Function<CTX>> whenMap = new HashMap<>();
            for (Pair<List<String>, Function<CTX>> pair : pairs) {
                for (String when : pair.left()) {
                    whenMap.put(when, pair.right());
                }
            }
            return new WhenFunction<>(
                    getPredicates(section),
                    source,
                    whenMap,
                    Function.allOf(section.getSectionList("fallback", super.functionFactory))
            );
        }
    }
}