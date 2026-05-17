package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LimitCountFunction extends AbstractLootConditionalFunction {
    public static final LootFunctionFactory<LimitCountFunction> FACTORY = new Factory();
    @Nullable
    public final NumberProvider min;
    @Nullable
    public final NumberProvider max;

    private LimitCountFunction(List<Condition<LootContext>> predicates, @Nullable NumberProvider min, @Nullable NumberProvider max) {
        super(predicates);
        this.min = min;
        this.max = max;
    }

    @Override
    protected Item applyInternal(Item item, LootContext context) {
        int amount = item.count();
        if (this.min != null) {
            int minAmount = this.min.getInt(context);
            if (amount < minAmount) {
                item.count(minAmount);
            }
        }
        if (this.max != null) {
            int maxAmount = this.max.getInt(context);
            if (amount > maxAmount) {
                item.count(maxAmount);
            }
        }
        return item;
    }

    private static class Factory implements LootFunctionFactory<LimitCountFunction> {

        @Override
        public LimitCountFunction create(ConfigSection section) {
            return new LimitCountFunction(
                    section.getList("conditions", CommonConditions::fromConfig),
                    section.getValue("min", ConfigValue::getAsNumber),
                    section.getValue("max", ConfigValue::getAsNumber)
            );
        }
    }
}
