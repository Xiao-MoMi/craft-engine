package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.entity.furniture.behavior.GlowingFurnitureBehaviorTemplate;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;

import java.util.function.Predicate;

public final class BlockUpdateListener implements ByteBufferPacketListener {
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final Predicate<Integer> occlusionPredicate;
    private final boolean cullingRayTracing;
    private final boolean glowingFurniture;
    private final boolean handleClientChunk;

    public BlockUpdateListener(int[] blockStateMapper, int[] modBlockStateMapper, Predicate<Integer> occlusionPredicate) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
        this.occlusionPredicate = occlusionPredicate;
        this.cullingRayTracing = Config.entityCullingRayTracing();
        this.glowingFurniture = Config.enableFurnitureLightSystem();
        this.handleClientChunk = this.cullingRayTracing || this.glowingFurniture;
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        BlockPos pos = buf.readBlockPos();
        int before = buf.readVarInt();
        int state = user.clientModEnabled() ? modBlockStateMapper[before] : blockStateMapper[before];

        if (this.handleClientChunk) {
            ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(pos.x >> 4, pos.z >> 4));
            if (trackedChunk != null) {
                if (this.cullingRayTracing) {
                    trackedChunk.setOccluding(pos.x, pos.y, pos.z, this.occlusionPredicate.test(before));
                }
                // 记录到客户侧世界
                if (this.glowingFurniture) {
                    trackedChunk.setLightBlockType(pos.x, pos.y, pos.z, getLightBlockType(before));
                    if (before == GlowingFurnitureBehaviorTemplate.AIR_BLOCK_STATE_ID) {
                        int lightPower = ((BukkitServerPlayer) user).furnitureLightData().getLightPower(pos);
                        if (lightPower != 0) {
                            state = BlockStateUtils.blockStateToId(GlowingFurnitureBehaviorTemplate.LIGHT_BLOCK_STATES[lightPower]);
                        }
                    } else if (before == GlowingFurnitureBehaviorTemplate.WATER_BLOCK_STATE_ID) {
                        int lightPower = ((BukkitServerPlayer) user).furnitureLightData().getLightPower(pos);
                        if (lightPower != 0) {
                            state = BlockStateUtils.blockStateToId(GlowingFurnitureBehaviorTemplate.WATERLOGGED_LIGHT_BLOCK_STATES[lightPower]);
                        }
                    }
                }
            }
        }

        // 未修改则忽略
        if (state == before) {
            return;
        }
        // 如果客户端有mod，且发送的是自定义方块，则忽略
        if (user.clientModEnabled() && !BlockStateUtils.isVanillaBlock(before)) {
            return;
        }
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeBlockPos(pos);
        buf.writeVarInt(state);
    }

    private static int getLightBlockType(int blockStateId) {
        if (blockStateId == GlowingFurnitureBehaviorTemplate.AIR_BLOCK_STATE_ID) return 1;
        else if (blockStateId == GlowingFurnitureBehaviorTemplate.WATER_BLOCK_STATE_ID) return 2;
        else return 0;
    }
}
