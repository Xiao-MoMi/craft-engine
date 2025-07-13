package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.bukkit.World;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class LightUtils {

    private LightUtils() {}

    public static void updateChunkLight(World world, Map<Long, BitSet> sectionPosSet) {
        try {
            Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world);
            Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
            for (Map.Entry<Long, BitSet> entry : sectionPosSet.entrySet()) {
                long chunkKey = entry.getKey();
                Object chunkHolder = FastNMS.INSTANCE.method$ServerChunkCache$getVisibleChunkIfPresent(chunkSource, chunkKey);
                if (chunkHolder == null) continue;
                List<Object> players = FastNMS.INSTANCE.method$ChunkHolder$getPlayers(chunkHolder);
                if (players.isEmpty()) continue;
                Object lightEngine = CoreReflections.field$ChunkHolder$lightEngine.get(chunkHolder);
                Object chunkPos = FastNMS.INSTANCE.constructor$ChunkPos((int) chunkKey, (int) (chunkKey >> 32));
                Object lightPacket = FastNMS.INSTANCE.constructor$ClientboundLightUpdatePacket(chunkPos, lightEngine, entry.getValue(), entry.getValue());
                for (Object player : players) {
                    FastNMS.INSTANCE.method$ServerPlayerConnection$send(
                            FastNMS.INSTANCE.field$Player$connection(player),
                            lightPacket);
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Could not update light for world " + world.getName(), e);
        }
    }
}
