package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class NoteBlockChainUpdateUtils {

    private NoteBlockChainUpdateUtils() {}

    public static void noteBlockChainUpdate(Object level, Object chunkSource, Object direction, Object blockPos, int times) throws ReflectiveOperationException {
        if (times >= Config.maxChainUpdate()) return;
        Object relativePos = Reflections.method$BlockPos$relative.invoke(blockPos, direction);
        Object state = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, relativePos);
        if (BlockStateUtils.isClientSideNoteBlock(state)) {
            FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, relativePos);
            noteBlockChainUpdate(level, chunkSource, direction, relativePos, times+1);
        }
    }

    public static void noteBlockChainUpdate(Object level, Object chunkSource, Direction direction, BlockPos blockPos, int times) throws ReflectiveOperationException {
        if (times >= Config.maxChainUpdate()) return;
        BlockPos relativePos = blockPos.relative(direction);
        Object nmsRelativePos = LocationUtils.toBlockPos(relativePos);
        Object state = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, nmsRelativePos);
        if (BlockStateUtils.isClientSideNoteBlock(state)) {
            FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, nmsRelativePos);
            noteBlockChainUpdate(level, chunkSource, direction, relativePos, times+1);
        }
    }

    public static void noteBlockChainUpdate(BlockPos blockPos, BlockFace direction, World world, Object chunkSource, Object serverLevel) throws ReflectiveOperationException {
        int updateAirTimes = 0;
        int whileTimes = 0;
        while (updateAirTimes < 1) {
            if (whileTimes >= Config.maxChainUpdate()) break;
            blockPos = blockPos.relative(DirectionUtils.toDirection(direction));
            Block pushBlock = world.getBlockAt(blockPos.x(), blockPos.y(), blockPos.z());
            if (pushBlock.getType() == Material.AIR) updateAirTimes++;
            Object nmsBlockPos = LocationUtils.toBlockPos(blockPos);
            FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, nmsBlockPos);
            if (direction == BlockFace.UP || direction == BlockFace.DOWN) continue;
            noteBlockChainUpdate(serverLevel, chunkSource, Direction.UP, blockPos, 0);
            noteBlockChainUpdate(serverLevel, chunkSource, Direction.DOWN, blockPos, 0);
            whileTimes++;
        }
    }
}
