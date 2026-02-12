package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;

public record BinomialNumberProvider(NumberProvider trials, NumberProvider successProbability) implements NumberProvider {
    public static final NumberProviderFactory<BinomialNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(RandomSource random) {
        return getInt(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        return getInt(random);
    }

    @Override
    public int getInt(RandomSource random) {
        int trialCount = this.trials.getInt(random);
        float probability = this.successProbability.getFloat(random);
        int successCount = 0;

        for (int i = 0; i < trialCount; i++) {
            if (RandomUtils.generateRandomFloat(0, 1) < probability) {
                successCount++;
            }
        }
        return successCount;
    }

    private static class Factory implements NumberProviderFactory<BinomialNumberProvider> {

        @Override
        public BinomialNumberProvider create(Map<String, Object> arguments) {
            Object trials = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("extra"), "warning.config.number.binomial.missing_extra");
            Object successProbability = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("probability"), "warning.config.number.binomial.missing_probability");
            return new BinomialNumberProvider(NumberProviders.fromObject(trials), NumberProviders.fromObject(successProbability));
        }
    }
}
