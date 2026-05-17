package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.List;

public final class DropExpFunction extends AbstractLootConditionalFunction {
    public static final LootFunctionFactory<DropExpFunction> FACTORY = new Factory();
    private final NumberProvider value;

    private DropExpFunction(List<Condition<LootContext>> predicates, NumberProvider value) {
        super(predicates);
        this.value = value;
    }

    @Override
    protected Item applyInternal(Item item, LootContext context) {
        context.getOptionalParameter(DirectContextParameters.POSITION)
                .ifPresent(it -> it.world().dropExp(it, value.getInt(context)));
        return item;
    }

    private static class Factory implements LootFunctionFactory<DropExpFunction> {
        private static final String[] COUNT = new String[] {"count", "amount"};

        @Override
        public DropExpFunction create(ConfigSection section) {
            return new DropExpFunction(
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getNonNullNumber(COUNT)
            );
        }
    }
}
