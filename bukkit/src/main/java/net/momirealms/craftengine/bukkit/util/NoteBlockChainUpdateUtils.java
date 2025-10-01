package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;

public final class NoteBlockChainUpdateUtils {

    private NoteBlockChainUpdateUtils() {}

    // TODO 都在一个区块内，应该优化到区块内的方块getter
    public static void noteBlockChainUpdate(Object level, Object chunkSource, Object direction, Object blockPos, int times) {
        if (times-- < 0) return;
        Object relativePos = FastNMS.INSTANCE.method$BlockPos$relative(blockPos, direction);
        Object state = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, relativePos);
        if (BukkitBlockManager.CLIENT_SIDE_NOTE_BLOCKS.contains(state)) {
            FastNMS.INSTANCE.method$ServerChunkCache$blockChanged(chunkSource, relativePos);
            noteBlockChainUpdate(level, chunkSource, direction, relativePos, times);
        }
    }
}
