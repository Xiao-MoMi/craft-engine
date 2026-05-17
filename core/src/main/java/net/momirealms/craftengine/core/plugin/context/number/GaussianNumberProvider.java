package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

public record GaussianNumberProvider(double min, double max, double mean, double stdDev, int maxAttempts) implements NumberProvider {
    public static final NumberProviderFactory<GaussianNumberProvider> FACTORY = new Factory();

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        int attempts = 0;
        while (attempts < this.maxAttempts) {
            double value = random.nextGaussian() * this.stdDev + this.mean;
            if (value >= this.min && value <= this.max) {
                return value;
            }
            attempts++;
        }
        return MiscUtils.clamp(this.mean, this.min, this.max);
    }

    private static class Factory implements NumberProviderFactory<GaussianNumberProvider> {
        private static final String[] STD_DEV = new String[] {"std_dev", "std-dev"};
        private static final String[] MAX_ATTEMPTS = new String[] {"max_attempts", "max-attempts"};

        @Override
        public GaussianNumberProvider create(ConfigSection section) {
            double min = section.getNonNullDouble("min");
            double max = section.getNonNullDouble("max");
            double mean = section.getDouble("mean", (min + max) / 2.0);
            double stdDev = section.getDouble(STD_DEV, (max - min) / 6.0);
            int maxAttempts = section.getInt(MAX_ATTEMPTS, 64);
            this.validateParameters(section.path(), min, max, stdDev, maxAttempts);
            return new GaussianNumberProvider(min, max, mean, stdDev, maxAttempts);
        }

        private void validateParameters(String path, double min, double max, double stdDev, int maxAttempts) {
            if (min >= max) {
                throw new KnownResourceException("number.less_than", path, "min", "max");
            }
            if (stdDev <= 0) {
                throw new KnownResourceException("number.greater_than", path, "std_dev", "0");
            }
            if (maxAttempts <= 0) {
                throw new KnownResourceException("number.greater_than", path, "max_attempts", "0");
            }
        }
    }
}