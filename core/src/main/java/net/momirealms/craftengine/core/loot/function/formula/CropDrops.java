package net.momirealms.craftengine.core.loot.function.formula;

import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;

public final class CropDrops implements Formula {
    public static final FormulaFactory<CropDrops> FACTORY = new Factory();
    private final int extra;
    private final float probability;

    private CropDrops(int extra, float probability) {
        this.extra = extra;
        this.probability = probability;
    }

    @Override
    public int apply(int initialCount, int enchantmentLevel) {
        for (int i = 0; i < enchantmentLevel + this.extra; i++) {
            if (RandomUtils.generateRandomFloat(0, 1) < this.probability) {
                initialCount++;
            }
        }
        return initialCount;
    }

    private static class Factory implements FormulaFactory<CropDrops> {

        @Override
        public CropDrops create(Map<String, Object> arguments) {
            int extra = ResourceConfigUtils.getAsInt(arguments.getOrDefault("extra", 1), "extra");
            float probability = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("probability", 0.5f), "probability");
            return new CropDrops(extra, probability);
        }
    }
}