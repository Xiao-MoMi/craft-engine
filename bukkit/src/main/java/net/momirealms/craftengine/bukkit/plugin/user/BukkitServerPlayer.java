package net.momirealms.craftengine.bukkit.plugin.user;

import ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.block.entity.renderer.display.BukkitDestroyStageDisplayRecorder;
import net.momirealms.craftengine.bukkit.entity.BukkitLivingEntity;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.DelegatingContainer;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.gui.CraftEngineGUIHolder;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.network.handler.PlayerPacketHandler;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.bukkit.world.WorldlyContainerHolder;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.attribute.damage.DamageVisibility;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.display.DestroyStageDisplayEntity;
import net.momirealms.craftengine.core.block.entity.render.display.DestroyStageDisplayEntitySetting;
import net.momirealms.craftengine.core.block.entity.render.display.DestroyStageDisplayRecorder;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.culling.EntityCulling;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureLightData;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureHitData;
import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacketType;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ClientboundResourcePackPopPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.*;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.login.ClientboundLoginDisconnectPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerGameModeProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerCommonPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerConfigurationPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerGamePacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.JoinWorldTaskProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.ServerResourcePackConfigurationTaskProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.util.thread.BlockableEventLoopProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.AbilitiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.InventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.InventoryMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.SlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemCooldownsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockAndLightGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.RegionizedPlayerChunkLoaderProxy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class BukkitServerPlayer extends BukkitLivingEntity implements Player {
    public static final Key SELECTED_LOCALE_KEY = Key.ce("locale");
    public static final Key ENTITY_CULLING_DISTANCE_SCALE = Key.ce("entity_culling_distance_scale");
    public static final Key DISPLAY_ENTITY_VIEW_DISTANCE_SCALE = Key.ce("display_entity_view_distance_scale");
    public static final Key ENABLE_ENTITY_CULLING = Key.ce("enable_entity_culling");
    public static final Key ENABLE_FURNITURE_DEBUG = Key.ce("enable_furniture_debug");
    public static final Key DAMAGE_VISIBILITY = Key.ce("damage_visibility");
    private static final int CUSTOM_PAYLOAD_PLAY = BukkitNetworkManager.PACKET_IDS.clientboundCustomPayloadPacket$play();
    private static final int CUSTOM_PAYLOAD_CONFIG = BukkitNetworkManager.PACKET_IDS.clientboundCustomPayloadPacket$configuration();
    private final BukkitCraftEngine plugin;

    // connection state
    private final Channel channel;
    private final Set<UUID> resourcePackUUID = Collections.synchronizedSet(new HashSet<>(4));
    private final EntityCulling culling;
    private ChannelHandler connection;
    private InetAddress address;
    private String name;
    private UUID uuid;
    private int entityId;
    private PropertyMap propertyMap;
    private boolean isNameVerified;
    private boolean isUUIDVerified;
    private ConnectionState decoderState = ConnectionState.HANDSHAKING; // inbound(decode|c2s)
    private ConnectionState encoderState = ConnectionState.HANDSHAKING; // outbound(encode|s2c)
    private boolean shouldProcessFinishConfiguration = true;
    // some references
    private Reference<org.bukkit.entity.Player> bukkitPlayerRef;
    private Reference<Object> nmsPlayerRef;
    // client side dimension info
    private World clientSideWorld;    // check main hand/offhand interaction
    private int lastSuccessfulInteraction;
    // to prevent duplicated events
    private int lastInteractEntityWithMainHand;
    private int lastInteractEntityWithOffHand;
    // re-sync attribute timely to prevent some bugs
    private long lastAttributeSyncTime;
    // for breaking blocks
    private int lastSentState = -1;
    private int lastHitBlockTime;
    private BlockPos destroyPos;
    private Object destroyedState;
    private boolean isDestroyingBlock;
    private boolean isDestroyingCustomBlock;
    // client-side ItemDisplay used to visualize custom-block destroy progress when the
    // block has a destroy_stages setting; null when no such display is active
    private DestroyStageDisplayRecorder.Entry destroyStageEntry;
    private boolean swingHandAck;
    private int lastSwingHandTick;
    private float miningProgress;
    // for client visual sync
    private int resentSoundTick;
    private int resentSwingTick;
    // has fabric client mod or not
    private boolean enableClientCustomBlock = false;
    private int clientModProtocol = -1;
    private IntIdentityList blockList = new IntIdentityList(BlockStateUtils.vanillaBlockStateCount());
    // cache if player can break blocks
    private boolean clientSideCanBreak = true;
    // 血量缩放：最近一次 UpdateAttributes 包内算出的客户端可见血量上限，-1 表示未知
    private double clientSideMaxHealth = 20;
    // a cooldown for better breaking experience
    private int lastSuccessfulBreak;
    // player's game tick
    private int gameTicks;
    // Captured before NMS resets attackStrengthTicker. One primary attack may
    // synchronously produce several player_attack damage events through sweeping
    private int capturedAttackStrengthTick = Integer.MIN_VALUE;
    private float capturedAttackStrength;
    // cache interaction range here
    private int lastUpdateInteractionRangeTick;
    private double cachedInteractionRange;
    // cooldown data
    private CooldownData cooldownData;
    // tracked chunks
    private ConcurrentChainedLong2ReferenceHashTable<ClientChunk> trackedChunks;
    // entity view
    private Map<Integer, EntityPacketHandler> entityTypeView;
    // 通过指令或api设定的语言
    @Nullable
    private Locale selectedLocale;
    // 客户端选择的语言
    private Locale clientLocale;
    // 跟踪到的方块实体渲染器
    private Map<BlockPos, CullableHolder> trackedBlockEntityRenderers;
    private Map<BlockPos, CullableHolder> trackedDynamicBlockEntityRenderers;
    private Map<Integer, CullableHolder> trackedEntities;
    private Vec3d firstPersonCameraVec3;
    private Vec3d thirdPersonCameraVec3;
    // 是否启用实体剔除
    private boolean enableEntityCulling;
    // 玩家眼睛所在位置
    private Vec3d eyeLocation;
    // 是否启用家具调试
    private boolean enableFurnitureDebug;
    // 伤害数字悬浮字可见性
    private DamageVisibility damageVisibility = Config.damageIndicatorDefaultVisibility();
    // 上一次对准的家具
    private BukkitFurniture lastHitFurniture;
    // 缓存的tick
    private int lastHitFurnitureTick;
    // 控制展示实体可见距离
    private double displayEntityViewDistance;
    // 玩家使用的游戏版本
    private GameEdition gameEdition;
    // 客户端协议
    private ProtocolVersion protocolVersion = ProtocolVersion.UNKNOWN;
    // 有概率出现如下情况
    // 客户端发送了stop包，但是仍然在继续破坏且未发出start包
    // 这种情况下可能就会卡无限挖掘状态
    private int awfulBreakFixer;
    // 上一次停止挖掘包发出的时间
    private int preventBreakTick;
    // 用于辨别是否在范围挖掘
    private boolean isRangeMining;
    // 缓存的已接收的地图数据，为了防止动态物品展示框渲染器在渲染地图物品的时候重复发送地图数据导致服务器带宽消耗过大
    private Cache<Object, Boolean> receivedMapData;
    private Set<UniqueKey> obtainedItems;
    // 家具击打记录
    private FurnitureHitData furnitureHitData;
    // 缓存可见的家具光源数据
    private FurnitureLightData furnitureLightData;
    // 是否正在模拟客户端可能缺失的交互逻辑
    // 比如客户端觉得音符盒可以交互，但实际上不可，导致副手的交互包并未发出，最终导致副手的物品逻辑不执行
    private boolean isSimulatingInteraction;

    public BukkitServerPlayer(BukkitCraftEngine plugin, @Nullable Channel channel) {
        super((WeakReference<Object>) null);
        this.channel = channel;
        this.plugin = plugin;
        if (channel != null) {
            for (String name : channel.pipeline().names()) {
                ChannelHandler handler = channel.pipeline().get(name);
                if (ConnectionProxy.CLASS.isInstance(handler)) {
                    this.connection = handler;
                    break;
                }
            }
        }
        this.culling = new EntityCulling(this);
    }

    public void setPlayer(org.bukkit.entity.Player player) {
        this.bukkitPlayerRef = new WeakReference<>(player);
        this.nmsPlayerRef = new WeakReference<>(CraftEntityProxy.INSTANCE.getEntity(player));
        this.uuid = player.getUniqueId();
        this.isUUIDVerified = true;
        this.name = player.getName();
        this.entityId = player.getEntityId();
        this.isNameVerified = true;
        this.initPlayStageFields();
        byte[] bytes = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(CooldownData.COOLDOWN_KEY), PersistentDataType.BYTE_ARRAY);
        String locale = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(SELECTED_LOCALE_KEY), PersistentDataType.STRING);
        Double scale = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENTITY_CULLING_DISTANCE_SCALE), PersistentDataType.DOUBLE);
        this.displayEntityViewDistance = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(DISPLAY_ENTITY_VIEW_DISTANCE_SCALE), PersistentDataType.DOUBLE)).orElse(1d);
        this.enableEntityCulling = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENABLE_ENTITY_CULLING), PersistentDataType.BOOLEAN)).orElse(true);
        this.enableFurnitureDebug = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENABLE_FURNITURE_DEBUG), PersistentDataType.BOOLEAN)).orElse(false);
        this.damageVisibility = DamageVisibility.byName(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(DAMAGE_VISIBILITY), PersistentDataType.STRING), Config.damageIndicatorDefaultVisibility());
        this.culling.setDistanceScale(Optional.ofNullable(scale).orElse(1.0));
        this.selectedLocale = TranslationManager.parseLocale(locale);
        this.eyeLocation = getEyePos();
        try {
            this.cooldownData = CooldownData.fromBytes(bytes);
        } catch (IOException e) {
            this.cooldownData = new CooldownData();
            this.plugin.logger().warn("Failed to parse cooldown data", e);
        }
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (!ItemStackUtils.isEmpty(item)) {
                this.obtainedItems.add(UniqueKey.create(BukkitItemManager.instance().wrap(item).id()));
            }
        }
    }

    private void initPlayStageFields() {
        this.capturedAttackStrengthTick = Integer.MIN_VALUE;
        this.capturedAttackStrength = 0.0F;
        this.trackedBlockEntityRenderers = new ConcurrentHashMap<>(64);
        this.trackedDynamicBlockEntityRenderers = new ConcurrentHashMap<>(64);
        this.trackedEntities = new ConcurrentHashMap<>(64);
        this.trackedChunks = ConcurrentChainedLong2ReferenceHashTable.createWithCapacity(128, 0.5f);
        this.entityTypeView = new ConcurrentHashMap<>(128);
        this.obtainedItems = new HashSet<>(32);
        this.furnitureHitData = new FurnitureHitData();
        this.furnitureLightData = new FurnitureLightData();
        this.receivedMapData = CacheBuilder.newBuilder()
                .weakKeys()
                .expireAfterAccess(Duration.of(30, ChronoUnit.MINUTES))
                .concurrencyLevel(4)
                .build();
    }

    @Override
    public Channel nettyChannel() {
        return this.channel;
    }

    @Override
    public CraftEngine plugin() {
        return this.plugin;
    }

    @Override
    public boolean isMiningBlock() {
        return this.isDestroyingBlock;
    }

    @Override
    public boolean shouldSyncAttribute() {
        long current = gameTicks();
        if (current - this.lastAttributeSyncTime > 20) {
            this.lastAttributeSyncTime = current;
            return true;
        }
        return false;
    }

    @Override
    public boolean isFlying() {
        return platformPlayer().isFlying();
    }

    @Override
    public GameMode gameMode() {
        Enum<?> gameType = (Enum<?>) ServerPlayerGameModeProxy.INSTANCE.getGameModeForPlayer(ServerPlayerProxy.INSTANCE.getGameMode(this.minecraftPlayer()));
        return GameMode.VALUES[gameType.ordinal()];
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void setGameMode(GameMode gameMode) {
        platformPlayer().setGameMode(Objects.requireNonNull(org.bukkit.GameMode.getByValue(gameMode.id())));
    }

    @Override
    public boolean canBreak(BlockPos pos, @Nullable Object state) {
        return AdventureModeUtils.canBreak(platformPlayer().getInventory().getItemInMainHand(), new Location(platformPlayer().getWorld(), pos.x(), pos.y(), pos.z()), state);
    }

    @Override
    public boolean canPlace(BlockPos pos, @Nullable Object state) {
        return AdventureModeUtils.canPlace(platformPlayer().getInventory().getItemInMainHand(), new Location(platformPlayer().getWorld(), pos.x(), pos.y(), pos.z()), state);
    }

    @Override
    public void sendToast(Component text, Item icon, AdvancementType type) {
        this.plugin.advancementManager().sendToast(this, icon, text, type);
    }

    @Override
    public void sendActionBar(Component text) {
        Object packet = ClientboundSetActionBarTextPacketProxy.INSTANCE.newInstance(ComponentUtils.adventureToMinecraft(text));
        sendPacket(packet, false);
    }

    @Override
    public void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Object titlePacket = ClientboundSetTitleTextPacketProxy.INSTANCE.newInstance(ComponentUtils.adventureToMinecraft(title));
        Object subtitlePacket = ClientboundSetSubtitleTextPacketProxy.INSTANCE.newInstance(ComponentUtils.adventureToMinecraft(subtitle));
        Object timePacket = ClientboundSetTitlesAnimationPacketProxy.INSTANCE.newInstance(fadeIn, stay, fadeOut);
        sendPackets(List.of(titlePacket, subtitlePacket, timePacket), false);
    }

    @Override
    public void sendMessage(Component text, boolean overlay) {
        Object packet = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(ComponentUtils.adventureToMinecraft(text), overlay);
        sendPacket(packet, false);
    }

    @Override
    public void setIsSimulatingInteraction(boolean isSimulating) {
        this.isSimulatingInteraction = isSimulating;
    }

    @Override
    public boolean isSimulatingInteraction() {
        return this.isSimulatingInteraction;
    }

    @Override
    public boolean updateLastSuccessfulInteractionTick(int tick) {
        if (this.lastSuccessfulInteraction != tick) {
            this.lastSuccessfulInteraction = tick;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int lastSuccessfulInteractionTick() {
        return this.lastSuccessfulInteraction;
    }

    @Override
    public void updateLastInteractEntityTick(@NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.lastInteractEntityWithMainHand = gameTicks();
        } else {
            this.lastInteractEntityWithOffHand = gameTicks();
        }
    }

    @Override
    public boolean lastInteractEntityCheck(@NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return gameTicks() == this.lastInteractEntityWithMainHand;
        } else {
            return gameTicks() == this.lastInteractEntityWithOffHand;
        }
    }

    @Override
    public int gameTicks() {
        return this.gameTicks;
    }

    public void captureAttackStrength(float strength) {
        this.capturedAttackStrengthTick = this.gameTicks;
        this.capturedAttackStrength = Math.clamp(strength, 0.0F, 1.0F);
    }

    public float capturedAttackStrength() {
        if (this.capturedAttackStrengthTick != this.gameTicks) return 0.0F;
        return this.capturedAttackStrength;
    }

    @Override
    public boolean hasInteractionInThisTick() {
        return this.gameTicks == this.lastSuccessfulInteraction;
    }

    @Override
    public void swingHand(InteractionHand hand) {
        LivingEntityProxy.INSTANCE.swing(minecraftPlayer(), hand == InteractionHand.MAIN_HAND ? InteractionHandProxy.MAIN_HAND : InteractionHandProxy.OFF_HAND, true);
    }

    @Override
    public boolean hasPermission(String permission) {
        return platformPlayer().hasPermission(permission);
    }

    @Override
    public boolean canInstabuild() {
        Object abilities = PlayerProxy.INSTANCE.getAbilities(minecraftPlayer());
        return AbilitiesProxy.INSTANCE.isInstantBuild(abilities);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean isNameVerified() {
        return this.isNameVerified;
    }

    @Override
    public void setUnverifiedName(String name) {
        if (this.isNameVerified) return;
        this.name = name;
    }

    @Override
    public void setVerifiedName(String name) {
        if (this.isNameVerified) return;
        this.name = name;
        this.isNameVerified = true;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public boolean isUUIDVerified() {
        return this.isUUIDVerified;
    }

    @Override
    public void setUnverifiedUUID(UUID uuid) {
        if (this.isUUIDVerified) return;
        this.uuid = uuid;
    }

    @Override
    public void setVerifiedUUID(UUID uuid) {
        if (this.isUUIDVerified) return;
        this.uuid = uuid;
        this.isUUIDVerified = true;
    }

    @Override
    public PropertyMap propertyMap() {
        return this.propertyMap;
    }

    @Override
    public void setPropertyMap(PropertyMap map) {
        this.propertyMap = map;
    }

    @Override
    public void playSound(Key sound, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(platformPlayer(), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void playSound(Position pos, Key sound, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(new Location(null, pos.x(), pos.y(), pos.z()), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void giveItem(Item item, boolean spawnFakeEntity) {
        PlayerUtils.giveItem(this, item.count(), item, spawnFakeEntity);
    }

    @Override
    public void closeInventory() {
        platformPlayer().closeInventory();
    }

    @Override
    public void sendPacket(Object packet, boolean immediately) {
        this.plugin.networkManager().sendPacket(this, packet, immediately);
    }

    @Override
    public void sendPacket(Object packet, boolean immediately, Runnable sendListener) {
        this.plugin.networkManager().sendPacket(this, packet, immediately, sendListener);
    }

    @Override
    public void sendPackets(List<Object> packet, boolean immediately) {
        this.plugin.networkManager().sendPackets(this, packet, immediately);
    }

    @Override
    public void sendPackets(List<Object> packet, boolean immediately, Runnable sendListener) {
        this.plugin.networkManager().sendPackets(this, packet, immediately, sendListener);
    }

    @Override
    public void simulatePacket(Object packet) {
        this.plugin.networkManager().simulatePacket(this, packet);
    }

    @Override
    public void sendCustomPacket(ClientCustomPacket packet) {
        ClientCustomPacketType<? extends ClientCustomPacket> type = BuiltInRegistries.CLIENT_MOD_PACKET.getValue(packet.id());
        if (type == null || !type.checkPermission(this)) return;
        FriendlyByteBuf result = new FriendlyByteBuf(Unpooled.buffer());
        result.writeVarInt(this.encoderState == ConnectionState.PLAY ? CUSTOM_PAYLOAD_PLAY : CUSTOM_PAYLOAD_CONFIG);
        result.writeKey(packet.id());
        @SuppressWarnings("unchecked")
        var codec = (NetworkCodec<FriendlyByteBuf, ClientCustomPacket>) packet.codec();
        codec.encode(result, packet);
        this.channel.writeAndFlush(result);
    }

    @Override
    public void sendCustomPackets(List<? extends ClientCustomPacket> packets) {
        for (ClientCustomPacket packet : packets) {
            sendCustomPacket(packet);
        }
    }

    @Override
    public void sendByteBufPacket(ByteBuf buf, boolean immediately) {
        if (immediately) {
            this.channel.writeAndFlush(buf);
        } else {
            this.channel.write(buf);
        }
    }

    @Override
    public void kick(@Nullable Component message) {
        Object reason = message != null ? ComponentUtils.adventureToMinecraft(message) : null;
        if (this.encoderState == ConnectionState.HANDSHAKING || this.encoderState == ConnectionState.STATUS) {
            ConnectionProxy.INSTANCE.disconnect(this.connection(), reason);
            return;
        }
        if (this.encoderState == ConnectionState.LOGIN) {
            this.sendPacket(ClientboundLoginDisconnectPacketProxy.INSTANCE.newInstance(reason), false);
            ConnectionProxy.INSTANCE.disconnect(this.connection(), reason);
            return;
        }
        Object kickPacket = ClientboundDisconnectPacketProxy.INSTANCE.newInstance(reason);
        this.sendPacket(kickPacket, false, () -> ConnectionProxy.INSTANCE.disconnect(this.connection(), reason));
        this.channel.config().setAutoRead(false);
        Runnable handleDisconnection = () -> ConnectionProxy.INSTANCE.handleDisconnection(this.connection());
        if (VersionHelper.hasFoliaPatch) {
            this.plugin.scheduler().platform().run(handleDisconnection, null, platformPlayer());
        } else {
            BlockableEventLoopProxy.INSTANCE.scheduleOnMain(MinecraftServerProxy.INSTANCE.getServer(), handleDisconnection);
        }
    }

    @Override
    public ConnectionState decoderState() {
        return this.decoderState;
    }

    @Override
    public ConnectionState encoderState() {
        return this.encoderState;
    }

    @Override
    public World clientSideWorld() {
        return this.clientSideWorld;
    }

    @Override
    public void setClientSideWorld(World world) {
        this.clientSideWorld = world;
    }

    public double clientSideMaxHealth() {
        return this.clientSideMaxHealth;
    }

    public void setClientSideMaxHealth(double maxHealth) {
        this.clientSideMaxHealth = maxHealth;
    }

    public void setConnectionState(ConnectionState connectionState) {
        this.encoderState = connectionState;
        this.decoderState = connectionState;
    }

    public void setDecoderState(ConnectionState decoderState) {
        this.decoderState = decoderState;
    }

    public void setEncoderState(ConnectionState encoderState) {
        this.encoderState = encoderState;
    }

    @Override
    public void resendChunks() {
        if (!VersionHelper.hasPaperPatch) return;
        Object chunkLoader = ServerPlayerProxy.INSTANCE.getChunkLoader(minecraftPlayer());
        LongOpenHashSet sentChunks = RegionizedPlayerChunkLoaderProxy.PlayerChunkLoaderDataProxy.INSTANCE.getSentChunks(chunkLoader);
        if (sentChunks.isEmpty()) {
            return;
        }
        sentChunks = sentChunks.clone();
        Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(platformPlayer().getWorld());
        Object lightEngine = BlockAndLightGetterProxy.INSTANCE.getLightEngine(serverLevel);
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        for (long chunkPos : sentChunks) {
            int chunkX = (int) chunkPos;
            int chunkZ = (int) (chunkPos >> 32);
            Object levelChunk = ChunkSourceProxy.INSTANCE.getChunk(chunkSource, chunkX, chunkZ, false);
            Object packet = ClientboundLevelChunkWithLightPacketProxy.INSTANCE.newInstance(levelChunk, lightEngine, null, null);
            sendPacket(packet, true);
        }
    }

    public void tick() {
        // 还没上线或是已经离线
        Object serverPlayer = minecraftPlayer();
        if (serverPlayer == null) return;

        // 更新玩家游戏刻
        this.gameTicks = ServerPlayerGameModeProxy.INSTANCE.getGameTicks(ServerPlayerProxy.INSTANCE.getGameMode(serverPlayer));

        // 更新CE UI
        if (this.gameTicks % 20 == 0) {
            this.updateGUI(serverPlayer);
        }

        // 家具调试模式
        if (this.enableFurnitureDebug) {
            BukkitFurniture furniture = CraftEngineFurniture.rayTrace(platformPlayer());
            boolean forceShow = furniture != this.lastHitFurniture;
            if (forceShow) {
                this.lastHitFurnitureTick = 0;
            } else {
                this.lastHitFurnitureTick++;
                if (this.lastHitFurnitureTick % 30 == 0) {
                    forceShow = true;
                }
            }
            this.lastHitFurniture = furniture;
            if (furniture != null && forceShow) {
                FurnitureVariant currentVariant = furniture.currentVariant();
                List<AABB> aabbs = new ArrayList<>();
                for (FurnitureHitBoxConfig<?> config : currentVariant.hitBoxConfigs()) {
                    config.prepareBoundingBox(furniture.position(), aabbs::add, true);
                }
                Key endRod = Key.of("soul_fire_flame");
                for (AABB aabb : aabbs) {
                    for (Vec3d point : aabb.getEdgePoints(0.125)) {
                        this.playParticle(endRod, point.x, point.y, point.z);
                    }
                }
            }
        }

        // 更新眼睛位置
        {
            this.eyeLocation = getEyePos(serverPlayer);
        }

        // 本tick内有挥手
        if (hasSwingHand()) {
            if (this.gameTicks - this.preventBreakTick >= 3) {
                // 连续挥手且没被重置
                // 原版有的，方块挖掘间隔。除非是秒破，否则必走此延迟
                if (this.gameTicks - this.lastSuccessfulBreak > 5) {
                    if (this.isDestroyingBlock) {
                        this.tickBlockDestroy(serverPlayer);
                    } else {
                        // 连续挥手且没被重置
                        if (++this.awfulBreakFixer >= 4) {
                            this.awfulBreakFixer = 0;
                            Object hitResult = rayTrace(serverPlayer, getCachedInteractionRange());
                            if (!BlockHitResultProxy.INSTANCE.isMiss(hitResult)) {
                                Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(hitResult);
                                BlockPos hitPos = LocationUtils.fromBlockPos(blockPos);
                                Object blockState = BlockGetterProxy.INSTANCE.getBlockState(EntityProxy.INSTANCE.getLevel(serverPlayer), blockPos);
                                ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
                                this.startMiningBlock(hitPos, blockState, customState);
                            }
                        }
                    }
                }
            }
            this.swingHandAck = false;
        }

        // 实体剔除更新相机位置
        if (Config.enableEntityCulling()) {
            this.firstPersonCameraVec3 = this.eyeLocation;
            int distance = 4;
            if (VersionHelper.isOrAbove1_21_6) {
                Object vehicle = EntityProxy.INSTANCE.getVehicle(serverPlayer);
                if (vehicle != null && EntityProxy.INSTANCE.getType(vehicle) == EntityTypesProxy.HAPPY_GHAST) {
                    distance = 8;
                }
            }

            float rotX = EntityProxy.INSTANCE.getXRot(serverPlayer);
            float rotY = EntityProxy.INSTANCE.getYRot(serverPlayer);
            float y = -MiscUtils.sin(MiscUtils.toRadians(rotY));
            float xz = MiscUtils.cos(MiscUtils.toRadians(rotY));
            float x = -xz * MiscUtils.sin(MiscUtils.toRadians(rotX));
            float z = xz * MiscUtils.cos(MiscUtils.toRadians(rotX));
            this.thirdPersonCameraVec3 = this.eyeLocation.subtract(x * distance, y * distance, z * distance);
        }
    }

    @Override
    public void entityCullingTick() {
        this.culling.restoreTokenOnTick();
        if (this.firstPersonCameraVec3 == null || this.thirdPersonCameraVec3 == null) {
            return;
        }
        boolean useRayTracing = Config.entityCullingRayTracing();
        if (this.enableEntityCulling) {
            for (CullableHolder cullableObject : this.trackedBlockEntityRenderers.values()) {
                cullEntity(useRayTracing, cullableObject);
            }
            for (CullableHolder cullableObject : this.trackedDynamicBlockEntityRenderers.values()) {
                cullEntity(useRayTracing, cullableObject);
            }
            for (CullableHolder cullableObject : this.trackedEntities.values()) {
                cullEntity(useRayTracing, cullableObject);
            }
        } else {
            for (CullableHolder cullableObject : this.trackedBlockEntityRenderers.values()) {
                cullableObject.setShown(this, true);
            }
            for (CullableHolder cullableObject : this.trackedDynamicBlockEntityRenderers.values()) {
                cullableObject.setShown(this, true);
            }
            for (CullableHolder cullableObject : this.trackedEntities.values()) {
                cullableObject.setShown(this, true);
            }
        }
    }

    private void cullEntity(boolean useRayTracing, CullableHolder cullableObject) {
        if (cullableObject.forceVisible) {
            cullableObject.setShown(this, true);
            return;
        }
        CullingData cullingData = cullableObject.cullable.cullingData();
        if (cullingData == null) {
            cullableObject.setShown(this, true);
            return;
        }
        boolean firstPersonVisible = this.culling.isVisible(cullingData, this.firstPersonCameraVec3, useRayTracing);
        // 之前可见
        if (cullableObject.isShown) {
            // 第一人称可见时结果已与第三人称无关
            if (!firstPersonVisible && !this.culling.isVisible(cullingData, this.thirdPersonCameraVec3, useRayTracing)) {
                cullableObject.setShown(this, false);
            }
        }
        // 之前不可见
        else {
            // 但是第一人称可见了
            if (firstPersonVisible) {
                // 下次再说
                if (Config.enableEntityCullingRateLimiting() && !this.culling.takeToken()) {
                    return;
                }
                cullableObject.setShown(this, true);
                return;
            }
            if (this.culling.isVisible(cullingData, this.thirdPersonCameraVec3, useRayTracing)) {
                // 下次再说
                if (Config.enableEntityCullingRateLimiting() && !this.culling.takeToken()) {
                    return;
                }
                cullableObject.setShown(this, true);
            }
            // 仍然不可见
        }
    }

    private void updateGUI(Object serverPlayer) {
        Object containerMenu = PlayerProxy.INSTANCE.getContainerMenu(serverPlayer);
        if (containerMenu == PlayerProxy.INSTANCE.getInventoryMenu(serverPlayer)) return;

        List<Object> slots = AbstractContainerMenuProxy.INSTANCE.getSlots(containerMenu);
        if (slots.isEmpty()) return;
        Object container = SlotProxy.INSTANCE.getContainer(slots.getFirst());
        if (!(container instanceof DelegatingContainer delegatingContainer)) return;
        if (!(delegatingContainer.getContainer() instanceof BukkitContainer bukkitContainer)) return;

        InventoryHolder holder = bukkitContainer.getOwner();
        if (holder instanceof CraftEngineGUIHolder guiHolder) {
            guiHolder.gui().onTimer();
        }
        if (holder instanceof WorldlyContainerHolder itemStorage) {
            WorldPosition pos = itemStorage.pos();
            if (!canInteractPoint(pos.toVec3d(), 4d)) {
                ServerPlayerProxy.INSTANCE.closeContainer(serverPlayer);
            }
        }
    }

    public boolean canInteractWithBlock(BlockPos pos, double distance) {
        double d = this.getCachedInteractionRange() + distance;
        return (new AABB(pos)).distanceToSqr(this.eyeLocation) < d * d;
    }

    @Override
    public boolean canInteractPoint(Vec3d pos, double distance) {
        double d = this.getCachedInteractionRange() + distance;
        return Vec3d.distanceToSqr(this.eyeLocation, pos) < d * d;
    }

    @Override
    public float getDestroyProgress(Object blockState, BlockPos pos) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        float progress = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getDestroyProgress(blockState, minecraftPlayer(), CraftWorldProxy.INSTANCE.getWorld(platformPlayer().getWorld()), LocationUtils.toBlockPos(pos));
        if (optionalCustomState.isPresent()) {
            ImmutableBlockState customState = optionalCustomState.get();
            Item tool = getItemInHand(InteractionHand.MAIN_HAND);
            // 如果自定义方块在服务端侧未使用正确的工具，那么需要还原挖掘速度
            if (!BlockStateUtils.isCorrectTool(customState, tool)) {
                progress *= customState.settings().incorrectToolSpeed();
            }
        }
        return progress;
    }

    public void startMiningBlock(BlockPos pos, @NotNull Object state, @Nullable ImmutableBlockState customState) {
        boolean custom = customState != null;
        boolean canInstantBreak = this.getDestroyProgress(state, pos) >= 1f;

        this.miningProgress = 0;
        this.awfulBreakFixer = 0;
        // 准备开始破坏方块
        if (canInstantBreak) {
            this.destroyPos = null;
            this.destroyedState = null;
            this.isDestroyingBlock = false;
            this.isDestroyingCustomBlock = false;
        } else {
            BlockPos previousPos = this.destroyPos;
            if (previousPos != null && !pos.equals(previousPos)) {
                this.broadcastDestroyProgress(previousPos, -1f);
            }
            this.destroyPos = pos;
            this.destroyedState = state;
            this.isDestroyingBlock = true;
            this.isDestroyingCustomBlock = custom;
        }

        if (custom) {
            // 如果是自定义方块且秒破
            if (canInstantBreak) {
                BlockStateWrapper vanillaBlockState = customState.visualBlockState();
                // 通常不会null
                if (vanillaBlockState != null) {
                    // 如果客户端觉得这个方块不能秒破，那么应该给客户端补发一个level event以正确渲染声音和粒子
                    // 客户端觉得不能秒破有两种情况：
                    // 1. 此时客户端觉得自己挖掘速度为0
                    boolean attributeCannotBreak = VersionHelper.isOrAbove1_20_5 && !this.clientSideCanBreak;
                    // 2. 客户端侧的方块就是不能秒破
                    if (attributeCannotBreak || getDestroyProgress(vanillaBlockState.minecraftState(), pos) < 1f) {
                        Object levelEventPacket = ClientboundLevelEventPacketProxy.INSTANCE.newInstance(
                                WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
                        sendPacket(levelEventPacket, false);
                    }
                }
            }
        } else {
            // 客户端此时觉得自己不能秒破，但实际可以秒破
            if (canInstantBreak && VersionHelper.isOrAbove1_20_5 && !this.clientSideCanBreak) {
                Object levelEventPacket = ClientboundLevelEventPacketProxy.INSTANCE.newInstance(
                        WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
                sendPacket(levelEventPacket, false);
            }
        }

        // 对于原版方块不影响属性，对于自定义方块需要影响
        setClientSideCanBreakBlock(!custom || canInstantBreak);
    }

    @Override
    public void setClientSideCanBreakBlock(boolean canBreak) {
        // 超过1秒就强制同步一次属性
        if (this.clientSideCanBreak == canBreak && !shouldSyncAttribute()) {
            return;
        }
        this.clientSideCanBreak = canBreak;
        if (VersionHelper.isOrAbove1_20_5) {
            Object serverPlayer = minecraftPlayer();
            Object attributeInstance = LivingEntityProxy.INSTANCE.getAttribute(serverPlayer, AttributesProxy.BLOCK_BREAK_SPEED);
            sendPacket(ClientboundUpdateAttributesPacketProxy.INSTANCE.newInstance$0(entityId(), Lists.newArrayList(attributeInstance)), true);
        } else {
            if (canBreak) {
                resetEffect(MobEffectsProxy.MINING_FATIGUE);
                resetEffect(MobEffectsProxy.HASTE);
            } else {
                Object fatiguePacket = MobEffectUtils.createPacket(MobEffectsProxy.MINING_FATIGUE, entityId(), (byte) 9, -1, false, false, false);
                Object hastePacket = MobEffectUtils.createPacket(MobEffectsProxy.HASTE, entityId(), (byte) 0, -1, false, false, false);
                sendPackets(List.of(fatiguePacket, hastePacket), true);
            }
        }
    }

    // 客户端完成破坏方块
    @Override
    public void finishMiningBlock() {
        clearActiveDestroyProgress(false);
        this.miningProgress = 0f;
        this.isDestroyingBlock = false;
        this.swingHandAck = false;
        this.destroyedState = null;
        this.destroyPos = null;
        this.isDestroyingCustomBlock = false;
        this.awfulBreakFixer = 0;
    }

    // 通过丢弃物品/右键方块/右键实体触发，会给几tick的挖掘冷却期
    @Override
    public void stopMiningBlock() {
        clearActiveDestroyProgress(false);
        this.miningProgress = 0f;
        this.isDestroyingBlock = false;
        this.swingHandAck = false;
        this.destroyedState = null;
        this.destroyPos = null;
        this.isDestroyingCustomBlock = false;
        this.awfulBreakFixer = 0;
        this.preventBreakTick = gameTicks();
    }

    // 客户端放弃破坏方块
    // 首次放弃的时候，只重置进度
    // 因为可以断断续续挖
    @Override
    public void abortMiningBlock() {
        this.swingHandAck = false;
        this.miningProgress = 0;
        this.awfulBreakFixer = 0;
        BlockPos pos = this.destroyPos;
        if (pos != null && this.isDestroyingCustomBlock) {
            // 只纠正自定义方块的破坏进度
            this.broadcastDestroyProgress(pos, -1f);
        }
    }

    @Override
    public void preventMiningBlock() {
        this.setClientSideCanBreakBlock(false);
        this.abortMiningBlock();
        this.isDestroyingBlock = false;
        this.destroyedState = null;
        this.destroyPos = null;
        this.isDestroyingCustomBlock = false;
    }

    @Override
    public boolean clientSideCanBreak() {
        return this.clientSideCanBreak;
    }

    private void resetEffect(Object mobEffect) {
        Object effectInstance = ServerPlayerProxy.INSTANCE.getEffect$legacy(minecraftPlayer(), mobEffect);
        Object packet;
        if (effectInstance != null) {
            packet = ClientboundUpdateMobEffectPacketProxy.INSTANCE.newInstance(entityId(), effectInstance);
        } else {
            packet = ClientboundRemoveMobEffectPacketProxy.INSTANCE.newInstance$legacy(entityId(), mobEffect);
        }
        sendPacket(packet, true);
    }

    private void tickBlockDestroy(Object serverPlayer) {
        int currentTick = gameTicks();
        Object destroyedState = this.destroyedState;
        if (destroyedState == null) return;

        // 进行实现追踪找到指向的方块
        Object hitResult = rayTrace(serverPlayer, getCachedInteractionRange());
        if (BlockHitResultProxy.INSTANCE.isMiss(hitResult)) return;
        Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(hitResult);
        BlockPos hitPos = LocationUtils.fromBlockPos(blockPos);
        // 如果命中点位和网络包设置的不同，那么不继续tick
        if (!hitPos.equals(this.destroyPos)) {
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(EntityProxy.INSTANCE.getLevel(serverPlayer), blockPos);
            ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
            this.startMiningBlock(hitPos, blockState, customState);
            return;
        }

        // check item in hand
        BukkitItem item = this.getItemInHand(InteractionHand.MAIN_HAND);

        // 发送破坏中音效
        if (currentTick - this.lastHitBlockTime > 3) {
            // 手上物品不是debug棒
            if (!BukkitItemUtils.isDebugStick(item) && !canInstabuild()) {
                Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(destroyedState);
                Object soundEvent = SoundTypeProxy.INSTANCE.getHitSound(soundType);
                Object soundPacket = ClientboundSoundPacketProxy.INSTANCE.newInstance(
                        HolderProxy.INSTANCE.direct(soundEvent),
                        SoundSourceProxy.BLOCKS,
                        hitPos.x(), hitPos.y(), hitPos.z(),
                        0.5F, 0.5F,
                        RandomUtils.generateRandomLong()
                );
                sendPacket(soundPacket, true);
            }
            this.lastHitBlockTime = currentTick;
        }

        // accumulate progress (custom blocks only)
        if (this.isDestroyingCustomBlock) {
            // prevent server from taking over breaking custom blocks
            Object gameMode = ServerPlayerProxy.INSTANCE.getGameMode(serverPlayer);
            ServerPlayerGameModeProxy.INSTANCE.setIsDestroyingBlock(gameMode, false);
            if (!item.isEmpty()) {
                // creative mode + invalid item in hand
                if (canInstabuild() && !ItemStackUtils.canBreakBlockInCreativeMode(item)) {
                    return;
                }
            }

            float progressToAdd = getDestroyProgress(destroyedState, hitPos);
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(destroyedState);
            // double check custom block
            if (optionalCustomState.isPresent()) {
                ImmutableBlockState customState = optionalCustomState.get();
                // accumulate progress
                this.miningProgress = progressToAdd + miningProgress;
                // broadcast changes; dedup (vanilla 0~10 stage or custom item index) happens inside
                broadcastDestroyProgress(hitPos, this.miningProgress);

                // can break now
                if (this.miningProgress >= 1f) {
                    boolean breakResult = false;
                    // for simplified adventure break, switch mayBuild temporarily
                    if (isAdventureMode() && Config.simplifyAdventureBreakCheck()) {
                        // check the appearance state
                        if (canBreak(hitPos, customState.visualBlockState().minecraftState())) {
                            // Error might occur so we use try here
                            Object abilities = PlayerProxy.INSTANCE.getAbilities(serverPlayer);
                            try {
                                AbilitiesProxy.INSTANCE.setMayBuild(abilities, true);
                                breakResult = ServerPlayerGameModeProxy.INSTANCE.destroyBlock(gameMode, blockPos);
                            } finally {
                                AbilitiesProxy.INSTANCE.setMayBuild(abilities, false);
                            }
                        }
                    } else {
                        // normal break check
                        breakResult = ServerPlayerGameModeProxy.INSTANCE.destroyBlock(gameMode, blockPos);
                    }
                    // send break particle + (removed sounds)
                    if (breakResult) {
                        sendPacket(ClientboundLevelEventPacketProxy.INSTANCE.newInstance(WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId(), false), false);
                        clearActiveDestroyProgress(true);
                        this.destroyPos = null;
                        this.miningProgress = 0;
                        this.isDestroyingBlock = false;
                        this.swingHandAck = false;
                        this.destroyedState = null;
                        this.isDestroyingCustomBlock = false;
                    } else {
                        // 事件被取消了，重置挖掘进度
                        clearDestroyStageDisplay();
                        this.miningProgress = 0;
                        this.isDestroyingCustomBlock = true;
                        this.isDestroyingBlock = true;
                    }
                }
            }
        }
    }

    public void updateLastSuccessBreakTick() {
        this.lastSuccessfulBreak = this.gameTicks;
    }

    @Override
    public void breakBlock(int x, int y, int z) {
        platformPlayer().breakBlock(new Location(platformPlayer().getWorld(), x, y, z).getBlock());
    }

    private void broadcastDestroyProgress(BlockPos hitPos, float progress) {
        DestroyStageDisplayEntitySetting display = currentDestroyStageDisplay();
        int key = progress < 0 ? -1 : (display != null ? display.itemIndexForProgress(progress) : (int) (progress * 10.0F));
        if (key == this.lastSentState) {
            return;
        }
        this.lastSentState = key;
        if (display != null) {
            broadcastDestroyProgressViaDisplay(hitPos, progress, display);
        } else {
            broadcastDestroyProgressVanilla(hitPos, key);
        }
    }

    private DestroyStageDisplayEntitySetting currentDestroyStageDisplay() {
        Object state = this.destroyedState;
        if (state == null) return null;
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null) return null;
        return customState.settings().destroyStageDisplay();
    }

    private void broadcastDestroyProgressVanilla(BlockPos hitPos, int stage) {
        Object packet = ClientboundBlockDestructionPacketProxy.INSTANCE.newInstance(Integer.MAX_VALUE - entityId(), LocationUtils.toBlockPos(hitPos), stage);
        sendPacket(packet, false);
        for (Player other : getTrackedBy()) {
            if (!isWithinDestroyRange(hitPos, other)) continue;
            other.sendPacket(packet, false);
        }
    }

    /**
     * Clears the destruction overlay before the tracked position/state is discarded.
     * Vanilla crack overlays are keyed by breaker id and survive a block replacement;
     * without an explicit reset, a newly placed block at the same position inherits
     * the final crack stage until another update happens.
     */
    private void clearActiveDestroyProgress(boolean clearAllDisplayMiners) {
        BlockPos position = this.destroyPos;
        if (position == null || !this.isDestroyingCustomBlock) {
            clearDestroyStageDisplay();
            this.lastSentState = -1;
            return;
        }

        if (currentDestroyStageDisplay() != null) {
            if (clearAllDisplayMiners) {
                clearDestroyStageDisplayForAll();
            } else {
                clearDestroyStageDisplay();
            }
        } else {
            broadcastDestroyProgressVanilla(position, -1);
        }
        this.lastSentState = -1;
    }

    public void broadcastDestroyProgressViaDisplay(BlockPos hitPos, float progress, DestroyStageDisplayEntitySetting display) {
        if (progress < 0) {
            clearDestroyStageDisplay();
            return;
        }
        DestroyStageDisplayRecorder.PosKey posKey = new DestroyStageDisplayRecorder.PosKey(platformPlayer().getWorld().getUID(), hitPos.asLong());
        DestroyStageDisplayRecorder.Entry entry = BukkitDestroyStageDisplayRecorder.INSTANCE.getOrCreate(posKey, display, hitPos);
        this.destroyStageEntry = entry;

        entry.minerProgress.put(this.uuid, progress);
        float maxProgress = 0f;
        for (Float p : entry.minerProgress.values()) {
            if (p != null && p > maxProgress) maxProgress = p;
        }
        int newIndex = display.itemIndexForProgress(maxProgress);
        if (newIndex == entry.displayedIndex()) {
            return;
        }
        entry.displayedIndex(newIndex);

        DestroyStageDisplayEntity entity = entry.entity;
        Set<UUID> current = new HashSet<>();
        current.add(this.uuid);
        Set<Player> trackedPlayers = getTrackedBy();
        for (Player other : trackedPlayers) {
            if (isWithinDestroyRange(hitPos, other)) {
                current.add(other.uuid());
            }
        }
        Object removePacket = entity.removePacket();
        entity.removeLeftViewers(current, viewerId -> {
            if (viewerId.equals(this.uuid)) return;
            BukkitServerPlayer serverPlayer = BukkitNetworkManager.instance().getOnlineUser(viewerId);
            if (serverPlayer == null) return;
            serverPlayer.sendPacket(removePacket, false);
        });
        showDestroyStageDisplayTo(this, entity, newIndex);
        for (Player other : trackedPlayers) {
            if (!isWithinDestroyRange(hitPos, other)) continue;
            showDestroyStageDisplayTo(other, entity, newIndex);
        }
    }

    private void showDestroyStageDisplayTo(Player viewer, DestroyStageDisplayEntity entity, int index) {
        entity.ensureSpawned(viewer, index, packet -> viewer.sendPacket(packet, false));
    }

    public void clearDestroyStageDisplay() {
        DestroyStageDisplayRecorder.Entry entry = this.destroyStageEntry;
        if (entry == null) return;
        DestroyStageDisplayEntity entity = entry.entity;
        this.destroyStageEntry = null;
        this.lastSentState = -1;
        entry.minerProgress.remove(this.uuid);
        if (entry.minerProgress.isEmpty()) {
            BukkitDestroyStageDisplayRecorder.INSTANCE.remove(entry.key);
        } else {
            float maxProgress = 0f;
            for (Float p : entry.minerProgress.values()) {
                if (p != null && p > maxProgress) maxProgress = p;
            }
            int newIndex = entry.display.itemIndexForProgress(maxProgress);
            if (newIndex != entry.displayedIndex()) {
                entry.displayedIndex(newIndex);
                for (UUID viewerId : entity.spawnedViewers()) {
                    BukkitServerPlayer serverPlayer = (BukkitServerPlayer) this.plugin.networkManager().getOnlineUser(viewerId);
                    if (serverPlayer == null) continue;
                    showDestroyStageDisplayTo(serverPlayer, entity, newIndex);
                }
            }
        }
    }

    public void clearDestroyStageDisplayForAll() {
        DestroyStageDisplayRecorder.Entry entry = this.destroyStageEntry;
        if (entry == null) {
            return;
        }
        for (UUID minerId : entry.minerProgress.keys()) {
            BukkitServerPlayer serverPlayer = (BukkitServerPlayer) this.plugin.networkManager().getOnlineUser(minerId);
            if (serverPlayer == null) continue;
            serverPlayer.destroyStageEntry = null;
            serverPlayer.lastSentState = -1;
        }
        BukkitDestroyStageDisplayRecorder.INSTANCE.remove(entry.key);
    }

    private boolean isWithinDestroyRange(BlockPos hitPos, Player other) {
        double d0 = (double) hitPos.x() - other.x();
        double d1 = (double) hitPos.y() - other.y();
        double d2 = (double) hitPos.z() - other.z();
        return d0 * d0 + d1 * d1 + d2 * d2 < 32 * 32;
    }

    @Override
    public double getCachedInteractionRange() {
        if (VersionHelper.isOrAbove1_20_5) {
            if (this.lastUpdateInteractionRangeTick + 20 > this.gameTicks) {
                return this.cachedInteractionRange;
            }
            Object attribute = LivingEntityProxy.INSTANCE.getAttribute(minecraftPlayer(), AttributesProxy.BLOCK_INTERACTION_RANGE);
            if (attribute == null) {
                this.cachedInteractionRange = 4.5d;
            } else {
                this.cachedInteractionRange = AttributeInstanceProxy.INSTANCE.getValue(attribute);
            }
            this.lastUpdateInteractionRangeTick = this.gameTicks;
            return this.cachedInteractionRange;
        } else {
            return 4.5d;
        }
    }

    @Override
    public void onSwingHand() {
        this.swingHandAck = true;
        this.lastSwingHandTick = gameTicks();
    }

    public boolean hasSwingHand() {
        return this.swingHandAck && this.lastSwingHandTick + 2 > gameTicks();
    }

    @Override
    public int entityId() {
        return this.entityId;
    }

    @Override
    public boolean isOnline() {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return false;
        return player.isOnline();
    }

    @Override
    public float yRot() {
        return EntityProxy.INSTANCE.getYRot(this.minecraftPlayer());
    }

    @Override
    public float xRot() {
        return EntityProxy.INSTANCE.getXRot(this.minecraftPlayer());
    }

    @Override
    public boolean isSecondaryUseActive() {
        return isSneaking();
    }

    @Override
    public Direction getDirection() {
        return DirectionUtils.toDirection(platformPlayer().getFacing());
    }

    @Override
    public World world() {
        return BukkitAdaptor.adapt(platformPlayer().getWorld());
    }

    @Override
    public double x() {
        return EntityProxy.INSTANCE.getX(minecraftPlayer());
    }

    @Override
    public double y() {
        return EntityProxy.INSTANCE.getY(minecraftPlayer());
    }

    @Override
    public double z() {
        return EntityProxy.INSTANCE.getZ(minecraftPlayer());
    }

    @Override
    public Object minecraftPlayer() {
        if (this.nmsPlayerRef == null) return null;
        return this.nmsPlayerRef.get();
    }

    @Override
    public org.bukkit.entity.Player platformPlayer() {
        if (this.bukkitPlayerRef == null) return null;
        return this.bukkitPlayerRef.get();
    }

    @Override
    public ChannelHandler connection() {
        if (this.connection == null) {
            Object serverPlayer = minecraftPlayer();
            if (serverPlayer != null) {
                if (VersionHelper.isOrAbove1_20_2) {
                    this.connection = ServerCommonPacketListenerImplProxy.INSTANCE.getConnection(ServerPlayerProxy.INSTANCE.getConnection(serverPlayer));
                } else {
                    this.connection = ServerGamePacketListenerImplProxy.INSTANCE.getConnection(ServerPlayerProxy.INSTANCE.getConnection(serverPlayer));
                }
            } else {
                throw new IllegalStateException("Cannot init or find connection instance for player " + name());
            }
        }
        return this.connection;
    }

    @Override
    public boolean isFakePlayer() {
        return false;
    }

    @Override
    public org.bukkit.entity.Player platformEntity() {
        return platformPlayer();
    }

    @Override
    public Object minecraftEntity() {
        return minecraftPlayer();
    }

    @Override
    public Map<Integer, EntityPacketHandler> entityPacketHandlers() {
        return this.entityTypeView;
    }

    public void setResendSound() {
        resentSoundTick = gameTicks();
    }

    public void setResendSwing() {
        resentSwingTick = gameTicks();
    }

    public boolean shouldResendSound() {
        return resentSoundTick == gameTicks();
    }

    public boolean shouldResendSwing() {
        return resentSwingTick == gameTicks();
    }

    @Override
    public boolean clientCustomBlockEnabled() {
        return this.enableClientCustomBlock;
    }

    @Override
    public void setClientCustomBlock(boolean enable) {
        this.enableClientCustomBlock = enable;
    }

    @Override
    public boolean hasClientMod() {
        return this.clientModProtocol != -1;
    }

    @Override
    public int clientModProtocol() {
        return this.clientModProtocol;
    }

    @Override
    public void setClientModProtocol(int version) {
        this.clientModProtocol = version;
    }

    @Override
    public void setClientBlockList(IntIdentityList blockList) {
        this.blockList = blockList;
    }

    @Override
    public ProtocolVersion protocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public IntIdentityList clientBlockList() {
        return this.blockList;
    }

    @Override
    public void addResourcePackUUID(UUID uuid) {
        if (VersionHelper.isOrAbove1_20_3) {
            this.resourcePackUUID.add(uuid);
        }
    }

    @Override
    public boolean isResourcePackLoading(UUID uuid) {
        return this.resourcePackUUID.contains(uuid);
    }

    @Override
    public void setShouldProcessFinishConfiguration(boolean shouldProcess) {
        this.shouldProcessFinishConfiguration = shouldProcess;
    }

    @Override
    public boolean shouldProcessFinishConfiguration() {
        return this.shouldProcessFinishConfiguration;
    }

    @Override
    public void clearEntityView() {
        this.entityTypeView.clear();
        // 玩家自身实体的包处理器不随视野清空（重生/切世界后仍需要血量 metadata 缩放等处理）
        this.entityTypeView.put(this.entityId, PlayerPacketHandler.INSTANCE);
    }

    @Override
    public void unloadCurrentResourcePack() {
        if (!VersionHelper.isOrAbove1_20_3) {
            return;
        }
        if (decoderState() == ConnectionState.PLAY && !this.resourcePackUUID.isEmpty()) {
            for (UUID u : this.resourcePackUUID) {
                sendPacket(ClientboundResourcePackPopPacketProxy.INSTANCE.newInstance(Optional.ofNullable(u)), true);
            }
            this.resourcePackUUID.clear();
        }
    }

    @Override
    public void performCommand(String command, boolean asOp) {
        org.bukkit.entity.Player player = platformPlayer();
        if (asOp) {
            boolean isOp = player.isOp();
            player.setOp(true);
            try {
                player.performCommand(command);
            } catch (Throwable t) {
                this.plugin.logger().warn("Failed to perform command '" + command + "' for " + this.name() + " as operator", t);
            }
            player.setOp(isOp);
        } else {
            player.performCommand(command);
        }
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void performCommandAsEvent(String command) {
        String formattedCommand = command.startsWith("/") ? command : "/" + command;
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(platformPlayer(), formattedCommand);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public void transfer(String server) {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return;
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArray);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to encode transfer data for " + this.name(), e);
            return;
        }
        player.sendPluginMessage(this.plugin.javaPlugin(), "BungeeCord", byteArray.toByteArray());
    }

    @Override
    public void transfer(String host, int port) {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return;
        player.transfer(host, port);
    }

    @Override
    public int foodLevel() {
        return platformPlayer().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int foodLevel) {
        this.platformPlayer().setFoodLevel(Math.clamp(foodLevel, 0, 20));
    }

    @Override
    public float saturation() {
        return platformPlayer().getSaturation();
    }

    @Override
    public void setSaturation(float saturation) {
        this.platformPlayer().setSaturation(saturation);
    }

    @Override
    public CooldownData cooldown() {
        return this.cooldownData;
    }

    @Override
    public boolean isChunkTracked(long chunkPos) {
        return this.trackedChunks.containsKey(chunkPos);
    }

    @Override
    public ClientChunk getTrackedChunk(long chunkPos) {
        return this.trackedChunks.get(chunkPos);
    }

    @Override
    public void addTrackedChunk(long chunkPos, ClientChunk chunkStatus) {
        this.trackedChunks.put(chunkPos, chunkStatus);
    }

    @Override
    public void removeTrackedChunk(long chunkPos) {
        this.trackedChunks.remove(chunkPos);
        if (Config.entityCullingRayTracing()) {
            this.culling.removeLastVisitChunkIfMatches((int) chunkPos, (int) (chunkPos >> 32));
        }
    }

    @Override
    public void clearTrackedChunks() {
        this.trackedChunks.clear();
    }

    @Override
    public Locale locale() {
        if (this.clientLocale != null) {
            return this.clientLocale;
        } else {
            org.bukkit.entity.Player player = this.platformPlayer();
            if (player != null) {
                return player.locale();
            } else {
                return Locale.ENGLISH;
            }
        }
    }

    @Override
    public void setClientLocale(Locale clientLocale) {
        this.clientLocale = clientLocale;
    }

    @Override
    public Locale selectedLocale() {
        if (this.selectedLocale != null) return this.selectedLocale;
        return locale();
    }

    @Override
    public void setSelectedLocale(@Nullable Locale locale) {
        this.selectedLocale = locale;
        if (locale != null) {
            platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(SELECTED_LOCALE_KEY), PersistentDataType.STRING, TranslationManager.formatLocale(locale));
        } else {
            platformPlayer().getPersistentDataContainer().remove(KeyUtils.toNamespacedKey(SELECTED_LOCALE_KEY));
        }
    }

    @Override
    public void setEntityCullingDistanceScale(double value) {
        value = Math.clamp(value, 0.125, 8);
        this.culling.setDistanceScale(value);
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(ENTITY_CULLING_DISTANCE_SCALE), PersistentDataType.DOUBLE, value);
    }

    @Override
    public double entityCullingDistanceScale() {
        return this.culling.distanceScale();
    }

    @Override
    public void setDisplayEntityViewDistanceScale(double value) {
        value = Math.clamp(value, 0.125, 8);
        this.displayEntityViewDistance = value;
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(DISPLAY_ENTITY_VIEW_DISTANCE_SCALE), PersistentDataType.DOUBLE, value);
    }

    @Override
    public double displayEntityViewDistance() {
        return this.displayEntityViewDistance;
    }

    @Override
    public void setEnableEntityCulling(boolean enable) {
        this.enableEntityCulling = enable;
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(ENABLE_ENTITY_CULLING), PersistentDataType.BOOLEAN, enable);
    }

    @Override
    public boolean enableEntityCulling() {
        return this.enableEntityCulling;
    }

    @Override
    public DamageVisibility damageVisibility() {
        return this.damageVisibility;
    }

    @Override
    public void setDamageVisibility(DamageVisibility visibility) {
        this.damageVisibility = visibility;
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(DAMAGE_VISIBILITY), PersistentDataType.STRING, visibility.name());
    }

    @Override
    public void setEnableFurnitureDebug(boolean enable) {
        this.enableFurnitureDebug = enable;
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(ENABLE_FURNITURE_DEBUG), PersistentDataType.BOOLEAN, enable);
    }

    @Override
    public boolean enableFurnitureDebug() {
        return enableFurnitureDebug;
    }

    @Override
    public void giveExperiencePoints(int xpPoints) {
        platformPlayer().giveExp(xpPoints);
    }

    @Override
    public void giveExperienceLevels(int levels) {
        platformPlayer().giveExpLevels(levels);
    }

    @Override
    public int getXpNeededForNextLevel() {
        return PlayerProxy.INSTANCE.getXpNeededForNextLevel(minecraftPlayer());
    }

    @Override
    public void setExperiencePoints(int experiencePoints) {
        float xpNeededForNextLevel = this.getXpNeededForNextLevel();
        float maxProgressThreshold = (xpNeededForNextLevel - 1.0F) / xpNeededForNextLevel;
        float experienceProgress = MiscUtils.clamp(experiencePoints / xpNeededForNextLevel, 0.0F, maxProgressThreshold);
        platformPlayer().setExp(experienceProgress);
    }

    @Override
    public void setExperienceLevels(int level) {
        platformPlayer().setLevel(level);
    }

    @Override
    public void sendTotemAnimation(Item totem, @Nullable SoundData sound, boolean silent) {
        PlayerUtils.sendTotemAnimation(this, totem, sound, silent);
    }

    @Override
    public void addTrackedBlockEntities(Map<BlockPos, ConstantBlockEntityRenderer> renders) {
        for (Map.Entry<BlockPos, ConstantBlockEntityRenderer> entry : renders.entrySet()) {
            this.trackedBlockEntityRenderers.put(entry.getKey(), new CullableHolder(entry.getValue()));
        }
    }

    @Override
    public void addTrackedBlockEntity(BlockPos blockPos, ConstantBlockEntityRenderer renderer) {
        this.trackedBlockEntityRenderers.put(blockPos, new CullableHolder(renderer));
    }

    @Override
    public CullableHolder getTrackedBlockEntity(BlockPos blockPos) {
        return this.trackedBlockEntityRenderers.get(blockPos);
    }

    @Override
    public void removeTrackedBlockEntities(Collection<BlockPos> renders) {
        for (BlockPos render : renders) {
            CullableHolder remove = this.trackedBlockEntityRenderers.remove(render);
            if (remove != null && remove.isShown) {
                remove.cullable.hide(this);
            }
        }
    }

    @Override
    public void removeTrackedBlockEntities(BlockPos pos) {
        CullableHolder remove = this.trackedBlockEntityRenderers.remove(pos);
        if (remove != null && remove.isShown) {
            remove.cullable.hide(this);
        }
    }

    @Override
    public void clearTrackedBlockEntities() {
        this.trackedBlockEntityRenderers.clear();
        this.trackedDynamicBlockEntityRenderers.clear();
    }

    @Override
    public void addTrackedDynamicBlockEntities(Map<BlockPos, DynamicBlockEntityRenderer> renderers) {
        for (Map.Entry<BlockPos, DynamicBlockEntityRenderer> entry : renderers.entrySet()) {
            this.addTrackedDynamicBlockEntity(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void addTrackedDynamicBlockEntity(BlockPos blockPos, DynamicBlockEntityRenderer renderer) {
        CullableHolder holder = new CullableHolder(renderer, renderer.initialForceVisible(this));
        this.trackedDynamicBlockEntityRenderers.put(blockPos, holder);
        if (holder.forceVisible) {
            holder.setShown(this, true);
        }
    }

    @Override
    public CullableHolder getTrackedDynamicBlockEntity(BlockPos blockPos) {
        return this.trackedDynamicBlockEntityRenderers.get(blockPos);
    }

    @Override
    public void removeTrackedDynamicBlockEntities(Collection<BlockPos> renders) {
        for (BlockPos render : renders) {
            this.removeTrackedDynamicBlockEntity(render);
        }
    }

    @Override
    public void removeTrackedDynamicBlockEntity(BlockPos pos) {
        CullableHolder remove = this.trackedDynamicBlockEntityRenderers.remove(pos);
        if (remove != null && remove.isShown) {
            remove.cullable.hide(this);
        }
    }

    @Override
    public int clearOrCountMatchingInventoryItems(Predicate<Item> predicate, int count) {
        Predicate<Object> nmsPredicate = nmsStack -> predicate.test(this.plugin.itemManager().wrap(ItemStackUtils.getBukkitStack(nmsStack)));
        Object inventory = PlayerProxy.INSTANCE.getInventory(minecraftPlayer());
        Object inventoryMenu = PlayerProxy.INSTANCE.getInventoryMenu(minecraftPlayer());
        Object craftSlots = InventoryMenuProxy.INSTANCE.getCraftSlots(inventoryMenu);
        return InventoryProxy.INSTANCE.clearOrCountMatchingItems(inventory, nmsPredicate, count, craftSlots);
    }

    @Override
    public GameEdition gameEdition() {
        if (this.gameEdition == null) {
            this.gameEdition = this.plugin.compatibilityManager().isBedrockPlayer(this) ? GameEdition.BEDROCK : GameEdition.JAVA;
        }
        return this.gameEdition;
    }

    @Override
    public CullableHolder getTrackedEntity(int entityId) {
        return this.trackedEntities.get(entityId);
    }

    @Override
    public void addTrackedEntity(int entityId, Cullable cullable) {
        this.trackedEntities.put(entityId, new CullableHolder(cullable));
    }

    @Override
    public void removeTrackedEntity(int entityId) {
        CullableHolder remove = this.trackedEntities.remove(entityId);
        if (remove != null && remove.isShown) {
            remove.cullable.hide(this);
        }
    }

    @Override
    public void clearTrackedEntities() {
        this.trackedEntities.clear();
    }

    @Override
    public Cache<Object, Boolean> receivedMapData() {
        return this.receivedMapData;
    }

    @Override
    public FurnitureLightData furnitureLightData() {
        return this.furnitureLightData;
    }

    @Override
    public void playParticle(Key particleId, double x, double y, double z) {
        Particle particle = Registry.PARTICLE_TYPE.get(KeyUtils.toNamespacedKey(particleId));
        if (particle != null) {
            if (VersionHelper.hasPaperPatch) {
                platformPlayer().getWorld().spawnParticle(particle, List.of(platformPlayer()), null, x, y, z, 1, 0, 0, 0, 0, null, false);
            } else {
                platformPlayer().spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private Object rayTrace(Object serverPlayer, double range) {
        Object start = Vec3Proxy.INSTANCE.newInstance(this.eyeLocation.x, this.eyeLocation.y, this.eyeLocation.z);
        Object direction = EntityProxy.INSTANCE.getLookAngle(serverPlayer);
        Object end = Vec3Proxy.INSTANCE.add(
                start,
                Vec3Proxy.INSTANCE.getX(direction) * range,
                Vec3Proxy.INSTANCE.getY(direction) * range,
                Vec3Proxy.INSTANCE.getZ(direction) * range
        );
        Object context = ClipContextProxy.INSTANCE.newInstance(
                start,
                end,
                ClipContextProxy.BlockProxy.OUTLINE,
                ClipContextProxy.FluidProxy.NONE,
                serverPlayer
        );
        return BlockGetterProxy.INSTANCE.clip(EntityProxy.INSTANCE.getLevel(serverPlayer), context);
    }

    public Map<BlockPos, CullableHolder> trackedBlockEntityRenderers() {
        return Collections.unmodifiableMap(this.trackedBlockEntityRenderers);
    }

    public Map<Integer, CullableHolder> trackedFurniture() {
        return Collections.unmodifiableMap(this.trackedEntities);
    }

    public boolean isRangeMining() {
        return this.isRangeMining;
    }

    public void setRangeMining(boolean rangeMining) {
        this.isRangeMining = rangeMining;
    }

    public FurnitureHitData furnitureHitData() {
        return furnitureHitData;
    }

    public boolean addObtainedItem(Key item) {
        return this.obtainedItems.add(UniqueKey.create(item));
    }

    public Set<UniqueKey> obtainedItems() {
        return this.obtainedItems;
    }

    @Override
    public void addResourcePackTasks(List<ResourcePackDownloadData> dataList) {
        if (dataList.isEmpty()) return;
        if (VersionHelper.isOrAbove1_20_2) {
            ChannelHandler connection = connection();
            if (connection == null) return;
            Object packetListener = ConnectionProxy.INSTANCE.getPacketListener(connection);
            if (!ServerConfigurationPacketListenerImplProxy.CLASS.isInstance(packetListener)) return;
            Queue<Object> tasks = ServerConfigurationPacketListenerImplProxy.INSTANCE.getConfigurationTasks(packetListener);
            boolean removed = tasks.removeIf(JoinWorldTaskProxy.CLASS::isInstance);
            if (VersionHelper.isOrAbove1_20_3) {
                for (ResourcePackDownloadData data : dataList) {
                    tasks.add(ServerResourcePackConfigurationTaskProxy.INSTANCE.newInstance(ResourcePackUtils.createServerResourcePackInfo(data.uuid(), data.url(), data.sha1())));
                    addResourcePackUUID(data.uuid());
                }
            } else {
                ResourcePackDownloadData data = dataList.getFirst();
                tasks.add(ServerResourcePackConfigurationTaskProxy.INSTANCE.newInstance(ResourcePackUtils.createServerResourcePackInfo(data.uuid(), data.url(), data.sha1())));
            }
            if (removed) {
                tasks.add(JoinWorldTaskProxy.INSTANCE.newInstance());
            }
        } else {
            ResourcePackDownloadData data = dataList.getFirst();
            sendPacket(ResourcePackUtils.createPacket(data.uuid(), data.url(), data.sha1()), true);
        }
    }

    @Override
    public InetAddress address() {
        if (this.address == null) {
            SocketAddress socketAddress = this.channel.remoteAddress();
            if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                this.address = inetSocketAddress.getAddress();
            }
        }
        return this.address;
    }

    @Override
    public void setItemCooldown(Key id, int ticks) {
        if (VersionHelper.isOrAbove1_21_2) {
            Object serverPlayer = minecraftPlayer();
            Object cooldowns = PlayerProxy.INSTANCE.getCooldowns(serverPlayer);
            ItemCooldownsProxy.INSTANCE.addCooldown(cooldowns, KeyUtils.toIdentifier(id), ticks);
        }
    }

    @Override
    public int getItemCooldown(Key id) {
        if (VersionHelper.isOrAbove1_21_2) {
            Object serverPlayer = minecraftPlayer();
            Object cooldowns = PlayerProxy.INSTANCE.getCooldowns(serverPlayer);
            Map<Object, Object> instanceById = ItemCooldownsProxy.INSTANCE.getCooldowns(cooldowns);
            Object instance = instanceById.get(KeyUtils.toIdentifier(id));
            if (instance != null) {
                return Math.max(0, ItemCooldownsProxy.CooldownInstanceProxy.INSTANCE.getEndTime(instance) - ItemCooldownsProxy.INSTANCE.getTickCount(cooldowns));
            }
        }
        return 0;
    }

    @NotNull
    @Override
    public BukkitItem getItemBySlot(int slot) {
        PlayerInventory inventory = platformPlayer().getInventory();
        return BukkitItemManager.instance().wrap(inventory.getItem(slot));
    }
}
