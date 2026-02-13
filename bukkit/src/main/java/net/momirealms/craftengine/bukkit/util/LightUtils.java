package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundLightUpdatePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.*;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerPlayerConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ChunkPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LightEngineProxy;
import org.bukkit.World;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

public final class LightUtils {
    private LightUtils() {}

    public static boolean hasDifferentLightProperties(Object oldState, Object newState) {
        if (VersionHelper.isOrAbove1_21_2()) {
            return LightEngineProxy.INSTANCE.hasDifferentLightProperties(oldState, newState);
        } else {
            return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightEmission(newState) != BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightEmission(oldState)
                    || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isUseShapeForLightOcclusion(newState)
                    || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isUseShapeForLightOcclusion(oldState);
        }
    }

    public static void updateChunkLight(World world, Map<Long, BitSet> sectionPosSet) {
        Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(world);
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        Object chunkMap = ServerChunkCacheProxy.INSTANCE.getChunkMap(chunkSource);
        for (Map.Entry<Long, BitSet> entry : sectionPosSet.entrySet()) {
            long chunkKey = entry.getKey();
            Object chunkHolder = ChunkMapProxy.INSTANCE.getVisibleChunkIfPresent(chunkMap, chunkKey);
            if (chunkHolder == null) continue;
            List<Object> players = ChunkHolderProxy.INSTANCE.getPlayers(chunkHolder, false);
            if (players.isEmpty()) continue;
            Object lightEngine = ChunkSourceProxy.INSTANCE.getLightEngine(chunkSource);
            Object chunkPos = ChunkPosProxy.INSTANCE.newInstance((int) chunkKey, (int) (chunkKey >> 32));
            Object lightPacket = ClientboundLightUpdatePacketProxy.INSTANCE.newInstance(chunkPos, lightEngine, entry.getValue(), entry.getValue());
            for (Object player : players) {
                ServerPlayerConnectionProxy.INSTANCE.send(
                        ServerPlayerProxy.INSTANCE.getConnection(player),
                        lightPacket
                );
            }
        }
    }
}
