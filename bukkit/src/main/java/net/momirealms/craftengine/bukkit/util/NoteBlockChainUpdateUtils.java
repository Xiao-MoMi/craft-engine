package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.config.ConfigManager;

public class NoteBlockChainUpdateUtils {

    private NoteBlockChainUpdateUtils() {}

    public static void noteBlockChainUpdate(Object level, Object chunkSource, Object direction, Object blockPos, int times) throws ReflectiveOperationException {
        if (times >= ConfigManager.maxChainUpdate()) return;
        Object relativePos = Reflections.method$BlockPos$relative.invoke(blockPos, direction);
        Object state = Reflections.method$BlockGetter$getBlockState.invoke(level, relativePos);
        if (BlockStateUtils.isClientSideNoteBlock(state)) {
            Reflections.method$ServerChunkCache$blockChanged.invoke(chunkSource, relativePos);
            noteBlockChainUpdate(level, chunkSource, direction, relativePos, times+1);
        }
    }
}
