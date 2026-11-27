package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.GeneratingWorld;
import net.momirealms.craftengine.core.world.WorldHeight;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;

public final class WorldGenAccess implements GeneratingWorld {
    private final Object level;
    private WorldHeight worldHeight;

    WorldGenAccess(Object level) {
        this.level = level;
    }

    @Override
    public BlockStateWrapper getBlockState(int x, int y, int z) {
        Object pos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        return BlockStateUtils.toBlockStateWrapper(BlockGetterProxy.INSTANCE.getBlockState(this.level, pos));
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockStateWrapper state, int flags) {
        Object pos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        LevelWriterProxy.INSTANCE.setBlock(this.level, pos, state.minecraftState(), flags);
    }

    @Override
    public WorldHeight worldHeight() {
        if (this.worldHeight == null) {
            this.worldHeight = WorldHeight.create(
                    LevelHeightAccessorProxy.INSTANCE.getMinY(this.level),
                    LevelHeightAccessorProxy.INSTANCE.getHeight(this.level)
            );
        }
        return this.worldHeight;
    }

    @Override
    public Key getNoiseBiome(int x, int y, int z) {
        Object holder = LevelReaderProxy.INSTANCE.getNoiseBiome(this.level, x, y, z);
        return KeyUtils.unwrapHolder(holder);
    }

    @Override
    public Object minecraftWorld() {
        return ServerLevelAccessorProxy.INSTANCE.getLevel(this.level);
    }

    @Override
    public Object generatingWorld() {
        return this.level;
    }
}
