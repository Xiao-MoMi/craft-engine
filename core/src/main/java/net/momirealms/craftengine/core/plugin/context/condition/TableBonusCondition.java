package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.List;
import java.util.Optional;

public final class TableBonusCondition<CTX extends Context> implements Condition<CTX> {
    private final Key enchantmentType;
    private final List<Float> values;

    private TableBonusCondition(Key enchantmentType, List<Float> values) {
        this.enchantmentType = enchantmentType;
        this.values = values;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        int level = item.map(value -> value.getEnchantment(this.enchantmentType).map(Enchantment::level).orElse(0)).orElse(0);
        float f = this.values.get(Math.min(level, this.values.size() - 1));
        return RandomUtils.generateRandomFloat(0, 1) < f;
    }

    public static <CTX extends Context> ConditionFactory<CTX, TableBonusCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, TableBonusCondition<CTX>> {

        @Override
        public TableBonusCondition<CTX> create(ConfigSection section) {
            return new TableBonusCondition<>(
                    section.getNonNullIdentifier("enchantment"),
                    section.getNonEmptyList("chances", ConfigValue::getAsFloat)
            );
        }
    }
}