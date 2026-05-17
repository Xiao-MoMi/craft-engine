package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;

import java.util.List;

public final class HealFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider amount;

    private HealFunction(List<Condition<CTX>> predicates,
                         PlayerSelector<CTX> selector,
                         NumberProvider amount) {
        super(predicates);
        this.selector = selector;
        this.amount = amount;
    }

    @Override
    protected void runInternal(CTX ctx) {
        if (this.selector != null) {
            this.selector.get(ctx).forEach(p -> p.heal(this.amount.getDouble(ctx)));
        } else {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> it.heal(this.amount.getDouble(ctx)));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, HealFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, HealFunction<CTX>> {
        private static final String[] AMOUNT = new String[] {"amount", "heal"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public HealFunction<CTX> create(ConfigSection section) {
            return new HealFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNumber(AMOUNT, ConfigConstants.CONSTANT_ONE)
            );
        }
    }
}