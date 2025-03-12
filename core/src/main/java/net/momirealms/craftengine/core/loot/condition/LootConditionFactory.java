package net.momirealms.craftengine.core.loot.condition;

import java.util.Map;

public interface LootConditionFactory {

    LootCondition create(Map<String, Object> arguments);
}
