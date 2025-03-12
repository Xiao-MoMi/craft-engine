package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FallingCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    public static final FallingCondition INSTANCE = new FallingCondition();

    @Override
    public Key type() {
        return LootConditions.FALLING_BLOCK;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getOptionalParameter(LootParameters.FALLING_BLOCK).orElse(false);
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
