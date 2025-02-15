package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.loot.provider.NumberProvider;
import net.momirealms.craftengine.core.loot.provider.NumberProviders;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DropExpFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final NumberProvider value;

    public DropExpFunction(NumberProvider value, List<LootCondition> predicates) {
        super(predicates);
        this.value = value;
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        context.getOptionalParameter(LootParameters.WORLD)
                .ifPresent(it -> context.getOptionalParameter(LootParameters.LOCATION).ifPresent(loc -> it.dropExp(loc.toCenter(), value.getInt(context))));
        return item;
    }

    @Override
    public Key type() {
        return LootFunctions.DROP_EXP;
    }

    public static class Factory<T> implements LootFunctionFactory<T> {
        @SuppressWarnings("unchecked")
        @Override
        public LootFunction<T> create(Map<String, Object> arguments) {
            Object value = arguments.get("count");
            if (value == null) {
                throw new IllegalArgumentException("count can not be null");
            }
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new DropExpFunction<>(NumberProviders.fromObject(value), conditions);
        }
    }
}
