package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class InvertedCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final LootCondition condition;

    public InvertedCondition(LootCondition condition) {
        this.condition = condition;
    }

    @Override
    public Key type() {
        return LootConditions.INVERTED;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return !condition.test(lootContext);
    }

    public static class Factory implements LootConditionFactory {
        @SuppressWarnings("unchecked")
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            Map<String, Object> term = (Map<String, Object>) arguments.get("term");
            return new InvertedCondition(LootConditions.fromMap(term));
        }
    }
}
