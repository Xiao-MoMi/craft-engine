package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.ImmutableBlockState;

public interface RandomTickBlock {

    boolean canRandomlyTick(ImmutableBlockState state);
}
