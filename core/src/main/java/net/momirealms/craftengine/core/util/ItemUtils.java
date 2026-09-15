package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.VanillaBreakPowers;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static boolean isEmpty(Item item) {
        return item == null || item.isEmpty();
    }

    public static Item emptyToNull(Item item) {
        if (item == null) return null;
        if (item.isEmpty()) return null;
        return item;
    }

    public static int breakPower(@NotNull Item item) {
        Optional<ItemDefinition> definition = item.getDefinition();
        if (definition.isPresent()) {
            int power = definition.get().settings().breakPower();
            if (power >= 0) {
                return power;
            }
        }
        return VanillaBreakPowers.breakPower(item.vanillaId());
    }
}
