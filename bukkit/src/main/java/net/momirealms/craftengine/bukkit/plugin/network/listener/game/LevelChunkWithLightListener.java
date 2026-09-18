package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.momirealms.craftengine.core.world.SectionPos;
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
import net.momirealms.craftengine.core.world.chunk.packet.GlobalPaletteSection;
import net.momirealms.craftengine.core.world.chunk.packet.LocalPaletteSection;
import net.momirealms.craftengine.core.world.chunk.packet.PacketSection;
import net.momirealms.craftengine.core.world.chunk.packet.SingleValueSection;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

public final class LevelChunkWithLightListener implements ByteBufferPacketListener {
    private static BiomeRemapper biomeRemapper = BiomeRemapper.DUMMY;
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final IntIdentityList blockList;
    private final IntPredicate occlusionPredicate;

    public LevelChunkWithLightListener(int[] blockStateMapper, int[] modBlockStateMapper, int blockRegistrySize, IntPredicate occlusionPredicate) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
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

        int[] remapper = user.clientCustomBlockEnabled() ? this.modBlockStateMapper : this.blockStateMapper;
        IntIdentityList clientBlockList = user.clientBlockList();
        IntIdentityList clientBiomeList = user.clientBiomeList();
        boolean needsBitWidthConversion = user.needsBlockStateBitWidthConversion();

