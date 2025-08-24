package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.item.behavior.EmptyItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.util.Key;

public class BukkitItemBehaviors extends ItemBehaviors {
    public static final Key BLOCK_ITEM = Key.from("craftengine:block_item");
    public static final Key ON_LIQUID_BLOCK_ITEM = Key.from("craftengine:liquid_collision_block_item");
    public static final Key FURNITURE_ITEM = Key.from("craftengine:furniture_item");
    public static final Key FLINT_AND_STEEL_ITEM = Key.from("craftengine:flint_and_steel_item");
    public static final Key COMPOSTABLE_ITEM = Key.from("craftengine:compostable_item");
    public static final Key AXE_ITEM = Key.from("craftengine:axe_item");
    public static final Key DOUBLE_HIGH_BLOCK_ITEM = Key.from("craftengine:double_high_block_item");

    public static void init() {
        register(EMPTY, EmptyItemBehavior.FACTORY);
        register(BLOCK_ITEM, BlockItemBehavior.FACTORY);
        register(ON_LIQUID_BLOCK_ITEM, LiquidCollisionBlockItemBehavior.FACTORY);
        register(FURNITURE_ITEM, FurnitureItemBehavior.FACTORY);
        register(FLINT_AND_STEEL_ITEM, FlintAndSteelItemBehavior.FACTORY);
        register(COMPOSTABLE_ITEM, CompostableItemBehavior.FACTORY);
        register(AXE_ITEM, AxeItemBehavior.FACTORY);
        register(DOUBLE_HIGH_BLOCK_ITEM, DoubleHighBlockItemBehavior.FACTORY);
    }
}
