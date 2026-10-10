package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.VanillaBreakPowers;
import net.momirealms.craftengine.core.item.ItemSelector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
        ItemDefinition definition = item.getDefinitionOrNull();
        if (definition != null) {
            int power = definition.settings().breakPower();
            if (power >= 0) {
                return power;
            }
        }
        return VanillaBreakPowers.breakPower(item.vanillaId());
    }

    public static ItemSelector[] parseItemOrTag(List<String> selectors) {
        ItemSelector[] elements = new ItemSelector[selectors.size()];
        for (int i = 0; i < elements.length; i++) {
            String selector = selectors.get(i);
            elements[i] = selector.startsWith("#")
                    ? ItemSelector.tag(Key.of(selector.substring(1)))
                    : ItemSelector.item(Key.of(selector));
        }
        return elements;
    }

    public static boolean matchesItemOrTag(Item item, ItemSelector[] selectors) {
        ItemDefinition definition = item.getDefinitionOrNull();
        for (ItemSelector selector : selectors) {
            if (selector instanceof ItemSelector.Tag element) {
                Key tag = element.tag();
                if ((definition != null && definition.is(tag)) || item.hasVanillaTag(tag)) {
                    return true;
                }
            } else if (selector instanceof ItemSelector.Item element && element.id().equals(item.id())) {
                return true;
            }
        }
        return false;
    }
}
