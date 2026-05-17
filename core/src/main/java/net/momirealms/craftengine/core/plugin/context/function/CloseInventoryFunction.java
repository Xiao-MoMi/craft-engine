package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CloseInventoryFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;

    private CloseInventoryFunction(List<Condition<CTX>> predicates,
                                   @Nullable PlayerSelector<CTX> selector) {
        super(predicates);
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(Player::closeInventory);
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                viewer.closeInventory();
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, CloseInventoryFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, CloseInventoryFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public CloseInventoryFunction<CTX> create(ConfigSection section) {
            return new CloseInventoryFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section)
            );
        }
    }
}