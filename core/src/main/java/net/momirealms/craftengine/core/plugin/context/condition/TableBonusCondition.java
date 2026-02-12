package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TableBonusCondition<CTX extends Context> implements Condition<CTX> {
    private final Key enchantmentType;
    private final List<Float> values;

    public TableBonusCondition(Key enchantmentType, List<Float> values) {
        this.enchantmentType = enchantmentType;
        this.values = values;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item<?>> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        int level = item.map(value -> value.getEnchantment(this.enchantmentType).map(Enchantment::level).orElse(0)).orElse(0);
        float f = this.values.get(Math.min(level, this.values.size() - 1));
        return RandomUtils.generateRandomFloat(0, 1) < f;
    }

    public static <CTX extends Context> ConditionFactory<CTX, TableBonusCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, TableBonusCondition<CTX>> {

        @Override
        public TableBonusCondition<CTX> create(Map<String, Object> arguments) {
            Object enchantmentObj = arguments.get("enchantment");
            if (enchantmentObj == null) {
                throw new LocalizedResourceConfigException("warning.config.condition.table_bonus.missing_enchantment");
            }
            Key enchantmentType = Key.of(enchantmentObj.toString());
            Object chances = arguments.get("chances");
            if (chances != null) {
                if (chances instanceof Number number) {
                    return new TableBonusCondition<>(enchantmentType, List.of(number.floatValue()));
                } else if (chances instanceof List<?> list) {
                    List<Float> values = new ArrayList<>(list.size());
                    for (Object o : list) {
                        values.add(ResourceConfigUtils.getAsFloat(o, "chances"));
                    }
                    return new TableBonusCondition<>(enchantmentType, values);
                }
            }
            throw new LocalizedResourceConfigException("warning.config.condition.table_bonus.missing_chances");
        }
    }
}