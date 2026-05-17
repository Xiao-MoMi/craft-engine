package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class SetLevelFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final BiConsumer<Player, Integer> operation;

    private SetLevelFunction(List<Condition<CTX>> predicates,
                             PlayerSelector<CTX> selector,
                             NumberProvider count,
                             BiConsumer<Player, Integer> operation) {
        super(predicates);
        this.selector = selector;
        this.count = count;
        this.operation = operation;
    }

    @Override
    protected void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> this.operation.accept(player, this.count.getInt(ctx)));
        } else {
            for (Player player : this.selector.get(ctx)) {
                this.operation.accept(player, this.count.getInt(ctx));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetLevelFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetLevelFunction<CTX>> {
        private static final BiConsumer<Player, Integer> ADD_LEVELS = Player::giveExperienceLevels;
        private static final BiConsumer<Player, Integer> SET_LEVELS = Player::setExperienceLevels;
        private static final String[] LEVEL = new String[]{"level", "count"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetLevelFunction<CTX> create(ConfigSection section) {
            return new SetLevelFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullNumber(LEVEL),
                    section.getBoolean("add") ? ADD_LEVELS : SET_LEVELS
            );
        }
    }
}