package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class DamageFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private static final Key GENERIC = Key.of("generic");
    private final PlayerSelector<CTX> selector;
    private final Key damageType;
    private final NumberProvider amount;

    private DamageFunction(List<Condition<CTX>> predicates,
                           PlayerSelector<CTX> selector,
                           NumberProvider amount,
                           Key damageType) {
        super(predicates);
        this.selector = selector;
        this.damageType = damageType;
        this.amount = amount;
    }

    @Override
    protected void runInternal(CTX ctx) {
        if (this.selector != null) {
            this.selector.get(ctx).forEach(p -> p.damage(this.amount.getDouble(ctx), this.damageType, null));
        } else {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> it.damage(this.amount.getDouble(ctx), this.damageType, null));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, DamageFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, DamageFunction<CTX>> {
        private static final String[] DAMAGE_TYPE = new String[] {"damage_type", "damage-type"};
        private static final String[] AMOUNT = new String[] {"amount", "damage"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public DamageFunction<CTX> create(ConfigSection section) {
            return new DamageFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section), section.getNumber(AMOUNT, ConfigConstants.CONSTANT_ONE), section.getIdentifier(DAMAGE_TYPE, GENERIC)
            );
        }
    }
}