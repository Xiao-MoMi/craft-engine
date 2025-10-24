package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public class DamageItemFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider amount;

    public DamageItemFunction(NumberProvider amount, List<Condition<CTX>> predicates) {
        super(predicates);
        this.amount = amount;
    }

    @Override
    protected void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.ITEM).ifPresent(item -> {
            int damage = item.damage().orElse(0);
            int maxDamage = item.maxDamage();
            damage += amount.getInt(ctx);
            if (damage >= maxDamage) {
                item.count(0);
            } else {
                item.damage(damage);
            }
        });
    }

    @Override
    public Key type() {
        return CommonFunctions.DAMAGE_ITEM;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            NumberProvider amount = NumberProviders.fromObject(arguments.getOrDefault("amount", 1));
            return new DamageItemFunction<>(amount, getPredicates(arguments));
        }
    }
}
