package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExplosionDecayFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final LootFunctionFactory<?> FACTORY = new Factory<>();

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

    private static class Factory<T> implements LootFunctionFactory<T> {

        @Override
        public LootFunction<T> create(Map<String, Object> arguments) {
            List<Condition<LootContext>> conditions = ResourceConfigUtils.parseConfigAsList(arguments.get("conditions"), CommonConditions::fromMap);
            return new ExplosionDecayFunction<>(conditions);
        }
    }
}
