package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Optional;

public final class RandomCondition<CTX extends Context> implements Condition<CTX> {
    private final NumberProvider chance;
    private final boolean previous;

    private RandomCondition(NumberProvider chance, boolean previous) {
        this.chance = chance;
        this.previous = previous;
    }

    @Override
    public boolean test(CTX ctx) {
        if (this.previous) {
            Optional<Double> random = ctx.getOptionalParameter(DirectContextParameters.LAST_RANDOM);
            return random.map(d -> d < this.chance.getFloat(ctx))
                    .orElseGet(() -> RandomUtils.generateRandomFloat(0, 1) < this.chance.getFloat(ctx));
        } else {
            Optional<Double> random = ctx.getOptionalParameter(DirectContextParameters.RANDOM);
            return random.map(d -> d < this.chance.getFloat(ctx))
                    .orElseGet(() -> RandomUtils.generateRandomFloat(0, 1) < this.chance.getFloat(ctx));
        }
    }

    public static <CTX extends Context> ConditionFactory<CTX, RandomCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, RandomCondition<CTX>> {
        private static final String[] USE_LAST = new String[] {"use-last", "use_last"};

        @Override
        public RandomCondition<CTX> create(ConfigSection section) {
            return new RandomCondition<>(
                    section.getNumber("value", ConfigConstants.CONSTANT_HALF),
                    section.getBoolean(USE_LAST)
            );
        }
    }
}