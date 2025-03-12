package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.bukkit.World;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class LightUtils {

    private LightUtils() {}

    public static void updateChunkLight(World world, Map<Long, BitSet> sectionPosSet) {
        try {
            Object serverLevel = Reflections.field$CraftWorld$ServerLevel.get(world);
            Object chunkSource = Reflections.field$ServerLevel$chunkSource.get(serverLevel);
            for (Map.Entry<Long, BitSet> entry : sectionPosSet.entrySet()) {
                long chunkKey = entry.getKey();
                Object chunkHolder = Reflections.method$ServerChunkCache$getVisibleChunkIfPresent.invoke(chunkSource, chunkKey);
                if (chunkHolder == null) continue;
                @SuppressWarnings("unchecked")
                List<Object> players = (List<Object>) Reflections.method$ChunkHolder$getPlayers.invoke(chunkHolder, false);
                if (players.isEmpty()) continue;
                Object lightEngine = Reflections.field$ChunkHolder$lightEngine.get(chunkHolder);
                BitSet blockChangedLightSectionFilter = (BitSet) Reflections.field$ChunkHolder$blockChangedLightSectionFilter.get(chunkHolder);
                blockChangedLightSectionFilter.or(entry.getValue());
                BitSet skyChangedLightSectionFilter = (BitSet) Reflections.field$ChunkHolder$skyChangedLightSectionFilter.get(chunkHolder);
                Object chunkPos = Reflections.constructor$ChunkPos.newInstance((int) chunkKey, (int) (chunkKey >> 32));
                Object lightPacket = Reflections.constructor$ClientboundLightUpdatePacket.newInstance(chunkPos, lightEngine, skyChangedLightSectionFilter, blockChangedLightSectionFilter);
                Reflections.method$ChunkHolder$broadcast.invoke(chunkHolder, players, lightPacket);
                blockChangedLightSectionFilter.clear();
                skyChangedLightSectionFilter.clear();
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Could not update light for world " + world.getName());
        }
    }
}
