package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public interface BlockEntityElementConfig<E extends BlockEntityElement> {

    E create(World world, BlockPos pos);
}
