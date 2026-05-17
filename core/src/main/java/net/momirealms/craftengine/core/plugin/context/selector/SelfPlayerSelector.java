package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.function.Function;

public final class SelfPlayerSelector<CTX extends Context> implements PlayerSelector<CTX> {
    private SelfPlayerSelector() {}

    public static <CTX extends Context> SelfPlayerSelector<CTX> self() {
        return new SelfPlayerSelector<>();
    }

    @Override
    public List<Player> get(CTX context) {
        return List.of(context.getParameterOrThrow(DirectContextParameters.PLAYER));
    }

    public static <CTX extends Context> PlayerSelectorFactory<CTX> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements PlayerSelectorFactory<CTX> {

        @Override
        public PlayerSelector<CTX> create(ConfigSection section, Function<ConfigSection, Condition<CTX>> conditionFactory) {
            return new SelfPlayerSelector<>();
        }
    }
}
