package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.random.RandomSource;
import net.momirealms.craftengine.core.util.random.RandomUtils;

public record UniformNumberProvider(NumberProvider min, NumberProvider max) implements NumberProvider {
    public static final NumberProviderFactory<UniformNumberProvider> FACTORY = new Factory();

    @Override
    public int getInt(RandomSource random) {
        return RandomUtils.generateRandomInt(this.min.getInt(random), this.max.getInt(random) + 1);
    }

    @Override
    public double getDouble(RandomSource random) {
        return RandomUtils.generateRandomDouble(this.min.getDouble(random), this.max.getDouble(random));
    }

    @Override
    public float getFloat(RandomSource random) {
        return RandomUtils.generateRandomFloat(this.min.getFloat(random), this.max.getFloat(random));
    }

    private static class Factory implements NumberProviderFactory<UniformNumberProvider> {

        @Override
        public UniformNumberProvider create(ConfigSection section) {
            return new UniformNumberProvider(
                    section.getNonNullNumber("min"),
                    section.getNonNullNumber("max")
            );
        }
    }
}
