package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.entity.furniture.behavior.GlowingFurnitureBehaviorTemplate;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.WorldHeight;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Palette;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.chunk.client.light.LightSection;
import net.momirealms.craftengine.core.world.chunk.client.light.PackedLightStorage;
import net.momirealms.craftengine.core.world.chunk.client.light.UniformLightStorage;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.OccludingSection;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.PackedOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.UniformOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.packet.MCSection;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Arrays;
import java.util.function.Predicate;

public final class LevelChunkWithLightListener implements ByteBufferPacketListener {
    private static BiomeRemapper biomeRemapper = BiomeRemapper.DUMMY;
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final IntIdentityList biomeList;
    private final IntIdentityList blockList;
    private final boolean needsDowngrade;
    private final Predicate<Integer> occlusionPredicate;
    // 和兄弟监听器一样在构造时取快照。重载是异步的, 而光照结果会被烘焙进 ClientChunk 且此后不会重算,
    // 所以这里必须是快照: 监听器在配置解析完成后才重新注册, 重载窗口内发出的区块仍用旧值。
    private final boolean furnitureLightInUse;
    private final boolean cullingRayTracing;

    public LevelChunkWithLightListener(int[] blockStateMapper, int[] modBlockStateMapper, int blockRegistrySize, int biomeRegistrySize, Predicate<Integer> occlusionPredicate) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
        this.biomeList = new IntIdentityList(biomeRegistrySize);
        this.blockList = new IntIdentityList(blockRegistrySize);
        this.needsDowngrade = MiscUtils.ceilLog2(BlockStateUtils.vanillaBlockStateCount()) != MiscUtils.ceilLog2(blockRegistrySize);
        this.occlusionPredicate = occlusionPredicate;
        this.furnitureLightInUse = Config.enableFurnitureLightSystem() && GlowingFurnitureBehaviorTemplate.inUse();
        // 兄弟监听器(BlockUpdateListener/SectionBlocksUpdateListener)一直是构造时取快照, 这里之前是每个包实时读取
        this.cullingRayTracing = Config.entityCullingRayTracing();
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        FriendlyByteBuf buf = event.getBuffer();
        int chunkX = buf.readInt();
        int chunkZ = buf.readInt();
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);

        boolean named = !VersionHelper.isOrAbove1_20_2;

        int[] remapper = user.clientCustomBlockEnabled() ? this.modBlockStateMapper : this.blockStateMapper;

        /*
        // 标准实现: 解析高度图
        int heightmapsCount = 0;
        Map<Integer, long[]> heightmapsMap = null;
        Tag heightmaps = null;
        if (VersionHelper.isOrAbove1_21_5) {
            heightmapsMap = new HashMap<>();
            heightmapsCount = buf.readVarInt();
            for (int i = 0; i < heightmapsCount; i++) {
                int key = buf.readVarInt();
                long[] value = buf.readLongArray();
                heightmapsMap.put(key, value);
            }
        } else {
            heightmaps = buf.readNbt(named);
        }

        int chunkDataBufferSize = buf.readVarInt();
        byte[] chunkDataBytes = new byte[chunkDataBufferSize];
        buf.readBytes(chunkDataBytes);
        FriendlyByteBuf chunkDataByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(chunkDataBytes));
        */

        // 跳过高度图, 不做解析; 需要改写时原样拷贝原始字节
        int heightmapsStart = buf.readerIndex();
        Tag heightmaps = null;
        if (VersionHelper.isOrAbove1_21_5) {
            int heightmapsCount = buf.readVarInt();
            for (int i = 0; i < heightmapsCount; i++) {
                buf.readVarInt();
                buf.skipBytes(buf.readVarInt() * 8);
            }
        } else {
            // 旧版无法跳过NBT, 只能解析
            heightmaps = buf.readNbt(named);
        }
        int heightmapsLength = buf.readerIndex() - heightmapsStart;

        int chunkDataBufferSize = buf.readVarInt();
        // 切片
        FriendlyByteBuf chunkDataByteBuf = new FriendlyByteBuf(buf.readSlice(chunkDataBufferSize));

        // 客户端侧section数量很重要，不能读取此时玩家所在的真实世界，包具有滞后性
        net.momirealms.craftengine.core.world.World clientSideWorld = player.clientSideWorld();
        WorldHeight worldHeight = clientSideWorld.worldHeight();
        int count = worldHeight.getSectionsCount();
        MCSection[] sections = new MCSection[count];

        boolean hasChangedAnyBlock = false;
        boolean hasGlobalPalette = false;

        // 创建客户端侧遮挡世界, 只在开启光线追踪情况下创建.
        OccludingSection[] occludingSections = this.cullingRayTracing ? new OccludingSection[count] : null;
        // 创建客户侧光照世界, 只在家具中存在 GlowingFurnitureBehavior 行为时创建.
        LightSection[] lightSections = this.furnitureLightInUse ? new LightSection[count] : null;

        for (int i = 0; i < count; i++) {
            MCSection mcSection = new MCSection(user.clientBlockList(), this.blockList, this.biomeList);
            mcSection.readPacket(chunkDataByteBuf);

            PalettedContainer<Integer> container = mcSection.blockStateContainer();
            // 重定向生物群系
            if (biomeRemapper != BiomeRemapper.DUMMY) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                if (biomeRemapper.remap(player, chunkPos, mcSection.biomeContainer())) {
                    hasChangedAnyBlock = true;
                }
            }

            Palette<Integer> palette = container.data().palette();
            if (palette.canRemap()) {

                // 重定向方块
                if (palette.remapAndCheck(s -> remapper[s])) {
                    hasChangedAnyBlock = true;
                }

                // 处理客户端侧哪些方块有阻挡
                if (occludingSections != null) {
                    int size = palette.getSize();
                    // 单个元素的情况下，使用优化的存储方案
                    if (size == 1) {
                        occludingSections[i] = new OccludingSection(UniformOcclusionStorage.fromTest(this.occlusionPredicate.test(palette.get(0))));
                    } else {
                        boolean hasOcclusions = false;
                        boolean hasNoOcclusions = false;
                        for (int h = 0; h < size; h++) {
                            if (this.occlusionPredicate.test(palette.get(h))) {
                                hasOcclusions = true;
                            } else {
                                hasNoOcclusions = true;
                            }
                            if (hasOcclusions && hasNoOcclusions) {
                                break;
                            }
                        }
                        // 两种情况都有，那么需要一个个遍历处理视线遮挡数据
                        if (hasOcclusions && hasNoOcclusions) {
                            PackedOcclusionStorage storage = new PackedOcclusionStorage(false);
                            occludingSections[i] = new OccludingSection(storage);
                            for (int j = 0; j < 4096; j++) {
                                int state = container.get(j);
                                storage.set(j, this.occlusionPredicate.test(state));
                            }
                        }
                        // 全遮蔽或全透视则使用优化存储方案
                        else {
                            occludingSections[i] = new OccludingSection(UniformOcclusionStorage.fromTest(hasOcclusions));
                        }
                    }
                }

                // 处理客户端侧光照方块
                if (lightSections != null) {
                    int size = palette.getSize();
                    // 单个元素的情况下，使用优化的存储方案
                    if (size == 1) {
                        int result = getLightBlockType(palette.get(0));
                        lightSections[i] = new LightSection(UniformLightStorage.fromLightPredicate(result));
                    }
                    // 多元素情况, 遍历检查
                    else {
                        boolean hasReplaceable = false;
                        boolean hasSolid = false;

                        // 遍历调色盘的元素
                        for (int h = 0; h < size; h++) {
                            int result = getLightBlockType(palette.get(h));
                            if (result == 0) {
                                hasSolid = true;
                            } else {
                                hasReplaceable = true;
                            }
                            if (hasReplaceable && hasSolid) {
                                break;
                            }
                        }

                        // 如果全实心, 则使用优化存储
                        if (hasSolid && !hasReplaceable) {
                            lightSections[i] = new LightSection(UniformLightStorage.SOLID);
                            sections[i] = mcSection;
                            continue;
                        }

                        // 需要一个个遍历处理
                        PackedLightStorage storage = new PackedLightStorage();
                        lightSections[i] = new LightSection(storage);
                        for (int j = 0; j < 4096; j++) {
                            int state = container.get(j);
                            storage.set(j, getLightBlockType(state));
                        }
                    }
                }
            } else {
                hasGlobalPalette = true;

                PackedOcclusionStorage occlusionStorage = null;
                if (occludingSections != null) {
                    occlusionStorage = new PackedOcclusionStorage(false);
                    occludingSections[i] = new OccludingSection(occlusionStorage);
                }

                PackedLightStorage lightStorage = null;
                if (lightSections != null) {
                    lightStorage = new PackedLightStorage();
                    lightSections[i] = new LightSection(lightStorage);
                }

                for (int j = 0; j < 4096; j++) {
                    int state = container.get(j);

                    // 重定向方块
                    int newState = remapper[state];
                    if (newState != state) {
                        container.set(j, newState);
                        hasChangedAnyBlock = true;
                    }

                    // 写入视线遮挡数据
                    if (occlusionStorage != null) {
                        occlusionStorage.set(j, this.occlusionPredicate.test(state));
                    }

                    // 写入光照数据
                    if (lightStorage != null) {
                        lightStorage.set(j, getLightBlockType(state));
                    }
                }
            }

            sections[i] = mcSection;
        }

        /*
        // 标准实现: 解析全部尾部数据(方块实体NBT/光照)再逐个写回
        if (hasChangedAnyBlock || (this.needsDowngrade && hasGlobalPalette)) {
            // 读取其他非必要信息
            int blockEntitiesDataCount = buf.readVarInt();
            List<BlockEntityData> blockEntitiesData = new ArrayList<>();
            for (int i = 0; i < blockEntitiesDataCount; i++) {
                byte packedXZ = buf.readByte();
                short y = buf.readShort();
                int type = buf.readVarInt();
                Tag tag = buf.readNbt(named);
                BlockEntityData blockEntityData = new BlockEntityData(packedXZ, y, type, tag);
                blockEntitiesData.add(blockEntityData);
            }
            // 光照信息
            BitSet skyYMask = buf.readBitSet();
            BitSet blockYMask = buf.readBitSet();
            BitSet emptySkyYMask = buf.readBitSet();
            BitSet emptyBlockYMask = buf.readBitSet();
            List<byte[]> skyUpdates = buf.readByteArrayList(2048);
            List<byte[]> blockUpdates = buf.readByteArrayList(2048);

            // 预分配容量
            FriendlyByteBuf newChunkDataBuf = new FriendlyByteBuf(Unpooled.buffer(chunkDataBufferSize + 16));
            for (int i = 0; i < count; i++) {
                sections[i].writePacket(newChunkDataBuf);
            }
            chunkDataBytes = newChunkDataBuf.array();

            // 开始修改
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeInt(chunkX);
            buf.writeInt(chunkZ);
            if (VersionHelper.isOrAbove1_21_5) {
                buf.writeVarInt(heightmapsCount);
                for (Map.Entry<Integer, long[]> entry : heightmapsMap.entrySet()) {
                    buf.writeVarInt(entry.getKey());
                    buf.writeLongArray(entry.getValue());
                }
            } else {
                buf.writeNbt(heightmaps, named);
            }
            buf.writeVarInt(chunkDataBytes.length);
            buf.writeBytes(chunkDataBytes);
            buf.writeVarInt(blockEntitiesDataCount);
            for (BlockEntityData blockEntityData : blockEntitiesData) {
                buf.writeByte(blockEntityData.packedXZ());
                buf.writeShort(blockEntityData.y());
                buf.writeVarInt(blockEntityData.type());
                buf.writeNbt(blockEntityData.tag(), named);
            }
            buf.writeBitSet(skyYMask);
            buf.writeBitSet(blockYMask);
            buf.writeBitSet(emptySkyYMask);
            buf.writeBitSet(emptyBlockYMask);
            buf.writeByteArrayList(skyUpdates);
            buf.writeByteArrayList(blockUpdates);
        }
        */

        // 只有被修改了才改写; 高度图与尾部数据(方块实体/光照)原样透传, 不做解析
        if (hasChangedAnyBlock || (this.needsDowngrade && hasGlobalPalette)) {
            int tailLength = buf.readableBytes();
            // 高度图
            FriendlyByteBuf staging = new FriendlyByteBuf(Unpooled.buffer(heightmapsLength + chunkDataBufferSize + 16 + tailLength));
            try {
                if (VersionHelper.isOrAbove1_21_5) {
                    staging.writeBytes(buf, heightmapsStart, heightmapsLength);
                } else {
                    staging.writeNbt(heightmaps, named);
                }
                // 区块数据
                int writtenHeightmapsLength = staging.writerIndex();
                for (int i = 0; i < count; i++) {
                    sections[i].writePacket(staging);
                }
                // 其他数据
                int newChunkDataLength = staging.writerIndex() - writtenHeightmapsLength;
                staging.writeBytes(buf, tailLength);

                // 开始修改
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeInt(chunkX);
                buf.writeInt(chunkZ);
                buf.writeBytes(staging, writtenHeightmapsLength);
                buf.writeVarInt(newChunkDataLength);
                buf.writeBytes(staging, staging.readableBytes());
            } finally {
                staging.release();
            }
        }

        // 记录加载的区块
        player.addTrackedChunk(chunkKey, new ClientChunk(occludingSections, lightSections, worldHeight));

        // 生成方块实体
        CEWorld ceWorld = clientSideWorld.storageWorld();
        // 世界可能被卸载，因为包滞后
        if (ceWorld != null) {
            CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(chunkKey);
            if (ceChunk != null) {
                // 生成方块实体
                ceChunk.spawnBlockEntities(player);
            }
        }
    }

    private static int getLightBlockType(int blockStateId) {
        if (blockStateId == GlowingFurnitureBehaviorTemplate.AIR_BLOCK_STATE_ID) return 1;
        else if (blockStateId == GlowingFurnitureBehaviorTemplate.WATER_BLOCK_STATE_ID) return 2;
        else return 0;
    }

    public static void addBiomeRemapper(BiomeRemapper remapper) {
        if (biomeRemapper == BiomeRemapper.DUMMY) {
            biomeRemapper = remapper;
        } else if (biomeRemapper instanceof DualBiomeRemapper(BiomeRemapper first, BiomeRemapper second)) {
            biomeRemapper = new CompositeBiomeRemapper(new BiomeRemapper[]{first, second, remapper});
        } else if (biomeRemapper instanceof CompositeBiomeRemapper(BiomeRemapper[] remappers)) {
            BiomeRemapper[] newRemappers = Arrays.copyOf(remappers, remappers.length + 1);
            newRemappers[remappers.length] = remapper;
            biomeRemapper = new CompositeBiomeRemapper(newRemappers);
        } else {
            biomeRemapper = new DualBiomeRemapper(biomeRemapper, remapper);
        }
    }

    public static BiomeRemapper getBiomeRemapper() {
        return biomeRemapper;
    }

    public static void clearBiomeRemappers() {
        biomeRemapper = BiomeRemapper.DUMMY;
    }

    public interface BiomeRemapper {
        BiomeRemapper DUMMY = (player, pos, biomes) -> false;

        boolean remap(Player player, ChunkPos pos, PalettedContainer<Integer> biomes);
    }

    private record DualBiomeRemapper(BiomeRemapper first, BiomeRemapper second) implements BiomeRemapper {

        @Override
        public boolean remap(Player player, ChunkPos pos, PalettedContainer<Integer> biomes) {
            return this.first.remap(player, pos, biomes) || this.second.remap(player, pos, biomes);
        }
    }

    private record CompositeBiomeRemapper(BiomeRemapper[] remappers) implements BiomeRemapper {

        @Override
        public boolean remap(Player player, ChunkPos pos, PalettedContainer<Integer> biomes) {
            boolean anyChanged = false;
            for (BiomeRemapper remapper : this.remappers) {
                if (remapper.remap(player, pos, biomes)) {
                    anyChanged = true;
                }
            }
            return anyChanged;
        }
    }
}
