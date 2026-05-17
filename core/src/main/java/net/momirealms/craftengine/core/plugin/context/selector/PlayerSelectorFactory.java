package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;

import java.util.function.Function;

public interface PlayerSelectorFactory<CTX extends Context> {

    PlayerSelector<CTX> create(ConfigSection section, Function<ConfigSection, Condition<CTX>> conditionFactory);
}
