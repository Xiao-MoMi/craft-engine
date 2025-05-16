package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class UniformNumberProvider implements NumberProvider {
    public static final FactoryImpl FACTORY = new FactoryImpl();
    private final NumberProvider min;
    private final NumberProvider max;

    public UniformNumberProvider(NumberProvider min, NumberProvider max) {
        this.min = min;
        this.max = max;
    }

    public NumberProvider max() {
        return max;
    }

    public NumberProvider min() {
        return min;
    }

    @Override
    public int getInt(Context context) {
        return RandomUtils.generateRandomInt(this.min.getInt(context), this.max.getInt(context) + 1);
    }

    @Override
    public float getFloat(Context context) {
        return RandomUtils.generateRandomFloat(this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public Key type() {
        return NumberProviders.UNIFORM;
    }

    public static class FactoryImpl implements NumberProviderFactory {

        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            Object min = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("min"), "warning.config.number.uniform.missing_min");
            Object max = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("max"), "warning.config.number.uniform.missing_max");
            return new UniformNumberProvider(NumberProviders.fromObject(min), NumberProviders.fromObject(max));
        }
    }
}
