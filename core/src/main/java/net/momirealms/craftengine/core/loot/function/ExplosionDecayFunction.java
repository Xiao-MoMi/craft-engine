package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.List;
import java.util.Optional;

public final class ExplosionDecayFunction extends AbstractLootConditionalFunction {
    public static final LootFunctionFactory<ExplosionDecayFunction> FACTORY = new Factory();

    public ExplosionDecayFunction(List<Condition<LootContext>> predicates) {
        super(predicates);
    }

    @Override
    protected Item applyInternal(Item item, LootContext context) {
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

    private static class Factory implements LootFunctionFactory<ExplosionDecayFunction> {

        @Override
        public ExplosionDecayFunction create(ConfigSection section) {
            return new ExplosionDecayFunction(section.getList("conditions", CommonConditions::fromConfig));
        }
    }
}
