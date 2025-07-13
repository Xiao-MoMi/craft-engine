package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetCountFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider count;
    private final boolean add;

    public SetCountFunction(NumberProvider count, boolean add, List<Condition<CTX>> predicates) {
        super(predicates);
        this.count = count;
        this.add = add;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Item<?>> optionalItem = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        if (optionalItem.isPresent()) {
            Item<?> item = optionalItem.get();
            if (this.add) {
                item.count(Math.min(item.count() + (this.count.getInt(ctx)), item.maxStackSize()));
            } else {
                item.count(Math.min(this.count.getInt(ctx), item.maxStackSize()));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.SET_COUNT;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object value = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("count"), "warning.config.function.set_count.missing_count");
            boolean add = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("add", false), "add");
            return new SetCountFunction<>(NumberProviders.fromObject(value), add, getPredicates(arguments));
        }
    }
}
