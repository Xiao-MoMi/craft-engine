package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.Optional;

public final class SwingHandFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Optional<InteractionHand> hand;

    private SwingHandFunction(List<Condition<CTX>> predicates,
                              Optional<InteractionHand> hand) {
        super(predicates);
        this.hand = hand;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Player> cancellable = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        cancellable.ifPresent(value -> {
            if (this.hand.isPresent()) {
                value.swingHand(this.hand.get());
            } else {
                value.swingHand(ctx.getOptionalParameter(DirectContextParameters.HAND).orElse(InteractionHand.MAIN_HAND));
            }
        });
    }

    public static <CTX extends Context> FunctionFactory<CTX, SwingHandFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SwingHandFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SwingHandFunction<CTX> create(ConfigSection section) {
            return new SwingHandFunction<>(
                    getPredicates(section),
                    Optional.ofNullable(section.getEnum("hand", InteractionHand.class))
            );
        }
    }
}