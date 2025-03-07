package net.momirealms.craftengine.core.loot.number;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class UniformNumberProvider implements NumberProvider {
    public static final Factory FACTORY = new Factory();
    private final NumberProvider min;
    private final NumberProvider max;

    public UniformNumberProvider(NumberProvider min, NumberProvider max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int getInt(LootContext context) {
        return context.randomSource().nextInt(this.min.getInt(context), this.max.getInt(context));
    }

    @Override
    public float getFloat(LootContext context) {
        return context.randomSource().nextFloat(this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public Key type() {
        return NumberProviders.UNIFORM;
    }

    public static class Factory implements NumberProviderFactory {
        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            Object min = arguments.getOrDefault("min", 1);
            Object max = arguments.getOrDefault("max", 1);
            return new UniformNumberProvider(NumberProviders.fromObject(min), NumberProviders.fromObject(max));
        }
    }
}
