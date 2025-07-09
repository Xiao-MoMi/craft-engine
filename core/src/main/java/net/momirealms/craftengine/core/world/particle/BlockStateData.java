package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.LazyReference;

public class BlockStateData implements ParticleData {
    private final LazyReference<BlockStateWrapper> blockState;

    public BlockStateData(LazyReference<BlockStateWrapper> blockState) {
        this.blockState = blockState;
    }

    public BlockStateWrapper blockState() {
        return blockState.get();
    }
}
