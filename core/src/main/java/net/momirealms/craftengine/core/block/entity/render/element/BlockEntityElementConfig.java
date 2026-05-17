package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

public interface BlockEntityElementConfig<E extends ConstantBlockEntityElement> {

    E create(CEChunk chunk, BlockPos pos);

    default E create(CEChunk chunk, BlockPos pos, E previous) {
        return null;
    }

    default E createExact(CEChunk chunk, BlockPos pos, E previous) {
        return null;
    }

    Class<E> elementClass();
}
