package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkSourceProxy;
import org.bukkit.World;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

public final class LightUtils {

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
                Object lightEngine = ChunkSourceProxy.INSTANCE.getLightEngine(chunkSource);
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
//
//    public static void relightChunk(BukkitServerPlayer player, ChunkPos pos) {
//        long chunkKey = pos.longKey;
//        ChunkStatus status = player.getTrackedChunk(chunkKey);
//        // 不处理未加载区块
//        if (status == null || status.relighted()) return;
//        for (ChunkPos anotherPos : pos.adjacentChunkPos()) {
//            // 要求周围区块必须都加载
//            if (player.getTrackedChunk(anotherPos.longKey) == null) {
//                return;
//            }
//        }
//        status.setRelighted(true);
//        net.momirealms.craftengine.core.world.World world = player.world();
//        Object serverLevel = world.serverWorld();
//        Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
//        Object chunkHolder = FastNMS.INSTANCE.method$ServerChunkCache$getVisibleChunkIfPresent(chunkSource, chunkKey);
//        if (chunkHolder == null) return;
//        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(world.uuid());
//        CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(chunkKey);
//        if (ceChunk == null) return;
//        CESection[] sections = ceChunk.sections();
//        BitSet bitSet = new BitSet();
//        for (int i = 0; i < sections.length; i++) {
//            if (!sections[i].statesContainer().isEmpty()) {
//                bitSet.set(i);
//            }
//        }
//        if (bitSet.isEmpty()) return;
//        try {
//            Object lightEngine = CoreReflections.field$ChunkHolder$lightEngine.get(chunkHolder);
//            Object chunkPos = FastNMS.INSTANCE.constructor$ChunkPos((int) chunkKey, (int) (chunkKey >> 32));
//            Object lightPacket = FastNMS.INSTANCE.constructor$ClientboundLightUpdatePacket(chunkPos, lightEngine, bitSet, bitSet);
//            player.sendPacket(lightPacket, false);
//        } catch (Throwable t) {
//            CraftEngine.instance().logger().warn("Could not send relight packet for " + player.name() + " at " + player.world().name() + " " + pos.x + "," + pos.z, t);
//        }
//    }
}
