package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootConditions;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.RandomUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExplosionDecayFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    public ExplosionDecayFunction(List<Condition<LootContext>> predicates) {
        super(predicates);
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        Optional<Float> radius = context.getOptionalParameter(DirectContextParameters.EXPLOSION_RADIUS);
        if (radius.isPresent()) {
            float f = 1f / radius.get();
            int amount = item.count();
            int survive = 0;
            for (int j = 0; j < amount; j++) {
                if (RandomUtils.generateRandomFloat(0, 1) <= f) {
                    survive++;
                }
            }
            item.count(survive);
        }
        return item;
    }

    @Override
    public Key type() {
        return LootFunctions.EXPLOSION_DECAY;
    }

    public static class Factory<T> implements LootFunctionFactory<T> {
        @SuppressWarnings("unchecked")
        @Override
        public LootFunction<T> create(Map<String, Object> arguments) {
            List<Condition<LootContext>> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new ExplosionDecayFunction<>(conditions);
        }
    }
}
