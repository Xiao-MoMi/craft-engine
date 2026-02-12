package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;

public interface BlockGetter {

    BlockStateWrapper getBlockState(int x, int y, int z);

    default BlockStateWrapper getBlockState(BlockPos pos) {
        return getBlockState(pos.x, pos.y, pos.z);
    }

    Key getNoiseBiome(int x, int y, int z);

    default Key getNoiseBiome(BlockPos pos) {
        return getNoiseBiome(pos.x >> 2, pos.y >> 2, pos.z >> 2);
    }
}
