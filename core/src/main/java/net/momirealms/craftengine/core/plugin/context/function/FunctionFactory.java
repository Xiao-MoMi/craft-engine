package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Context;

public interface FunctionFactory<CTX extends Context, T extends Function<CTX>> {

    T create(ConfigSection section);
}
