package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.function.BiConsumer;

public final class SetExpFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final BiConsumer<Player, Integer> operation;

    private SetExpFunction(List<Condition<CTX>> predicates,
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
        for (Player player : this.selector.get(ctx)) {
            this.operation.accept(player, this.count.getInt(ctx));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetExpFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetExpFunction<CTX>> {
        private static final BiConsumer<Player, Integer> ADD_POINTS = Player::giveExperiencePoints;
        private static final BiConsumer<Player, Integer> SET_POINTS = (player, experience) -> {
            if (experience < player.getXpNeededForNextLevel()) {
                player.setExperiencePoints(experience);
            }
        };
        private static final String[] EXP = new String[] {"exp", "count", "value"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetExpFunction<CTX> create(ConfigSection section) {
            return new SetExpFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullNumber(EXP),
                    section.getBoolean("add") ? ADD_POINTS : SET_POINTS
            );
        }
    }
}