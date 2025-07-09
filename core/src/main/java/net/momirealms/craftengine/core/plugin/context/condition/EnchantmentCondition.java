package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class EnchantmentCondition<CTX extends Context> implements Condition<CTX> {
    private final Key id;
    private final Function<Integer, Boolean> expression;

    public EnchantmentCondition(Key id, Function<Integer, Boolean> expression) {
        this.expression = expression;
        this.id = id;
    }

    @Override
    public Key type() {
        return CommonConditions.ENCHANTMENT;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<Item<?>> item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND);
        if (item.isEmpty()) return false;
        Optional<Enchantment> enchantment = item.get().getEnchantment(id);
        int level = enchantment.map(Enchantment::level).orElse(0);
        return this.expression.apply(level);
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            String predicate = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("predicate"), "warning.config.condition.enchantment.missing_predicate");
            String[] split = predicate.split("(<=|>=|<|>|==|=)", 2);
            int level;
            try {
                level = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                throw new LocalizedResourceConfigException("warning.config.condition.enchantment.invalid_predicate", e, predicate);
            }
            String operator = predicate.substring(split[0].length(), predicate.length() - split[1].length());
            Function<Integer, Boolean> expression;
            switch (operator) {
                case "<" -> expression = (i -> i < level);
                case ">" -> expression = (i -> i > level);
                case "==", "=" -> expression = (i -> i == level);
                case "<=" -> expression = (i -> i <= level);
                case ">=" -> expression = (i -> i >= level);
                default -> throw new LocalizedResourceConfigException("warning.config.condition.enchantment.invalid_predicate", predicate);
            }
            return new EnchantmentCondition<>(Key.of(split[0]), expression);
        }
    }
}
