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
