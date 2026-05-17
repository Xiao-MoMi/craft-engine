package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AllPlayerSelector<CTX extends Context> implements PlayerSelector<CTX> {
    private final Predicate<CTX> predicate;

    private AllPlayerSelector(List<Condition<CTX>> predicates) {
        this.predicate = MiscUtils.allOf(predicates);
    }

    private AllPlayerSelector(Predicate<CTX> predicate) {
        this.predicate = predicate;
    }

    private AllPlayerSelector() {
        this.predicate = null;
    }

    public static <CTX extends Context> AllPlayerSelector<CTX> all(List<Condition<CTX>> predicates) {
        return new AllPlayerSelector<>(predicates);
    }

    public static <CTX extends Context> AllPlayerSelector<CTX> all(Predicate<CTX> predicate) {
        return new AllPlayerSelector<>(predicate);
    }

    public static <CTX extends Context> AllPlayerSelector<CTX> all() {
        return new AllPlayerSelector<>();
    }

    public static <CTX extends Context> PlayerSelectorFactory<CTX> factory() {
        return new Factory<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Player> get(CTX context) {
        if (this.predicate == null) {
            return Arrays.asList(CraftEngine.instance().networkManager().onlineUsers());
        } else {
            List<Player> players = new ArrayList<>();
            for (Player player : CraftEngine.instance().networkManager().onlineUsers()) {
                PlayerOptionalContext newContext = PlayerOptionalContext.of(player, ContextHolder.builder()
                        .withOptionalParameter(DirectContextParameters.POSITION, context.getOptionalParameter(DirectContextParameters.POSITION).orElse(null))
                );
                if (!this.predicate.test((CTX) newContext)) {
                    continue;
                }
                players.add(player);
            }
            return players;
        }
    }

    private static class Factory<CTX extends Context> implements PlayerSelectorFactory<CTX> {

        @Override
        public PlayerSelector<CTX> create(ConfigSection section, Function<ConfigSection, Condition<CTX>> conditionFactory) {
            return new AllPlayerSelector<>();
        }
    }
}
