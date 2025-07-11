package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.item.Item;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static boolean isEmpty(Item<?> item) {
        return item == null || item.isEmpty();
    }
}
