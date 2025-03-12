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

package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;

import java.util.List;

public class EnchantmentModifier<I> implements ItemModifier<I> {
    private final List<Enchantment> enchantments;

    public EnchantmentModifier(List<Enchantment> enchantments) {
        this.enchantments = enchantments;
    }

    @Override
    public String name() {
        return "enchantment";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) item.setStoredEnchantments(enchantments);
        else item.setEnchantments(enchantments);
    }
}
