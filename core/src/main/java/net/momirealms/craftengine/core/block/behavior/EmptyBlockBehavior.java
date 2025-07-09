package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.EmptyBlock;

public final class EmptyBlockBehavior extends BlockBehavior {
    public static final EmptyBlockBehavior INSTANCE = new EmptyBlockBehavior();

    @Override
    public CustomBlock block() {
        return EmptyBlock.INSTANCE;
    }
}
