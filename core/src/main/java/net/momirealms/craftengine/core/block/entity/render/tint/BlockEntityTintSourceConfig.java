package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

public interface BlockEntityTintSourceConfig<T extends BlockEntityTintSource> {

    T create(CEChunk world, BlockPos pos);
}
