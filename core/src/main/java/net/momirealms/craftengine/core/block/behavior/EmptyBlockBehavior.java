package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.EmptyBlock;

public final class EmptyBlockBehavior extends BlockBehavior {
    public static final EmptyBlockBehavior INSTANCE = new EmptyBlockBehavior(EmptyBlock.INSTANCE);

    public EmptyBlockBehavior(CustomBlock block) {
        super(block);
    }
}
