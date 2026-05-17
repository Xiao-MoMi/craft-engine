package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Optional;

public final class BreakBlockFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;

    private BreakBlockFunction(List<Condition<CTX>> predicates,
                               NumberProvider x,
                               NumberProvider y,
                               NumberProvider z) {
        super(predicates);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        optionalPlayer.ifPresent(player -> player.breakBlock(MiscUtils.floor(x.getDouble(ctx)), MiscUtils.floor(y.getDouble(ctx)), MiscUtils.floor(z.getDouble(ctx))));
    }

    public static <CTX extends Context> FunctionFactory<CTX, BreakBlockFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, BreakBlockFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public BreakBlockFunction<CTX> create(ConfigSection section) {
            return new BreakBlockFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z)
            );
        }
    }
}