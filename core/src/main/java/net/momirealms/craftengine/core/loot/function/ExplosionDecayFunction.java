package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;

public class ExplosionDecayFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    public ExplosionDecayFunction(List<LootCondition> predicates) {
        super(predicates);
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        Optional<Float> radius = context.getOptionalParameter(LootParameters.EXPLOSION_RADIUS);
        if (radius.isPresent()) {
            Random random = context.randomSource();
            float f = 1f / radius.get();
            int amount = item.count();
            int survive = 0;
            for (int j = 0; j < amount; j++) {
                if (random.nextFloat() <= f) {
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
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new ExplosionDecayFunction<>(conditions);
        }
    }
}
