package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record GaussianNumberProvider(double min, double max, double mean, double stdDev, int maxAttempts) implements NumberProvider {
    public static final NumberProviderFactory<GaussianNumberProvider> FACTORY = new Factory();

    public GaussianNumberProvider(double min, double max, double mean, double stdDev, int maxAttempts) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stdDev = stdDev;
        this.maxAttempts = maxAttempts;
        validateParameters();
    }

    private void validateParameters() {
        if (this.min >= this.max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        if (this.stdDev <= 0) {
            throw new IllegalArgumentException("std-dev must be greater than 0");
        }
        if (this.maxAttempts <= 0) {
            throw new IllegalArgumentException("max-attempts must be greater than 0");
        }
    }

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            double value = random.nextGaussian() * stdDev + mean;
            if (value >= min && value <= max) {
                return value;
            }
            attempts++;
        }
        return MiscUtils.clamp(this.mean, this.min, this.max);
    }

    private static class Factory implements NumberProviderFactory<GaussianNumberProvider> {

        @Override
        public GaussianNumberProvider create(Map<String, Object> arguments) {
            double min = ResourceConfigUtils.getAsDouble(ResourceConfigUtils.requireNonNullOrThrow(arguments.get("min"), "warning.config.number.gaussian.missing_min"), "min");
            double max = ResourceConfigUtils.getAsDouble(ResourceConfigUtils.requireNonNullOrThrow(arguments.get("max"), "warning.config.number.gaussian.missing_max"), "max");
            double mean = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("mean", (min + max) / 2.0), "mean");
            double stdDev = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("std-dev", (max - min) / 6.0), "std-dev");
            int maxAttempts = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-attempts", 128), "max-attempts");
            return new GaussianNumberProvider(min, max, mean, stdDev, maxAttempts);
        }
    }

    @Override
    public @NotNull String toString() {
        return String.format("GaussianNumberProvider{min=%.2f, max=%.2f, mean=%.2f, stdDev=%.2f, maxAttempts=%d}",
                min, max, mean, stdDev, maxAttempts);
    }
}