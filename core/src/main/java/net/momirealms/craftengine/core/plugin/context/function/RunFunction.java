package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class RunFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<Function<CTX>> functions;
    private final NumberProvider delay;

    private RunFunction(List<Condition<CTX>> predicates, NumberProvider delay, List<Function<CTX>> functions) {
        super(predicates);
        this.functions = functions;
        this.delay = delay;
    }

    @Override
    public void runInternal(CTX ctx) {
        int delay = this.delay.getInt(ctx);
        if (delay <= 0) {
            for (Function<CTX> function : functions) {
                function.run(ctx);
            }
        } else {
            Optional<WorldPosition> position = ctx.getOptionalParameter(DirectContextParameters.POSITION);
            if (!VersionHelper.isFolia || position.isEmpty()) {
                CraftEngine.instance().scheduler().platform().runLater(() -> {
                    for (Function<CTX> function : functions) {
                        function.run(ctx);
                    }
                }, delay);
            } else {
                WorldPosition pos = position.get();
                CraftEngine.instance().scheduler().platform().runLater(() -> {
                    for (Function<CTX> function : functions) {
                        function.run(ctx);
                    }
                }, delay, pos.world(), MiscUtils.floor(pos.x()) >> 4, MiscUtils.floor(pos.z()) >> 4);
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, RunFunction<CTX>> factory(java.util.function.Function<ConfigSection, Function<CTX>> f1, java.util.function.Function<ConfigSection, Condition<CTX>> f2) {
        return new Factory<>(f1, f2);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RunFunction<CTX>> {
        private final java.util.function.Function<ConfigSection, Function<CTX>> functionFactory;

        public Factory(java.util.function.Function<ConfigSection, Function<CTX>> functionFactory, java.util.function.Function<ConfigSection, Condition<CTX>> conditionFactory) {
            super(conditionFactory);
            this.functionFactory = functionFactory;
        }

        @Override
        public RunFunction<CTX> create(ConfigSection section) {
            List<Function<CTX>> functions = section.getSectionList("functions", this.functionFactory);
            return new RunFunction<>(
                    getPredicates(section),
                    section.getNumber("delay", ConfigConstants.CONSTANT_ZERO),
                    functions
            );
        }
    }
}