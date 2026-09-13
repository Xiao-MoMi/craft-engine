package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import io.netty.buffer.PooledByteBufAllocator;
import net.momirealms.craftengine.bukkit.entity.furniture.behavior.GlowingFurnitureBehaviorTemplate;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.WorldHeight;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.chunk.client.light.LightSection;
import net.momirealms.craftengine.core.world.chunk.client.light.PackedLightStorage;
import net.momirealms.craftengine.core.world.chunk.client.light.UniformLightStorage;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.OccludingSection;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.PackedOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.UniformOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.packet.LocalPaletteSection;
import net.momirealms.craftengine.core.world.chunk.packet.PacketSection;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Arrays;
import java.util.function.IntPredicate;

public final class LevelChunkWithLightListener implements ByteBufferPacketListener {
    private static BiomeRemapper biomeRemapper = BiomeRemapper.DUMMY;
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final IntIdentityList biomeList;
    private final IntIdentityList blockList;
    private final IntPredicate occlusionPredicate;

    public LevelChunkWithLightListener(int[] blockStateMapper, int[] modBlockStateMapper, int blockRegistrySize, int biomeRegistrySize, IntPredicate occlusionPredicate) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
        this.biomeList = new IntIdentityList(biomeRegistrySize);
        this.blockList = new IntIdentityList(blockRegistrySize);
        this.occlusionPredicate = occlusionPredicate;
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
        IntIdentityList clientBlockList = user.clientBlockList();
        boolean needsBitWidthConversion = user.needsBlockStateBitWidthConversion();

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
        PacketSection[] sections = new PacketSection[count];

        boolean hasChanges = false;
        boolean hasGlobalPalette = false;

        // 创建客户端侧遮挡世界, 只在开启光线追踪情况下创建.
        OccludingSection[] occludingSections = Config.entityCullingRayTracing() ? new OccludingSection[count] : null;
        // 创建客户侧光照世界, 只在家具中存在 GlowingFurnitureBehavior 行为时创建.
        LightSection[] lightSections = Config.enableFurnitureLightSystem() ? new LightSection[count] : null;

        for (int i = 0; i < count; i++) {
            PacketSection section = PacketSection.readPacket(chunkDataByteBuf, this.blockList, clientBlockList, this.biomeList);
            sections[i] = section;
            if (section.remap(remapper)) {
                hasChanges = true;
            }

            // 重定向生物群系
            if (biomeRemapper != BiomeRemapper.DUMMY) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                PalettedContainer<Integer> biomes = section.biomeContainer();
                if (biomeRemapper.remap(player, chunkPos, biomes)) {
                    hasChanges = true;
                }
            }

            if (section instanceof LocalPaletteSection localSection) {

                // 处理客户端侧哪些方块有阻挡
                if (occludingSections != null) {
                    int size = localSection.paletteSize();
                    // 单个元素的情况下，使用优化的存储方案
                    if (size == 1) {
                        occludingSections[i] = new OccludingSection(UniformOcclusionStorage.fromTest(this.occlusionPredicate.test(localSection.sourcePaletteState(0))));
                    } else {
                        boolean hasOcclusions = false;
                        boolean hasNoOcclusions = false;
                        for (int h = 0; h < size; h++) {
                            if (this.occlusionPredicate.test(localSection.sourcePaletteState(h))) {
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
                                int state = localSection.sourceBlockState(j);
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
                    int size = localSection.paletteSize();
                    // 单个元素的情况下，使用优化的存储方案
                    if (size == 1) {
                        int result = getLightBlockType(localSection.sourcePaletteState(0));
                        lightSections[i] = new LightSection(UniformLightStorage.fromLightPredicate(result));
                    }
                    // 多元素情况, 遍历检查
                    else {
                        boolean hasReplaceable = false;
                        boolean hasSolid = false;

                        // 遍历调色盘的元素
                        for (int h = 0; h < size; h++) {
                            int result = getLightBlockType(localSection.sourcePaletteState(h));
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
                            continue;
                        }

                        // 需要一个个遍历处理
                        PackedLightStorage storage = new PackedLightStorage();
                        lightSections[i] = new LightSection(storage);
                        for (int j = 0; j < 4096; j++) {
                            int state = localSection.sourceBlockState(j);
                            storage.set(j, getLightBlockType(state));
                        }
                    }
                }
            } else {
                hasGlobalPalette = true;

                if (occludingSections != null || lightSections != null) {
                    int firstState = section.sourceBlockState(0);
                    OccludingSection occlusionSection = null;
                    if (occludingSections != null) {
                        occlusionSection = new OccludingSection(UniformOcclusionStorage.fromTest(this.occlusionPredicate.test(firstState)));
                        occludingSections[i] = occlusionSection;
                    }
                    LightSection lightSection = null;
                    if (lightSections != null) {
                        lightSection = new LightSection(UniformLightStorage.fromLightPredicate(getLightBlockType(firstState)));
                        lightSections[i] = lightSection;
                    }
                    // Uniform storage expands only when a different block type is encountered.
                    for (int j = 1; j < 4096; j++) {
                        int state = section.sourceBlockState(j);
                        if (occlusionSection != null) {
                            occlusionSection.setOccluding(j, this.occlusionPredicate.test(state));
                        }
                        if (lightSection != null) {
                            lightSection.setBlockType(j, getLightBlockType(state));
                        }
                    }
                }
            }
        }

        // 只有被修改了才改写; 高度图与尾部数据(方块实体/光照)原样透传, 不做解析
        if (hasChanges || (needsBitWidthConversion && hasGlobalPalette)) {
            int tailLength = buf.readableBytes();
            // 高度图
            FriendlyByteBuf staging = new FriendlyByteBuf(PooledByteBufAllocator.DEFAULT.buffer(heightmapsLength + chunkDataBufferSize + 16 + tailLength));
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
