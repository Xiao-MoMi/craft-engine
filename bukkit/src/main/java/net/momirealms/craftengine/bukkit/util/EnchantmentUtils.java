package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.enchantment.ItemEnchantmentsProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnchantmentUtils {
    private EnchantmentUtils() {}

    public static List<Enchantment> toList(Object itemEnchantments) {
        if (itemEnchantments == null) return List.of();
        List<Enchantment> enchantmentList = new ArrayList<>();
        Map<Object, Integer> enchantments = ItemEnchantmentsProxy.INSTANCE.getEnchantments(itemEnchantments);
        for (Map.Entry<Object, Integer> entry : enchantments.entrySet()) {
            Object holder = entry.getKey();
            String name = HolderProxy.INSTANCE.getRegisteredName(holder);
            int level = entry.getValue();
            enchantmentList.add(new Enchantment(Key.of(name), level));
        }
        return enchantmentList;
    }

    public static Map<String, Integer> toMap(Object itemEnchantments) {
        if (itemEnchantments == null) return Map.of();
        Map<String, Integer> map = new HashMap<>();
        Map<Object, Integer> enchantments = ItemEnchantmentsProxy.INSTANCE.getEnchantments(itemEnchantments);
        for (Map.Entry<Object, Integer> entry : enchantments.entrySet()) {
            Object holder = entry.getKey();
            String name = HolderProxy.INSTANCE.getRegisteredName(holder);
            int level = entry.getValue();
            map.put(name, level);
        }
        return map;
    }
}
