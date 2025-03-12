package net.momirealms.craftengine.core.loot.function;

import java.util.Map;

public interface LootFunctionFactory<T> {

    LootFunction<T> create(Map<String, Object> arguments);
}
