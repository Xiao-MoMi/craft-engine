/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class EnchantmentCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final Key id;
    private final Function<Integer, Boolean> expression;

    public EnchantmentCondition(Key id, Function<Integer, Boolean> expression) {
        this.id = id;
        this.expression = expression;
    }

    @Override
    public Key type() {
        return LootConditions.ENCHANTMENT;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Optional<Item<?>> item = lootContext.getOptionalParameter(LootParameters.TOOL);
        if (item.isEmpty()) return false;
        Optional<Enchantment> enchantment = item.get().getEnchantment(id);
        int level = enchantment.map(Enchantment::level).orElse(0);
        return this.expression.apply(level);
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            String predicate = (String) arguments.get("predicate");
            String[] split = predicate.split("(<=|>=|<|>|==|=)", 2);
            int level = Integer.parseInt(split[1]);
            String operator = predicate.substring(split[0].length(), predicate.length() - split[1].length());
            Function<Integer, Boolean> expression;
            switch (operator) {
                case "<" -> expression = (i -> i < level);
                case ">" -> expression = (i -> i > level);
                case "==", "=" -> expression = (i -> i == level);
                case "<=" -> expression = (i -> i <= level);
                case ">=" -> expression = (i -> i >= level);
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            }
            return new EnchantmentCondition(Key.of(split[0]), expression);
        }
    }
}
