package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;
import java.util.function.Function;

public final class EnchantmentCondition<CTX extends Context> implements Condition<CTX> {
    private final Key id;
    private final Function<Integer, Boolean> expression;

    private EnchantmentCondition(Key id, Function<Integer, Boolean> expression) {
        this.expression = expression;
        this.id = id;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        if (item.isEmpty()) return false;
        Optional<Enchantment> enchantment = item.get().getEnchantment(id);
        int level = enchantment.map(Enchantment::level).orElse(0);
        return this.expression.apply(level);
    }

    public static <CTX extends Context> ConditionFactory<CTX, EnchantmentCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, EnchantmentCondition<CTX>> {

        @Override
        public EnchantmentCondition<CTX> create(ConfigSection section) {
            String predicate = section.getNonNullString("predicate");
            String[] split = predicate.split("(<=|>=|<|>|==|=)", 2);
            int level;
            try {
                level = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                throw new KnownResourceException("condition.enchantment.invalid_format", section.assemblePath("predicate"), predicate);
            }
            String operator = predicate.substring(split[0].length(), predicate.length() - split[1].length());
            Function<Integer, Boolean> expression;
            switch (operator) {
                case "<" -> expression = (i -> i < level);
                case ">" -> expression = (i -> i > level);
                case "==", "=" -> expression = (i -> i == level);
                case "<=" -> expression = (i -> i <= level);
                case ">=" -> expression = (i -> i >= level);
                default -> throw new KnownResourceException("condition.enchantment.invalid_format", section.assemblePath("predicate"), predicate);
            }
            return new EnchantmentCondition<>(Key.of(split[0]), expression);
        }
    }
}