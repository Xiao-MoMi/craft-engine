package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;
import java.util.Optional;

public final class SetCountFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider count;
    private final boolean add;

    private SetCountFunction(List<Condition<CTX>> predicates,
                             boolean add,
                             NumberProvider count) {
        super(predicates);
        this.count = count;
        this.add = add;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Item> optionalItem = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        if (optionalItem.isPresent()) {
            Item item = optionalItem.get();
            if (this.add) {
                item.count(Math.min(item.count() + (this.count.getInt(ctx)), item.maxStackSize()));
            } else {
                item.count(Math.min(this.count.getInt(ctx), item.maxStackSize()));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetCountFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetCountFunction<CTX>> {
        private static final String[] COUNT = new String[] {"count", "amount"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetCountFunction<CTX> create(ConfigSection section) {
            return new SetCountFunction<>(
                    getPredicates(section),
                    section.getBoolean("add"),
                    section.getNonNullNumber(COUNT)
            );
        }
    }
}