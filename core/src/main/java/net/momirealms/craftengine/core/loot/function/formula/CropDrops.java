package net.momirealms.craftengine.core.loot.function.formula;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.random.RandomUtils;

public final class CropDrops implements Formula {
    public static final FormulaFactory<CropDrops> FACTORY = new Factory();
    public final int extra;
    public final float probability;

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
        private static final String[] PROBABILITY = new String[] {"probability", "chance"};

        @Override
        public CropDrops create(ConfigSection section) {
            return new CropDrops(
                    section.getInt("extra", 1),
                    section.getFloat(PROBABILITY, 0.5f)
            );
        }
    }
}