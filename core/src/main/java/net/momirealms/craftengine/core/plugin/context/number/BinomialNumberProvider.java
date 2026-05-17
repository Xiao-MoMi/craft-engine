package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.random.RandomSource;
import net.momirealms.craftengine.core.util.random.RandomUtils;

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
        public BinomialNumberProvider create(ConfigSection section) {
            return new BinomialNumberProvider(
                    section.getNonNullNumber("extra"),
                    section.getNonNullNumber("probability")
            );
        }
    }
}
