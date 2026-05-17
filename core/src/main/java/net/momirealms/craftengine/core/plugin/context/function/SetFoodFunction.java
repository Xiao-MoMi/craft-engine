package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.Optional;

public final class SetFoodFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final boolean add;

    private SetFoodFunction(List<Condition<CTX>> predicates,
                            PlayerSelector<CTX> selector,
                            boolean add,
                            NumberProvider count) {
        super(predicates);
        this.count = count;
        this.add = add;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> player.setFoodLevel(this.add ? player.foodLevel() + this.count.getInt(ctx) : this.count.getInt(ctx)));
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target));
                target.setFoodLevel(this.add ? target.foodLevel() + this.count.getInt(relationalContext) : this.count.getInt(relationalContext));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetFoodFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetFoodFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetFoodFunction<CTX> create(ConfigSection section) {
            return new SetFoodFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section), section.getBoolean("add"),
                    section.getNonNullValue("food", ConfigConstants.ARGUMENT_NUMBER, NumberProviders::fromConfig)
            );
        }
    }
}