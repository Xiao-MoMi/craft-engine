package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.util.Key;

public record LootFunctionType<T extends LootFunction>(Key id, LootFunctionFactory<T> factory) {
}
