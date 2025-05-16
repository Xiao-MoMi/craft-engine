package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;

import java.util.Map;
import java.util.function.Function;

public interface PlayerSelectorFactory<CTX extends Context> {

    PlayerSelector<CTX> create(Map<String, Object> args, Function<Map<String, Object>, Condition<CTX>> conditionFactory);
}
