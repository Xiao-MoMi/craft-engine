package net.momirealms.craftengine.bukkit.entity.furniture.behavior;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorFactory;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureLightData;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowingFurnitureBehaviorTemplate extends FurnitureBehaviorTemplate {
    public static final FurnitureBehaviorFactory<GlowingFurnitureBehaviorTemplate> FACTORY = new Factory();
    public static final Key PAYLOAD_ID = Key.ce("light");
    public static final Object[] LIGHT_BLOCK_STATES = new Object[16];
    public static final Object[] WATERLOGGED_LIGHT_BLOCK_STATES = new Object[16];
    public static final int[] LIGHT_BLOCK_STATES_ID = new int[16];
    public static final int[] WATERLOGGED_LIGHT_BLOCK_STATES_ID = new int[16];
    public static final int AIR_BLOCK_STATE_ID;
    public static final int WATER_BLOCK_STATE_ID;
    public static final Map<UUID, FurnitureLightData> LIGHT_DATA = new ConcurrentHashMap<>();
    private static final int BLOCK_UPDATE_PACKET_ID = PacketIds.INSTANCE.clientboundBlockUpdatePacket();

    static {
        LIGHT_BLOCK_STATES[0] = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData("minecraft:air"));
        WATERLOGGED_LIGHT_BLOCK_STATES[0] = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData("minecraft:water"));
        for (int i = 1; i < 16; i++) {
            LIGHT_BLOCK_STATES[i] = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData("minecraft:light[level=" + i + "]"));
            LIGHT_BLOCK_STATES_ID[i] = BlockStateUtils.blockStateToId(LIGHT_BLOCK_STATES[i]);
            WATERLOGGED_LIGHT_BLOCK_STATES[i] = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData("minecraft:light[level=" + i + ",waterlogged=true]"));
            WATERLOGGED_LIGHT_BLOCK_STATES_ID[i] = BlockStateUtils.blockStateToId(WATERLOGGED_LIGHT_BLOCK_STATES[i]);
        }
        AIR_BLOCK_STATE_ID = BlockStateUtils.blockStateToId(LIGHT_BLOCK_STATES[0]);
        LIGHT_BLOCK_STATES_ID[0] = AIR_BLOCK_STATE_ID;
        WATER_BLOCK_STATE_ID = BlockStateUtils.blockStateToId(WATERLOGGED_LIGHT_BLOCK_STATES[0]);
        WATERLOGGED_LIGHT_BLOCK_STATES_ID[0] = WATER_BLOCK_STATE_ID;
    }

    @NotNull
    public final Map<String, List<LightConfig>> lightDataByVariant;
    public final List<LightConfig> defaultLightData;

    private GlowingFurnitureBehaviorTemplate(FurnitureDefinition furniture,
                                             @NotNull Map<String, List<LightConfig>> lightDataByVariant,
                                             List<LightConfig> defaultLightData
    ) {
        super(furniture);
        this.lightDataByVariant = lightDataByVariant;
        this.defaultLightData = defaultLightData;
    }

    @Override
    public FurnitureController createController(Furniture furniture) {
        return new GlowingFurnitureController(furniture, this);
    }

    public List<LightConfig> getLightDataByVariant(final String variant) {
        return this.lightDataByVariant.getOrDefault(variant, this.defaultLightData);
    }

    public static final class GlowingFurnitureController extends FurnitureController {
        private static final CustomDataType<List<LightData>> LIGHT_DATA_KEY = new CustomDataType<>();
        private final GlowingFurnitureBehaviorTemplate behavior;
        private boolean unloaded = false;
        private List<LightData> placedLights;

        public GlowingFurnitureController(Furniture furniture, GlowingFurnitureBehaviorTemplate behavior) {
            super(furniture);
            this.behavior = behavior;
        }

        @Override
        public void onVariantChange(FurnitureVariant previousVariant) {
            List<LightConfig> oldLightData = this.behavior.getLightDataByVariant(previousVariant.name());
            List<LightConfig> lightData = this.behavior.getLightDataByVariant(furniture.getCurrentVariant().name());
            if (oldLightData.isEmpty() && lightData.isEmpty()) return;
            this.removeLightBlocks(true);
            this.setLightBlocks();
        }

        @Override
        public void preRemove(@Nullable Player player) {
            this.removeLightBlocks(true);
            this.unloaded = true;
        }

        @Override
        public void onLoad() {
            this.unloaded = false;
            this.setLightBlocks();
        }

        @Override
        public void onUnload(boolean isStopping) {
            if (!this.unloaded) {
                this.removeLightBlocks(false);
                this.unloaded = true;
            }
        }

        private void setLightBlocks() {
            this.updateLightData();
            if (!this.placedLights.isEmpty()) {
                FurnitureLightData realLightData = this.getOrCreateLightData();
                for (int i = 0; i < this.placedLights.size(); i++) {
                    LightData addData = this.placedLights.get(i);
                    BlockPos blockPos = addData.blockPos;
                    int changed = realLightData.addLightData(blockPos, addData.light());
                    if (changed != -1) {
                        updateServerLightBlocks(blockPos, changed);
                    }
                }
            }
        }

        private void removeLightBlocks(boolean remove) {
            if (this.placedLights != null && !this.placedLights.isEmpty()) {
                FurnitureLightData realLightData = this.getOrCreateLightData();
                for (int i = 0; i < this.placedLights.size(); i++) {
                    LightData removeData = this.placedLights.get(i);
                    BlockPos blockPos = removeData.blockPos;
                    int changed = realLightData.removeLightData(blockPos, removeData.light());
                    if (changed != -1 && remove) {
                        updateServerLightBlocks(blockPos, changed);
                    }
                }
            }
        }

        private void updateServerLightBlocks(BlockPos blockPos, int level) {
            BlockStateWrapper blockState = super.furniture.world().getBlockState(blockPos);
            int stateId = blockState.registryId();
            if (stateId == AIR_BLOCK_STATE_ID) {
                Object targetState = LIGHT_BLOCK_STATES[level];
                super.furniture.world().setBlockState(blockPos, BlockStateUtils.toBlockStateWrapper(targetState), UpdateFlags.UPDATE_ALL);
            } else if (stateId == WATER_BLOCK_STATE_ID) {
                Object targetState = WATERLOGGED_LIGHT_BLOCK_STATES[level];
                super.furniture.world().setBlockState(blockPos, BlockStateUtils.toBlockStateWrapper(targetState), UpdateFlags.UPDATE_ALL);
            } else {
                Key blockId = blockState.ownerId();
                if (blockId.equals(BlockKeys.LIGHT)) {
                    if (level == 0) {
                        boolean waterlogged = blockState.getProperty("waterlogged");
                        if (waterlogged) {
                            super.furniture.world().setBlockState(blockPos, BlockStateUtils.toBlockStateWrapper(WATERLOGGED_LIGHT_BLOCK_STATES[0]), UpdateFlags.UPDATE_ALL);
                        } else {
                            super.furniture.world().setBlockState(blockPos, BlockStateUtils.toBlockStateWrapper(LIGHT_BLOCK_STATES[0]), UpdateFlags.UPDATE_ALL);
                        }
                    } else {
                        super.furniture.world().setBlockState(blockPos, blockState.withProperty("level", String.valueOf(level)), UpdateFlags.UPDATE_ALL);
                    }
                }
            }
        }

        private FurnitureLightData getOrCreateLightData() {
            UUID uuid = super.furniture.world().uuid();
            return LIGHT_DATA.computeIfAbsent(uuid, k -> new FurnitureLightData());
        }

        @Override
        public void onAsyncPlayerTrack(Player player, FurnitureSnapshotState snapshotState) {
            List<LightData> actualise = this.getLightDataSnapshot(snapshotState);
            if (actualise == null || actualise.isEmpty()) return;
            for (int i = 0; i < actualise.size(); i++) {
                LightData addData = actualise.get(i);
                BlockPos blockPos = addData.blockPos;
                int newLight = player.furnitureLightData().addLightData(blockPos, addData.light());
                if (newLight != -1) {
                    this.updateLightBlock(player, blockPos, newLight);
                }
            }
        }

        @Override
        public void onAsyncPlayerUntrack(Player player, FurnitureSnapshotState snapshotState) {
            List<LightData> actualise = this.getLightDataSnapshot(snapshotState);
            if (actualise == null || actualise.isEmpty()) return;
            for (int i = 0; i < actualise.size(); i++) {
                LightData removeData = actualise.get(i);
                BlockPos blockPos = removeData.blockPos;
                int newLight = player.furnitureLightData().removeLightData(blockPos, removeData.light());
                if (newLight != -1) {
                    this.updateLightBlock(player, blockPos, newLight);
                }
            }
        }

        @Nullable
        private List<LightData> getLightDataSnapshot(FurnitureSnapshotState snapshotState) {
            return snapshotState.getCustomData(LIGHT_DATA_KEY);
        }

        private void updateLightData() {
            List<LightConfig> lightData = this.behavior.lightDataByVariant.get(this.furniture.getCurrentVariant().name());
            if (lightData != null) {
                List<LightData> currentActualise = lightData.stream().map(it -> it.create(this.furniture)).toList();
                this.furniture.snapshotState().setCustomData(LIGHT_DATA_KEY, currentActualise);
                this.placedLights = currentActualise;
            } else {
                this.placedLights = List.of();
            }
        }

        private void updateLightBlock(Player player, BlockPos blockPos, int lightPower) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(9));
            buf.writeBlockPos(blockPos);
            buf.writeByte(lightPower);
            player.sendCustomPayload(PAYLOAD_ID, buf.array());
        }
    }

    private static class Factory implements FurnitureBehaviorFactory<GlowingFurnitureBehaviorTemplate> {
        @Override
        public GlowingFurnitureBehaviorTemplate create(FurnitureDefinition furniture, ConfigSection section) {
            if (!Config.enableFurnitureLightSystem()) {
                throw new IllegalStateException("'furniture.light-system.enable' is not enabled in config.yml");
            }

            ConfigSection variantsSection = section.getSection("variants");
            Map<String, List<LightConfig>> lightDataByVariant;
            if (variantsSection == null) {
                lightDataByVariant = Map.of();
            } else {
                lightDataByVariant = new HashMap<>();
                for (String variantName : variantsSection.keySet()) {
                    List<LightConfig> lightData = variantsSection.getList(variantName, this::parseLightData);
                    lightDataByVariant.put(variantName, lightData);
                }
            }
            return new GlowingFurnitureBehaviorTemplate(furniture, lightDataByVariant, section.getList("lights", this::parseLightData));
        }

        private LightConfig parseLightData(ConfigValue v) {
            if (v.is(Map.class)) {
                ConfigSection s = v.getAsSection();
                Vector3f position = s.getVector3f("position", ConfigConstants.ZERO_VECTOR3);
                int light = s.getValue("level", a -> a.getAsInt(1, 15), 15);
                return new LightConfig(position, light);
            } else {
                ConfigValue[] split = v.splitValues(" ", 2);
                if (split.length == 1) {
                    return new LightConfig(split[0].getAsVector3f(), 15);
                } else {
                    return new LightConfig(split[0].getAsVector3f(), split[1].getAsInt(1, 15));
                }
            }
        }
    }

    public record LightConfig(
            Vector3f relative,
            int light
    ) {

        LightData create(Furniture furniture) {
            return new LightData(BlockPos.fromVec3d(furniture.getRelativePosition(this.relative)), this.light);
        }
    }

    public record LightData(
            BlockPos blockPos,
            int light
    ) {}

    public static void handleLightPacket(NetWorkUser user, ByteBufPacketEvent event, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        byte lightPower = buf.readByte();

        ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(pos.x >> 4, pos.z >> 4));
        if (trackedChunk == null) return;
        int blockType = trackedChunk.lightBlockType(pos.x, pos.y, pos.z);
        if (blockType == 0) { // 不替换固体方块
            event.setCancelled(true);
            return;
        }

        // 重写入发出
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(BLOCK_UPDATE_PACKET_ID);
        buf.writeBlockPos(pos);
        if (blockType == 1) { // 替换空气
            buf.writeVarInt(LIGHT_BLOCK_STATES_ID[lightPower]);
        } else { // 替换水
            buf.writeVarInt(WATERLOGGED_LIGHT_BLOCK_STATES_ID[lightPower]);
        }

    }
}
