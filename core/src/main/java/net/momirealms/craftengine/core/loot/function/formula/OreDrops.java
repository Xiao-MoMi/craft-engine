package net.momirealms.craftengine.core.loot.function.formula;

import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;

public final class OreDrops implements Formula {
    public static final FormulaFactory<OreDrops> FACTORY = new Factory();
    private static final OreDrops INSTANCE = new OreDrops();

    private OreDrops() {}

    @Override
    public int apply(int initialCount, int enchantmentLevel) {
        if (enchantmentLevel > 0) {
            int i = RandomUtils.generateRandomInt(0, enchantmentLevel + 2) - 1;
            if (i < 0) {
                i = 0;
            }
            return initialCount * (i + 1);
        } else {
            return initialCount;
        }
    }

    private static class Factory implements FormulaFactory<OreDrops> {

        @Override
        public OreDrops create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}