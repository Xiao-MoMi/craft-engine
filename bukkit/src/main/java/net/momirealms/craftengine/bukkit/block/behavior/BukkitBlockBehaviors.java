package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.EmptyBlockBehavior;

public class BukkitBlockBehaviors extends BlockBehaviors {
    public static final Key EMPTY = Key.from("craftengine:empty");
    public static final Key BUSH_BLOCK = Key.from("craftengine:bush_block");
    public static final Key FALLING_BLOCK = Key.from("craftengine:falling_block");
    public static final Key LEAVES_BLOCK = Key.from("craftengine:leaves_block");
    public static final Key STRIPPABLE_BLOCK = Key.from("craftengine:strippable_block");
    public static final Key SAPLING_BLOCK = Key.from("craftengine:sapling_block");

    public static void init() {
        register(EMPTY, (block, args) -> EmptyBlockBehavior.INSTANCE);
        register(FALLING_BLOCK, FallingBlockBehavior.FACTORY);
        register(BUSH_BLOCK, BushBlockBehavior.FACTORY);
        register(LEAVES_BLOCK, LeavesBlockBehavior.FACTORY);
        register(STRIPPABLE_BLOCK, StrippableBlockBehavior.FACTORY);
        register(SAPLING_BLOCK, SaplingBlockBehavior.FACTORY);
    }
}