        // 跳过高度图, 不做解析; 需要改写时原样拷贝原始字节
        int heightmapsStart = buf.readerIndex();
        if (VersionHelper.isOrAbove1_21_5) {
            int heightmapsCount = buf.readVarInt();
            for (int i = 0; i < heightmapsCount; i++) {
                buf.readVarInt();
                buf.skipBytes(buf.readVarInt() * 8);
            }
        } else {
            buf.skipNbt(!VersionHelper.isOrAbove1_20_2);
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

        BiomeRemapper currentBiomeRemapper = biomeRemapper;
        SectionTracker tracker = null;
        for (int i = 0; i < count; i++) {
            PacketSection section = PacketSection.readPacket(chunkDataByteBuf, this.blockList, clientBlockList, clientBiomeList);
            sections[i] = section;
            boolean scanGlobal = section instanceof GlobalPaletteSection && (occludingSections != null || lightSections != null);
            if (!scanGlobal && section.remap(remapper)) {
                hasChanges = true;
            }

            // 重定向生物群系
            if (currentBiomeRemapper != BiomeRemapper.DUMMY) {
                PalettedContainer<Integer> biomes = section.biomeContainer();
                SectionPos sectionPos = new SectionPos(chunkX, worldHeight.getSectionYFromSectionIndex(i), chunkZ);
                if (currentBiomeRemapper.remap(player, sectionPos, biomes)) {
                    section.markBiomesChanged();
                    hasChanges = true;
                }
            }

            boolean scanOcclusion = false;
            boolean scanLight = false;
            if (section instanceof SingleValueSection singleSection) {
                int state = singleSection.sourceBlockState(0);
                if (occludingSections != null) {
                    occludingSections[i] = new OccludingSection(UniformOcclusionStorage.fromTest(this.occlusionPredicate.test(state)));
                }
                if (lightSections != null) {
                    lightSections[i] = new LightSection(UniformLightStorage.fromLightPredicate(getLightBlockType(state)));
                }
            } else if (section instanceof LocalPaletteSection localSection) {
                if (occludingSections != null || lightSections != null) {
                    int firstState = localSection.sourcePaletteState(0);
                    boolean firstOcclusion = occludingSections != null && this.occlusionPredicate.test(firstState);
                    int firstLight = lightSections != null ? getLightBlockType(firstState) : 0;
                    for (int h = 1; h < localSection.paletteSize(); h++) {
                        int state = localSection.sourcePaletteState(h);
                        if (occludingSections != null && !scanOcclusion) {
                            scanOcclusion = this.occlusionPredicate.test(state) != firstOcclusion;
                        }
                        if (lightSections != null && !scanLight) {
                            scanLight = getLightBlockType(state) != firstLight;
                        }
                        if ((occludingSections == null || scanOcclusion) && (lightSections == null || scanLight)) {
                            break;
                        }
                    }
                    if (occludingSections != null && !scanOcclusion) {
                        occludingSections[i] = new OccludingSection(UniformOcclusionStorage.fromTest(firstOcclusion));
                    }
                    if (lightSections != null && !scanLight) {
                        lightSections[i] = new LightSection(UniformLightStorage.fromLightPredicate(firstLight));
                    }
                }
            } else {
                hasGlobalPalette = true;
                scanOcclusion = occludingSections != null;
                scanLight = lightSections != null;
            }

            if (scanOcclusion || scanLight) {
                if (tracker == null) tracker = new SectionTracker(this.occlusionPredicate);
                tracker.reset(scanOcclusion, scanLight, section.sourceBlockState(0));
                if (section instanceof GlobalPaletteSection globalSection) {
                    if (globalSection.remap(remapper, tracker)) hasChanges = true;
                } else {
                    section.forEachBlockState(tracker);
                }
                if (scanOcclusion) occludingSections[i] = tracker.buildOcclusionSection();
                if (scanLight) lightSections[i] = tracker.buildLightSection();
            }
        }

        // 区块加载路径的容器方块实体(shelf等)不会另行发包,必须在这里应用物品客户端侧组件
        boolean named = !VersionHelper.isOrAbove1_20_2;
        boolean blockEntityChanged = false;
        List<ParsedBlockEntity> blockEntities = null;
        int blockEntityStart = 0;
        int blockEntityLength = 0;
        if (Config.interceptItem()) {
            blockEntityStart = buf.readerIndex();
            int blockEntityCount = buf.readVarInt();
            blockEntities = new ObjectArrayList<>(blockEntityCount);
            for (int i = 0; i < blockEntityCount; i++) {
                byte packedXZ = buf.readByte();
                short y = buf.readShort();
                int typeId = buf.readVarInt();
                CompoundTag tag = (CompoundTag) buf.readNbt(named);
                if (BlockEntityDataListener.processItemsTag(player, tag)) {
                    blockEntityChanged = true;
                }
                blockEntities.add(new ParsedBlockEntity(packedXZ, y, typeId, tag));
            }
            blockEntityLength = buf.readerIndex() - blockEntityStart;
        }

        // 只有被修改了才改写; 光照数据原样透传, 不做解析
        if (hasChanges || blockEntityChanged || (needsBitWidthConversion && hasGlobalPalette)) {
            int tailLength = buf.readableBytes();
            // 高度图
            FriendlyByteBuf staging = new FriendlyByteBuf(PooledByteBufAllocator.DEFAULT.buffer(heightmapsLength + chunkDataBufferSize + 16 + tailLength));
            try {
                staging.writeBytes(buf, heightmapsStart, heightmapsLength);
                // 区块数据
                int writtenHeightmapsLength = staging.writerIndex();
                for (int i = 0; i < count; i++) {
                    sections[i].writePacket(staging);
                }
                // 其他数据
                int newChunkDataLength = staging.writerIndex() - writtenHeightmapsLength;
                if (blockEntities != null) {
                    // 未变化时按原始字节原样拷贝,避免NBT重序列化
                    if (blockEntityChanged) {
                        staging.writeVarInt(blockEntities.size());
                        for (int i = 0; i < blockEntities.size(); i++) {
                            blockEntities.get(i).write(staging, named);
                        }
                    } else {
                        staging.writeBytes(buf, blockEntityStart, blockEntityLength);
                    }
                }
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

    private record ParsedBlockEntity(byte packedXZ, short y, int typeId, CompoundTag tag) {
        void write(FriendlyByteBuf buf, boolean named) {
            buf.writeByte(this.packedXZ);
            buf.writeShort(this.y);
            buf.writeVarInt(this.typeId);
            buf.writeNbt(this.tag, named);
        }
    }

    private static final class SectionTracker implements PacketSection.BlockStateConsumer {
        private final IntPredicate occlusionPredicate;
        private boolean trackOcclusion;
        private boolean trackLight;
        private boolean firstOcclusion;
        private int firstLight;
        private PackedOcclusionStorage occlusionStorage;
        private PackedLightStorage lightStorage;
        private long occlusionWord;
        private long lightWord;

        private SectionTracker(IntPredicate occlusionPredicate) {
            this.occlusionPredicate = occlusionPredicate;
        }

        private void reset(boolean trackOcclusion, boolean trackLight, int firstState) {
            this.trackOcclusion = trackOcclusion;
            this.trackLight = trackLight;
            this.firstOcclusion = trackOcclusion && this.occlusionPredicate.test(firstState);
            this.firstLight = trackLight ? getLightBlockType(firstState) : 0;
            this.occlusionStorage = null;
            this.lightStorage = null;
            this.occlusionWord = 0;
            this.lightWord = 0;
        }

        @Override
        public void accept(int index, int state) {
            if (this.trackOcclusion) {
                boolean occluding = this.occlusionPredicate.test(state);
                if (this.occlusionStorage == null && occluding != this.firstOcclusion) {
                    this.occlusionStorage = new PackedOcclusionStorage(this.firstOcclusion);
                }
                this.occlusionWord |= (occluding ? 1L : 0L) << (index & 63);
                if ((index & 63) == 63) {
                    if (this.occlusionStorage != null) this.occlusionStorage.setPackedWord(index >>> 6, this.occlusionWord);
                    this.occlusionWord = 0;
                }
            }
            if (this.trackLight) {
                int type = getLightBlockType(state);
                if (this.lightStorage == null && type != this.firstLight) {
                    this.lightStorage = new PackedLightStorage(this.firstLight);
                }
                this.lightWord |= (long) type << ((index & 31) << 1);
                if ((index & 31) == 31) {
                    if (this.lightStorage != null) this.lightStorage.setPackedWord(index >>> 5, this.lightWord);
                    this.lightWord = 0;
                }
            }
        }

        private OccludingSection buildOcclusionSection() {
            return new OccludingSection(this.occlusionStorage != null
                    ? this.occlusionStorage : UniformOcclusionStorage.fromTest(this.firstOcclusion));
        }

        private LightSection buildLightSection() {
            return new LightSection(this.lightStorage != null
                    ? this.lightStorage : UniformLightStorage.fromLightPredicate(this.firstLight));
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

        boolean remap(Player player, SectionPos pos, PalettedContainer<Integer> biomes);
    }

    private record DualBiomeRemapper(BiomeRemapper first, BiomeRemapper second) implements BiomeRemapper {

        @Override
        public boolean remap(Player player, SectionPos pos, PalettedContainer<Integer> biomes) {
            return this.first.remap(player, pos, biomes) | this.second.remap(player, pos, biomes);
        }
    }

    private record CompositeBiomeRemapper(BiomeRemapper[] remappers) implements BiomeRemapper {

        @Override
        public boolean remap(Player player, SectionPos pos, PalettedContainer<Integer> biomes) {
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
