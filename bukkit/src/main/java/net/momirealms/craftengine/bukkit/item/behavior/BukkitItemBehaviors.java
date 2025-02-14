package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.item.behavior.EmptyItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.util.Key;

public class BukkitItemBehaviors extends ItemBehaviors {
    public static final Key EMPTY = Key.from("craftengine:empty");
    public static final Key BLOCK_ITEM = Key.from("craftengine:block_item");
    public static final Key FURNITURE_ITEM = Key.from("craftengine:furniture_item");
    public static final Key AXE_ITEM = Key.from("craftengine:axe_item");

    public static void init() {
        register(EMPTY, (args, id) -> EmptyItemBehavior.INSTANCE);
        register(BLOCK_ITEM, BlockItemBehavior.FACTORY);
        register(FURNITURE_ITEM, FurnitureItemBehavior.FACTORY);
        register(AXE_ITEM, AxeItemBehavior.FACTORY);
    }
}
