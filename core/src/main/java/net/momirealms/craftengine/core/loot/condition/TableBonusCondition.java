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
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TableBonusCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final Key enchantmentType;
    private final List<Float> values;

    public TableBonusCondition(Key enchantmentType, List<Float> values) {
        this.enchantmentType = enchantmentType;
        this.values = values;
    }

    @Override
    public Key type() {
        return LootConditions.TABLE_BONUS;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Optional<Item<?>> item = lootContext.getOptionalParameter(LootParameters.TOOL);
        int level = item.map(value -> value.getEnchantment(this.enchantmentType).map(Enchantment::level).orElse(0)).orElse(0);
        float f = this.values.get(Math.min(level, this.values.size() - 1));
        return lootContext.randomSource().nextFloat() < f;
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            Key enchantmentType = Key.of((String) arguments.get("enchantment"));
            List<Float> floats = MiscUtils.getAsFloatList(arguments.get("chances"));
            return new TableBonusCondition(enchantmentType, floats);
        }
    }
}
