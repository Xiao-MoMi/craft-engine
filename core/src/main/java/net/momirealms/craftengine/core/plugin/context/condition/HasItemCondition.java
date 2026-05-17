package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.Optional;

public final class HasItemCondition<CTX extends Context> implements Condition<CTX> {
    public static final HasItemCondition<Context> INSTANCE = new HasItemCondition<>();

    private HasItemCondition() {
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        if (item.isEmpty()) return false;
        Item itemInHand = item.get();
        return !itemInHand.isEmpty();
    }

    public static <CTX extends Context> ConditionFactory<CTX, HasItemCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, HasItemCondition<CTX>> {

        @SuppressWarnings("unchecked")
        @Override
        public HasItemCondition<CTX> create(ConfigSection arguments) {
            return (HasItemCondition<CTX>) INSTANCE;
        }
    }
}