package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;
import java.util.Optional;

public final class SetSaturationFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final boolean add;

    private SetSaturationFunction(List<Condition<CTX>> predicates,
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
            optionalPlayer.ifPresent(player -> player.setSaturation(this.add ? player.saturation() + this.count.getFloat(ctx) : this.count.getFloat(ctx)));
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target));
                target.setSaturation(this.add ? target.saturation() + this.count.getFloat(relationalContext) : this.count.getFloat(relationalContext));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetSaturationFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetSaturationFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetSaturationFunction<CTX> create(ConfigSection section) {
            return new SetSaturationFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section), section.getBoolean("add"),
                    section.getNonNullNumber("saturation")
            );
        }
    }
}