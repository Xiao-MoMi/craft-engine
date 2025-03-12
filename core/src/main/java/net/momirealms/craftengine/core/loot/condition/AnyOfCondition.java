package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public class AnyOfCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final List<LootCondition> conditions;

    public AnyOfCondition(List<LootCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public Key type() {
        return LootConditions.ANY_OF;
    }

    @Override
    public boolean test(LootContext lootContext) {
        for (LootCondition condition : conditions) {
            if (condition.test(lootContext)) {
                return true;
            }
        }
        return false;
    }

    public static class Factory implements LootConditionFactory {
        @SuppressWarnings("unchecked")
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            List<Map<String, Object>> terms = (List<Map<String, Object>>) arguments.get("terms");
            return new AnyOfCondition(LootConditions.fromMapList(terms));
        }
    }
}
