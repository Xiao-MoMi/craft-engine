package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.VanillaBreakPowers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static boolean isEmpty(Item item) {
        return item == null || item.isEmpty();
    }

    // 返回物品的副本, null 或空物品返回新的空物品; 调用方拥有返回值, 因此不能交出共享的 EMPTY.
    @NotNull
    public static Item copyOrEmpty(@Nullable Item item) {
        return isEmpty(item) ? Item.empty() : item.copy();
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
