package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class AllPlayerSelector<CTX extends Context> implements PlayerSelector<CTX> {
    private final Predicate<CTX> predicate;

    public AllPlayerSelector(List<Condition<CTX>> predicates) {
        this.predicate = MCUtils.allOf(predicates);
    }

    public AllPlayerSelector() {
        this.predicate = null;
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

    @Override
    public Key type() {
        return PlayerSelectors.ALL;
    }

    public static class FactoryImpl<CTX extends Context> implements PlayerSelectorFactory<CTX> {

        @Override
        public PlayerSelector<CTX> create(Map<String, Object> args, Function<Map<String, Object>, Condition<CTX>> conditionFactory) {


            return null;
        }
    }
}
