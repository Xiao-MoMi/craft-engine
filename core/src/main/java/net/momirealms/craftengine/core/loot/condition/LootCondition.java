package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Predicate;

public interface LootCondition extends Predicate<LootContext> {

    Key type();
}
