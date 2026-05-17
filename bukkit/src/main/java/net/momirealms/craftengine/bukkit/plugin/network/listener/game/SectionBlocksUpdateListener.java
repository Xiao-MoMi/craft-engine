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
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.chunk.client.light.LightSection;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.OccludingSection;

import java.util.function.Predicate;

public final class SectionBlocksUpdateListener implements ByteBufferPacketListener {
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final Predicate<Integer> occlusionPredicate;
    private final boolean cullingRayTracing;
    private final boolean glowingFurniture;
    private final boolean handleClientChunk;

    public SectionBlocksUpdateListener(int[] blockStateMapper, int[] modBlockStateMapper, Predicate<Integer> occlusionPredicate) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
        this.occlusionPredicate = occlusionPredicate;
        this.cullingRayTracing = Config.entityCullingRayTracing();
        this.glowingFurniture = Config.enableFurnitureLightSystem();
        this.handleClientChunk = this.cullingRayTracing || this.glowingFurniture;
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        int[] remapper = user.clientModEnabled() ? this.modBlockStateMapper : this.blockStateMapper;
        FriendlyByteBuf buf = event.getBuffer();
        long sPos = buf.readLong();
        int blocks = buf.readVarInt();
        short[] positions = new short[blocks];
        int[] beforeStates = new int[blocks];
        int[] afterStates = new int[blocks];

        for (int i = 0; i < blocks; i++) {
            long k = buf.readVarLong();
            short posIndex = (short) ((int) (k & 4095L));
            positions[i] = posIndex;
            int beforeState = ((int) (k >>> 12));
            beforeStates[i] = beforeState;
            afterStates[i] = remapper[beforeState];
        }

        // 获取客户端侧区域
        LightSection lightSection;
        if (this.handleClientChunk) {
            SectionPos sectionPos = SectionPos.of(sPos);
            ClientChunk trackedChunk = user.getTrackedChunk(sectionPos.asChunkPos().longKey);
            if (trackedChunk != null) {
                if (this.cullingRayTracing) {
                    OccludingSection occludingSection = trackedChunk.occludingSectionById(sectionPos.y);
                    if (occludingSection != null) {
                        for (int i = 0; i < blocks; i++) {
                            BlockPos pos = SectionPos.unpackSectionRelativePos(positions[i]);
                            occludingSection.setOccluding(pos.x, pos.y, pos.z, this.occlusionPredicate.test(beforeStates[i]));
                        }
                    }
                }
                if (this.glowingFurniture) {
                    lightSection = trackedChunk.lightSectionById(sectionPos.y);
                    if (lightSection != null) {
                        for (int i = 0; i < blocks; i++) {
                            BlockPos pos = SectionPos.unpackSectionRelativePos(positions[i]);
                            int beforeState = beforeStates[i];
                            lightSection.setBlockType(pos.x, pos.y, pos.z, getLightBlockType(beforeState));
                            if (beforeState == GlowingFurnitureBehaviorTemplate.AIR_BLOCK_STATE_ID) {
                                int lightPower = ((BukkitServerPlayer) user).furnitureLightData().getLightPower(new BlockPos(sectionPos.x * 16 + pos.x, sectionPos.y * 16 + pos.y, sectionPos.z * 16 + pos.z));
                                if (lightPower != 0) {
                                    afterStates[i] = BlockStateUtils.blockStateToId(GlowingFurnitureBehaviorTemplate.LIGHT_BLOCK_STATES[lightPower]);
                                }
                            } else if (beforeState == GlowingFurnitureBehaviorTemplate.WATER_BLOCK_STATE_ID) {
                                int lightPower = ((BukkitServerPlayer) user).furnitureLightData().getLightPower(new BlockPos(sectionPos.x * 16 + pos.x, sectionPos.y * 16 + pos.y, sectionPos.z * 16 + pos.z));
                                if (lightPower != 0) {
                                    afterStates[i] = BlockStateUtils.blockStateToId(GlowingFurnitureBehaviorTemplate.WATERLOGGED_LIGHT_BLOCK_STATES[lightPower]);
                                }
                            }
                        }
                    }
                }
            }
        }

        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeLong(sPos);
        buf.writeVarInt(blocks);
        for (int i = 0; i < blocks; i++) {
            buf.writeVarLong((long) afterStates[i] << 12 | positions[i]);
        }
        event.setChanged(true);
    }

    private static int getLightBlockType(int blockStateId) {
        if (blockStateId == GlowingFurnitureBehaviorTemplate.AIR_BLOCK_STATE_ID) return 1;
        else if (blockStateId == GlowingFurnitureBehaviorTemplate.WATER_BLOCK_STATE_ID) return 2;
        else return 0;
    }
}
