package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface LootFunctionFactory<T extends LootFunction> {

    T create(ConfigSection section);
}
