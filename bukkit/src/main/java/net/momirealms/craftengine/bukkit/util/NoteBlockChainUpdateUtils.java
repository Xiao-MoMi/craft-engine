package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.config.Config;

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
}
