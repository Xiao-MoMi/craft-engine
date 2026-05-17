package net.momirealms.craftengine.core.block.entity.tick;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;

public interface BlockEntityTicker<T extends BlockEntityController> {

    void tick(CEWorld world, BlockPos pos, ImmutableBlockState state, T controller);
}
