package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;

public interface EntityBlock {

    BlockEntityController createBlockEntityController(BlockEntity blockEntity);

    void initControllerId(int id);
}
