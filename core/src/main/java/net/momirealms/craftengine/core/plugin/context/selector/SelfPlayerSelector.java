package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SelfPlayerSelector<CTX extends Context> implements PlayerSelector<CTX> {

    @Override
    public List<Player> get(CTX context) {
        return List.of(context.getParameterOrThrow(DirectContextParameters.PLAYER));
    }

    @Override
    public Key type() {
        return PlayerSelectors.SELF;
    }

    public static class FactoryImpl<CTX extends Context> implements PlayerSelectorFactory<CTX> {
        @Override
        public PlayerSelector<CTX> create(Map<String, Object> args, Function<Map<String, Object>, Condition<CTX>> conditionFactory) {
            return new SelfPlayerSelector<>();
        }
    }
}
