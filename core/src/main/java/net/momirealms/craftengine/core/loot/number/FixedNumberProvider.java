package net.momirealms.craftengine.core.loot.number;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FixedNumberProvider implements NumberProvider {
    public static final Factory FACTORY = new Factory();
    private final float value;

    public FixedNumberProvider(float value) {
        this.value = value;
    }

    @Override
    public float getFloat(LootContext context) {
        return this.value;
    }

    @Override
    public Key type() {
        return NumberProviders.FIXED;
    }

    public static class Factory implements NumberProviderFactory {
        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            Number value = (Number) arguments.get("value");
            return new FixedNumberProvider(value.floatValue());
        }
    }
}
