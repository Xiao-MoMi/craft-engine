package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.Optional;

public final class UpdateInteractionFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {

    private UpdateInteractionFunction(List<Condition<CTX>> predicates) {
        super(predicates);
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Player> cancellable = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        cancellable.ifPresent(value -> value.updateLastSuccessfulInteractionTick(value.gameTicks()));
    }

    public static <CTX extends Context> FunctionFactory<CTX, UpdateInteractionFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, UpdateInteractionFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public UpdateInteractionFunction<CTX> create(ConfigSection section) {
            return new UpdateInteractionFunction<>(getPredicates(section));
        }
    }
}