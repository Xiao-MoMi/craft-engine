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

package net.momirealms.craftengine.bukkit.util;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentUtils {

    private EnchantmentUtils() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Integer> toMap(Object itemEnchantments) throws ReflectiveOperationException {
        Map<String, Integer> map = new HashMap<>();
        Map<Object, Integer> enchantments = (Map<Object, Integer>) Reflections.field$ItemEnchantments$enchantments.get(itemEnchantments);

        for (Map.Entry<Object, Integer> entry : enchantments.entrySet()) {
            Object holder = entry.getKey();
            String name = (String) Reflections.method$Holder$getRegisteredName.invoke(holder);
            int level = entry.getValue();
            map.put(name, level);
        }
        return map;
    }
}
