package net.momirealms.craftengine.bukkit.plugin.user;

import ca.spottedleaf.concurrentutil.map.ConcurrentLong2ReferenceChainedHashTable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.block.entity.BlockEntityHolder;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.gui.CraftEngineGUIHolder;
import net.momirealms.craftengine.bukkit.plugin.network.payload.DiscardedPayload;
import net.momirealms.craftengine.bukkit.plugin.network.payload.UnknownPayload;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MAttributeHolders;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MMobEffects;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.data.EntityData;
import net.momirealms.craftengine.core.entity.furniture.FurnitureHitData;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.culling.EntityCulling;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class BukkitServerPlayer extends Player {
    public static final Key SELECTED_LOCALE_KEY = Key.ce("locale");
    public static final Key ENTITY_CULLING_DISTANCE_SCALE = Key.ce("entity_culling_distance_scale");
    public static final Key DISPLAY_ENTITY_VIEW_DISTANCE_SCALE = Key.ce("display_entity_view_distance_scale");
    public static final Key ENABLE_ENTITY_CULLING = Key.ce("enable_entity_culling");
    public static final Key ENABLE_FURNITURE_DEBUG = Key.ce("enable_furniture_debug");
    private final BukkitCraftEngine plugin;

    // connection state
    private final Channel channel;
    private ChannelHandler connection;
    private String name;
    private UUID uuid;
    private boolean isNameVerified;
    private boolean isUUIDVerified;
    private ConnectionState decoderState; // inbound(decode|c2s)
    private ConnectionState encoderState; // outbound(encode|s2c)
    private boolean shouldProcessFinishConfiguration = true;
    private final Set<UUID> resourcePackUUID = Collections.synchronizedSet(new HashSet<>());
    // some references
    private Reference<org.bukkit.entity.Player> playerRef;
    private Reference<Object> serverPlayerRef;
    // client side dimension info
    private World clientSideWorld;
    // check main hand/offhand interaction
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
    private boolean swingHandAck;
    private int lastSwingHandTick;
    private float miningProgress;
    // for client visual sync
    private int resentSoundTick;
    private int resentSwingTick;
    // has fabric client mod or not
    private boolean hasClientMod = false;
    private IntIdentityList blockList = new IntIdentityList(BlockStateUtils.vanillaBlockStateCount());
    // cache if player can break blocks
    private boolean clientSideCanBreak = true;
    // a cooldown for better breaking experience
    private int lastSuccessfulBreak;
    // player's game tick
    private int gameTicks;
    // cache interaction range here
    private int lastUpdateInteractionRangeTick;
    private double cachedInteractionRange;
    // cooldown data
    private CooldownData cooldownData;
    // tracked chunks
    private ConcurrentLong2ReferenceChainedHashTable<ClientChunk> trackedChunks;
    // entity view
    private Map<Integer, EntityPacketHandler> entityTypeView;
    // 通过指令或api设定的语言
    @Nullable
    private Locale selectedLocale;
    // 客户端选择的语言
    private Locale clientLocale;
    // 跟踪到的方块实体渲染器
    private final Map<BlockPos, CullableHolder> trackedBlockEntityRenderers = new ConcurrentHashMap<>();
    private final Map<Integer, CullableHolder> trackedEntities = new ConcurrentHashMap<>();
    private final EntityCulling culling;
    private Vec3d firstPersonCameraVec3;
    private Vec3d thirdPersonCameraVec3;
    // 是否启用实体剔除
    private boolean enableEntityCulling;
    // 玩家眼睛所在位置
    private Location eyeLocation;
    // 是否启用家具调试
    private boolean enableFurnitureDebug;
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
    // 家具击打记录
    private final FurnitureHitData furnitureHitData = new FurnitureHitData();
    // 缓存的已接收的地图数据，为了防止动态物品展示框渲染器在渲染地图物品的时候重复发送地图数据导致服务器带宽消耗过大
    private final Cache<Object, Boolean> receivedMapData = CacheBuilder.newBuilder()
            .weakKeys()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .concurrencyLevel(4)
            .build();

    public BukkitServerPlayer(BukkitCraftEngine plugin, @Nullable Channel channel) {
        this.channel = channel;
        this.plugin = plugin;
        if (channel != null) {
            for (String name : channel.pipeline().names()) {
                ChannelHandler handler = channel.pipeline().get(name);
                if (NetworkReflections.clazz$Connection.isInstance(handler)) {
                    this.connection = handler;
                    break;
                }
            }
        }
        this.culling = new EntityCulling(this);
    }

    public void setPlayer(org.bukkit.entity.Player player) {
        this.playerRef = new WeakReference<>(player);
        this.serverPlayerRef = new WeakReference<>(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player));
        this.uuid = player.getUniqueId();
        this.isUUIDVerified = true;
        this.name = player.getName();
        this.isNameVerified = true;
        byte[] bytes = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(CooldownData.COOLDOWN_KEY), PersistentDataType.BYTE_ARRAY);
        String locale = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(SELECTED_LOCALE_KEY), PersistentDataType.STRING);
        Double scale = player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENTITY_CULLING_DISTANCE_SCALE), PersistentDataType.DOUBLE);
        this.displayEntityViewDistance = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(DISPLAY_ENTITY_VIEW_DISTANCE_SCALE), PersistentDataType.DOUBLE)).orElse(1d);
        this.enableEntityCulling = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENABLE_ENTITY_CULLING), PersistentDataType.BOOLEAN)).orElse(true);
        this.enableFurnitureDebug = Optional.ofNullable(player.getPersistentDataContainer().get(KeyUtils.toNamespacedKey(ENABLE_FURNITURE_DEBUG), PersistentDataType.BOOLEAN)).orElse(false);
        this.culling.setDistanceScale(Optional.ofNullable(scale).orElse(1.0));
        this.selectedLocale = TranslationManager.parseLocale(locale);
        this.trackedChunks = ConcurrentLong2ReferenceChainedHashTable.createWithCapacity(512, 0.5f);
        this.entityTypeView = new ConcurrentHashMap<>(256);
        try {
            this.cooldownData = CooldownData.fromBytes(bytes);
        } catch (IOException e) {
            this.cooldownData = new CooldownData();
            this.plugin.logger().warn("Failed to parse cooldown data", e);
        }
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
    public boolean isSneaking() {
        return platformPlayer().isSneaking();
    }

    @Override
    public boolean isSwimming() {
        return platformPlayer().isSwimming();
    }

    @Override
    public boolean isClimbing() {
        return platformPlayer().isClimbing();
    }

    @Override
    public boolean isGliding() {
        return platformPlayer().isGliding();
    }

    @Override
    public boolean isFlying() {
        return platformPlayer().isFlying();
    }

    @Override
    public GameMode gameMode() {
        return switch (platformPlayer().getGameMode()) {
            case CREATIVE -> GameMode.CREATIVE;
            case SPECTATOR -> GameMode.SPECTATOR;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SURVIVAL -> GameMode.SURVIVAL;
        };
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
    public void sendToast(Component text, Item<?> icon, AdvancementType type) {
        this.plugin.advancementManager().sendToast(this, icon, text, type);
    }

    @Override
    public void sendActionBar(Component text) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundActionBarPacket(ComponentUtils.adventureToMinecraft(text));
        sendPacket(packet, false);
    }

    @Override
    public void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Object titlePacket = FastNMS.INSTANCE.constructor$ClientboundSetTitleTextPacket(ComponentUtils.adventureToMinecraft(title));
        Object subtitlePacket = FastNMS.INSTANCE.constructor$ClientboundSetSubtitleTextPacket(ComponentUtils.adventureToMinecraft(subtitle));
        Object timePacket = FastNMS.INSTANCE.constructor$ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
        sendPackets(List.of(titlePacket, subtitlePacket, timePacket), false);
    }

    @Override
    public void sendMessage(Component text, boolean overlay) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(ComponentUtils.adventureToMinecraft(text), overlay);
        sendPacket(packet, false);
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

    @Override
    public void swingHand(InteractionHand hand) {
        platformPlayer().swingHand(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
    }

    @Override
    public boolean hasPermission(String permission) {
        return platformPlayer().hasPermission(permission);
    }

    @Override
    public boolean canInstabuild() {
        Object abilities = FastNMS.INSTANCE.field$Player$abilities(serverPlayer());
        return FastNMS.INSTANCE.field$Abilities$instabuild(abilities);
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
    public void playSound(Key sound, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(platformPlayer(), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void playSound(Position pos, Key sound, SoundSource source, float volume, float pitch) {
        platformPlayer().playSound(new Location(null, pos.x(), pos.y(), pos.z()), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void giveItem(Item<?> item) {
        PlayerUtils.giveItem(this, item.count(), (Item<ItemStack>) item);
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
    public void sendCustomPayload(Key channelId, byte[] data) {
        try {
            Object channelResourceLocation = KeyUtils.toResourceLocation(channelId);
            Object responsePacket;
            if (VersionHelper.isOrAbove1_20_2()) {
                Object dataPayload;
                if (VersionHelper.isOrAbove1_20_5()) {
                    dataPayload = NetworkReflections.constructor$DiscardedPayload.newInstance(channelResourceLocation, DiscardedPayload.useNewMethod ? data : Unpooled.wrappedBuffer(data));
                } else {
                    dataPayload = NetworkReflections.constructor$ServerboundCustomPayloadPacket$UnknownPayload.newInstance(channelResourceLocation, UnknownPayload.isByteArray ? data : Unpooled.wrappedBuffer(data));
                }
                responsePacket = NetworkReflections.constructor$ClientboundCustomPayloadPacket.newInstance(dataPayload);
            } else {
                responsePacket = NetworkReflections.constructor$ClientboundCustomPayloadPacket.newInstance(channelResourceLocation, FastNMS.INSTANCE.constructor$FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
            }
            this.sendPacket(responsePacket, true);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to send custom payload to " + name(), e);
        }
    }

    @Override
    public void kick(@Nullable Component message) {
        Object reason = message != null ? ComponentUtils.adventureToMinecraft(message) : null;
        if (this.encoderState == ConnectionState.HANDSHAKING || this.encoderState == ConnectionState.STATUS) {
            FastNMS.INSTANCE.method$Connection$disconnect(this.connection(), reason);
            return;
        }
        try {
            if (this.encoderState == ConnectionState.LOGIN) {
                this.sendPacket(NetworkReflections.constructor$ClientboundLoginDisconnectPacket.newInstance(reason), false);
                FastNMS.INSTANCE.method$Connection$disconnect(this.connection(), reason);
                return;
            }
            Object kickPacket = NetworkReflections.constructor$ClientboundDisconnectPacket.newInstance(reason);
            this.sendPacket(kickPacket, false, () -> FastNMS.INSTANCE.method$Connection$disconnect(this.connection(), reason));
            this.nettyChannel().config().setAutoRead(false);
            Runnable handleDisconnection = () -> FastNMS.INSTANCE.method$Connection$handleDisconnection(this.connection());
            FastNMS.INSTANCE.method$BlockableEventLoop$scheduleOnMain(handleDisconnection);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to kick " + name(), e);
        }
    }

    @Override
    public ConnectionState decoderState() {
        return decoderState;
    }

    @Override
    public ConnectionState encoderState() {
        return encoderState;
    }

    @Override
    public World clientSideWorld() {
        return this.clientSideWorld;
    }

    @Override
    public void setClientSideWorld(World world) {
        this.clientSideWorld = world;
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
    public void tick() {
        // 还没上线或是已经离线
        Object serverPlayer = serverPlayer();
        if (serverPlayer == null) return;

        // 更新玩家游戏刻
        org.bukkit.entity.Player bukkitPlayer = platformPlayer();
        if (VersionHelper.isFolia()) {
            try {
                Object gameMode = FastNMS.INSTANCE.field$ServerPlayer$gameMode(serverPlayer);
                this.gameTicks = (int) CoreReflections.field$ServerPlayerGameMode$gameTicks.get(gameMode);
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to get game tick for " + name(), e);
            }
        } else {
            this.gameTicks = FastNMS.INSTANCE.field$MinecraftServer$currentTick();
        }

        // 更新CE UI
        if (this.gameTicks % 20 == 0) {
            this.updateGUI();
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
            Location unsureEyeLocation = bukkitPlayer.getEyeLocation();
            Entity vehicle = bukkitPlayer.getVehicle();
            if (vehicle != null) {
                Object mountPos = FastNMS.INSTANCE.method$Entity$getPassengerRidingPosition(FastNMS.INSTANCE.method$CraftEntity$getHandle(vehicle), serverPlayer);
                unsureEyeLocation.set(FastNMS.INSTANCE.field$Vec3$x(mountPos), FastNMS.INSTANCE.field$Vec3$y(mountPos) + bukkitPlayer.getEyeHeight(), FastNMS.INSTANCE.field$Vec3$z(mountPos));
            }
            this.eyeLocation = unsureEyeLocation;
        }

        // 本tick内有挥手
        if (hasSwingHand()) {
            if (this.gameTicks - this.preventBreakTick >= 3) {
                // 连续挥手且没被重置
                // 原版有的，方块挖掘间隔。除非是秒破，否则必走此延迟
                if (this.gameTicks - this.lastSuccessfulBreak > 5) {
                    if (this.isDestroyingBlock) {
                        this.tickBlockDestroy();
                    } else {
                        // 连续挥手且没被重置
                        if (++this.awfulBreakFixer >= 4) {
                            this.awfulBreakFixer = 0;
                            RayTraceResult result = rayTrace(this.eyeLocation, getCachedInteractionRange(), FluidCollisionMode.NEVER);
                            if (result != null) {
                                Entity hitEntity = result.getHitEntity();
                                if (hitEntity == null) {
                                    Block hitBlock = result.getHitBlock();
                                    if (hitBlock != null) {
                                        Location location = hitBlock.getLocation();
                                        BlockPos hitPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                                        Object blockState = BlockStateUtils.getBlockState(hitBlock);
                                        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
                                        this.startMiningBlock(hitPos, blockState, customState);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            this.swingHandAck = false;
        }

        // 实体剔除更新相机位置
        if (Config.entityCullingRayTracing()) {
            org.bukkit.entity.Player player = platformPlayer();
            Location eyeLocation = this.eyeLocation.clone();
            this.firstPersonCameraVec3 = LocationUtils.toVec3d(eyeLocation);
            int distance = 4;
            if (VersionHelper.isOrAbove1_21_6()) {
                Entity vehicle = player.getVehicle();
                if (vehicle != null && vehicle.getType() == EntityType.HAPPY_GHAST) {
                    distance = 8;
                }
            }
            this.thirdPersonCameraVec3 = LocationUtils.toVec3d(eyeLocation.subtract(eyeLocation.getDirection().multiply(distance)));
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
            for (CullableHolder cullableObject : this.trackedEntities.values()) {
                cullEntity(useRayTracing, cullableObject);
            }
        } else {
            for (CullableHolder cullableObject : this.trackedBlockEntityRenderers.values()) {
                cullableObject.setShown(this, true);
            }
            for (CullableHolder cullableObject : this.trackedEntities.values()) {
                cullableObject.setShown(this, true);
            }
        }
    }

    private void cullEntity(boolean useRayTracing, CullableHolder cullableObject) {
        CullingData cullingData = cullableObject.cullable.cullingData();
        if (cullingData != null) {
            boolean firstPersonVisible = this.culling.isVisible(cullingData, this.firstPersonCameraVec3, useRayTracing);
            // 之前可见
            if (cullableObject.isShown) {
                boolean thirdPersonVisible = this.culling.isVisible(cullingData, this.thirdPersonCameraVec3, useRayTracing);
                if (!firstPersonVisible && !thirdPersonVisible) {
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
        } else {
            cullableObject.setShown(this, true);
        }
    }

    private void updateGUI() {
        org.bukkit.inventory.Inventory top = !VersionHelper.isOrAbove1_21() ? LegacyInventoryUtils.getTopInventory(platformPlayer()) : platformPlayer().getOpenInventory().getTopInventory();
        if (!InventoryUtils.isCustomContainer(top)) return;
        if (top.getHolder() instanceof CraftEngineGUIHolder holder) {
            holder.gui().onTimer();
        } else if (top.getHolder() instanceof BlockEntityHolder holder) {
            BlockEntity blockEntity = holder.blockEntity();
            BlockPos blockPos = blockEntity.pos();
            if (!canInteractWithBlock(blockPos, 4d)) {
                platformPlayer().closeInventory();
            }
        }
    }

    public boolean canInteractWithBlock(BlockPos pos, double distance) {
        double d = this.getCachedInteractionRange() + distance;
        return (new AABB(pos)).distanceToSqr(this.getEyePos()) < d * d;
    }

    public boolean canInteractPoint(Vec3d pos, double distance) {
        double d = this.getCachedInteractionRange() + distance;
        return Vec3d.distanceToSqr(this.getEyePos(), pos) < d * d;
    }

    @Override
    public float getDestroyProgress(Object blockState, BlockPos pos) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        float progress = FastNMS.INSTANCE.method$BlockStateBase$getDestroyProgress(blockState, serverPlayer(), FastNMS.INSTANCE.field$CraftWorld$ServerLevel(platformPlayer().getWorld()), LocationUtils.toBlockPos(pos));
        if (optionalCustomState.isPresent()) {
            ImmutableBlockState customState = optionalCustomState.get();
            Item<ItemStack> tool = getItemInHand(InteractionHand.MAIN_HAND);
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
                this.broadcastDestroyProgress(this.platformPlayer(), previousPos, -1);
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
                    boolean attributeCannotBreak = VersionHelper.isOrAbove1_20_5() && !this.clientSideCanBreak;
                    // 2. 客户端侧的方块就是不能秒破
                    if (attributeCannotBreak || getDestroyProgress(vanillaBlockState.literalObject(), pos) < 1f) {
                        Object levelEventPacket = FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(
                                WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
                        sendPacket(levelEventPacket, false);
                    }
                }
            }
        } else {
            // 客户端此时觉得自己不能秒破，但实际可以秒破
            if (canInstantBreak && VersionHelper.isOrAbove1_20_5() && !this.clientSideCanBreak) {
                Object levelEventPacket = FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(
                        WorldEvents.BLOCK_BREAK_EFFECT, LocationUtils.toBlockPos(pos), BlockStateUtils.blockStateToId(state), false);
                sendPacket(levelEventPacket, false);
            }
        }

        // 对于原版方块不影响属性，对于自定义方块需要影响
        setClientSideCanBreakBlock(!custom || canInstantBreak);
    }

    @Override
    public void setClientSideCanBreakBlock(boolean canBreak) {
        try {
            // 超过1秒就强制同步一次属性
            if (this.clientSideCanBreak == canBreak && !shouldSyncAttribute()) {
                return;
            }
            this.clientSideCanBreak = canBreak;
            if (canBreak) {
                if (VersionHelper.isOrAbove1_20_5()) {
                    Object serverPlayer = serverPlayer();
                    Object attributeInstance = CoreReflections.methodHandle$ServerPlayer$getAttributeMethod.invokeExact(serverPlayer, MAttributeHolders.BLOCK_BREAK_SPEED);
                    sendPacket(FastNMS.INSTANCE.constructor$ClientboundUpdateAttributesPacket(entityId(), Lists.newArrayList(attributeInstance)), true);
                } else {
                    resetEffect(MMobEffects.MINING_FATIGUE);
                    resetEffect(MMobEffects.HASTE);
                }
            } else {
                if (VersionHelper.isOrAbove1_20_5()) {
                    Object attributeModifier = VersionHelper.isOrAbove1_21() ?
                            CoreReflections.constructor$AttributeModifier.newInstance(KeyUtils.toResourceLocation(Key.DEFAULT_NAMESPACE, "custom_hardness"), -9999d, CoreReflections.instance$AttributeModifier$Operation$ADD_VALUE) :
                            CoreReflections.constructor$AttributeModifier.newInstance(UUID.randomUUID(), Key.DEFAULT_NAMESPACE + ":custom_hardness", -9999d, CoreReflections.instance$AttributeModifier$Operation$ADD_VALUE);
                    Object attributeSnapshot = NetworkReflections.constructor$ClientboundUpdateAttributesPacket$AttributeSnapshot.newInstance(MAttributeHolders.BLOCK_BREAK_SPEED, 1d, Lists.newArrayList(attributeModifier));
                    Object newPacket = NetworkReflections.constructor$ClientboundUpdateAttributesPacket1.newInstance(entityId(), Lists.newArrayList(attributeSnapshot));
                    sendPacket(newPacket, true);
                } else {
                    Object fatiguePacket = MobEffectUtils.createPacket(MMobEffects.MINING_FATIGUE, entityId(), (byte) 9, -1, false, false, false);
                    Object hastePacket = MobEffectUtils.createPacket(MMobEffects.HASTE, entityId(), (byte) 0, -1, false, false, false);
                    sendPackets(List.of(fatiguePacket, hastePacket), true);
                }
            }
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to set attribute for player " + platformPlayer().getName(), e);
        }
    }

    // 客户端完成破坏方块
    @Override
    public void finishMiningBlock() {
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
            this.broadcastDestroyProgress(platformPlayer(), pos, -1);
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

    private void resetEffect(Object mobEffect) throws ReflectiveOperationException {
        Object effectInstance = CoreReflections.method$ServerPlayer$getEffect.invoke(serverPlayer(), mobEffect);
        Object packet;
        if (effectInstance != null) {
            packet = NetworkReflections.constructor$ClientboundUpdateMobEffectPacket.newInstance(entityId(), effectInstance);
        } else {
            packet = NetworkReflections.constructor$ClientboundRemoveMobEffectPacket.newInstance(entityId(), mobEffect);
        }
        sendPacket(packet, true);
    }

    private void tickBlockDestroy() {
        int currentTick = gameTicks();
        // 如果没有正在被挖掘的对象，那么不继续
        Object destroyedState = this.destroyedState;
        if (destroyedState == null) return;

        try {
            // 进行实现追踪找到指向的方块
            org.bukkit.entity.Player player = platformPlayer();
            double range = getCachedInteractionRange();
            RayTraceResult result = rayTrace(this.eyeLocation, range, FluidCollisionMode.NEVER);
            if (result == null) return;
            if (result.getHitEntity() != null) return;
            Block hitBlock = result.getHitBlock();
            if (hitBlock == null) return;
            Location location = hitBlock.getLocation();
            BlockPos hitPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // 如果命中点位和网络包设置的不同，那么不继续tick
            if (!hitPos.equals(this.destroyPos)) {
                Object blockState = BlockStateUtils.getBlockState(hitBlock);
                ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
                this.startMiningBlock(hitPos, blockState, customState);
                return;
            }

            Object blockPos = LocationUtils.toBlockPos(hitPos);
            Object serverPlayer = serverPlayer();

            // check item in hand
            Item<ItemStack> item = this.getItemInHand(InteractionHand.MAIN_HAND);

            // 发送破坏中音效
            if (currentTick - this.lastHitBlockTime > 3) {
                // 手上物品不是debug棒
                if (!BukkitItemUtils.isDebugStick(item)) {
                    Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(destroyedState);
                    Object soundEvent = FastNMS.INSTANCE.field$SoundType$hitSound(soundType);
                    Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
                    player.playSound(location, soundId.toString(), SoundCategory.BLOCKS, 0.5F, 0.5F);
                }
                this.lastHitBlockTime = currentTick;
            }

            // accumulate progress (custom blocks only)
            if (this.isDestroyingCustomBlock) {
                // prevent server from taking over breaking custom blocks
                Object gameMode = FastNMS.INSTANCE.field$ServerPlayer$gameMode(serverPlayer);
                CoreReflections.field$ServerPlayerGameMode$isDestroyingBlock.set(gameMode, false);
                if (!item.isEmpty()) {
                    Material itemMaterial = item.getItem().getType();
                    // creative mode + invalid item in hand
                    if (canInstabuild() && (itemMaterial == Material.DEBUG_STICK
                            || itemMaterial == Material.TRIDENT
                            || (VersionHelper.isOrAbove1_20_5() && itemMaterial == MaterialUtils.MACE)
                            || item.hasItemTag(ItemTags.SWORDS))) {
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
                    int packetStage = (int) (this.miningProgress * 10.0F);
                    if (packetStage != this.lastSentState) {
                        this.lastSentState = packetStage;
                        // broadcast changes
                        broadcastDestroyProgress(platformPlayer(), hitPos, packetStage);
                    }

                    // can break now
                    if (this.miningProgress >= 1f) {
                        boolean breakResult = false;
                        // for simplified adventure break, switch mayBuild temporarily
                        if (isAdventureMode() && Config.simplifyAdventureBreakCheck()) {
                            // check the appearance state
                            if (canBreak(hitPos, customState.visualBlockState().literalObject())) {
                                // Error might occur so we use try here
                                try {
                                    FastNMS.INSTANCE.field$Player$mayBuild(serverPlayer, true);
                                    breakResult = (boolean) CoreReflections.method$ServerPlayerGameMode$destroyBlock.invoke(gameMode, blockPos);
                                } finally {
                                    FastNMS.INSTANCE.field$Player$mayBuild(serverPlayer, false);
                                }
                            }
                        } else {
                            // normal break check
                            breakResult = (boolean) CoreReflections.method$ServerPlayerGameMode$destroyBlock.invoke(gameMode, blockPos);
                        }
                        // send break particle + (removed sounds)
                        if (breakResult) {
                            sendPacket(FastNMS.INSTANCE.constructor$ClientboundLevelEventPacket(WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId(), false), false);
                            this.destroyPos = null;
                            this.miningProgress = 0;
                            this.isDestroyingBlock = false;
                            this.swingHandAck = false;
                            this.destroyedState = null;
                            this.isDestroyingCustomBlock = false;
                        } else {
                            // 事件被取消了，重置挖掘进度
                            this.miningProgress = 0;
                            this.isDestroyingCustomBlock = true;
                            this.isDestroyingBlock = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to tick destroy for player " + platformPlayer().getName(), e);
        }
    }

    public void updateLastSuccessBreakTick() {
        this.lastSuccessfulBreak = this.gameTicks;
    }

    @Override
    public void breakBlock(int x, int y, int z) {
        platformPlayer().breakBlock(new Location(platformPlayer().getWorld(), x, y, z).getBlock());
    }

    private void broadcastDestroyProgress(org.bukkit.entity.Player player, BlockPos hitPos, int stage) {
        Object packet = FastNMS.INSTANCE.constructor$ClientboundBlockDestructionPacket(Integer.MAX_VALUE - entityId(), LocationUtils.toBlockPos(hitPos), stage);
        for (org.bukkit.entity.Player other : player.getWorld().getPlayers()) {
            Location otherLocation = other.getLocation();
            double d0 = (double) hitPos.x() - otherLocation.getX();
            double d1 = (double) hitPos.y() - otherLocation.getY();
            double d2 = (double) hitPos.z() - otherLocation.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
                FastNMS.INSTANCE.method$Connection$send(
                        FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(
                                FastNMS.INSTANCE.field$Player$connection(
                                        FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)
                                )
                        ),
                        packet,
                        null
                );
            }
        }
    }

    @Override
    public double getCachedInteractionRange() {
        if (this.lastUpdateInteractionRangeTick + 20 > gameTicks()) {
            return this.cachedInteractionRange;
        }
        this.cachedInteractionRange = FastNMS.INSTANCE.method$Player$getInteractionRange(serverPlayer());
        this.lastUpdateInteractionRangeTick = gameTicks();
        return this.cachedInteractionRange;
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
        return platformPlayer().getEntityId();
    }

    @Override
    public boolean isOnline() {
        org.bukkit.entity.Player player = platformPlayer();
        if (player == null) return false;
        return player.isOnline();
    }

    @Override
    public float yRot() {
        return platformPlayer().getYaw();
    }

    @Override
    public float xRot() {
        return platformPlayer().getPitch();
    }

    @Override
    public boolean isSecondaryUseActive() {
        return isSneaking();
    }

    @Override
    public Direction getDirection() {
        return DirectionUtils.toDirection(platformPlayer().getFacing());
    }

    @NotNull
    @Override
    public Item<ItemStack> getItemInHand(InteractionHand hand) {
        PlayerInventory inventory = platformPlayer().getInventory();
        return BukkitItemManager.instance().wrap(hand == InteractionHand.MAIN_HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand());
    }

    @NotNull
    @Override
    public Item<ItemStack> getItemBySlot(int slot) {
        PlayerInventory inventory = platformPlayer().getInventory();
        return BukkitItemManager.instance().wrap(inventory.getItem(slot));
    }

    @Override
    public void setItemInHand(InteractionHand hand, Item<?> item) {
        PlayerInventory inventory = platformPlayer().getInventory();
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        inventory.setItem(slot, (ItemStack) item.getItem());
    }

    @Override
    public World world() {
        return BukkitAdaptors.adapt(platformPlayer().getWorld());
    }

    @Override
    public double x() {
        return platformPlayer().getX();
    }

    @Override
    public double y() {
        return platformPlayer().getY();
    }

    @Override
    public double z() {
        return platformPlayer().getZ();
    }

    @Override
    public Object serverPlayer() {
        if (serverPlayerRef == null) return null;
        return serverPlayerRef.get();
    }

    @Override
    public org.bukkit.entity.Player platformPlayer() {
        if (playerRef == null) return null;
        return playerRef.get();
    }

    @Override
    public ChannelHandler connection() {
        if (this.connection == null) {
            Object serverPlayer = serverPlayer();
            if (serverPlayer != null) {
                this.connection = (ChannelHandler) FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(
                        FastNMS.INSTANCE.field$Player$connection(serverPlayer)
                );
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
    public Object serverEntity() {
        return serverPlayer();
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
    public boolean clientModEnabled() {
        return this.hasClientMod;
    }

    @Override
    public void setClientModState(boolean enable) {
        this.hasClientMod = enable;
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
        if (VersionHelper.isOrAbove1_20_3()) {
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
    public void clearView() {
        this.entityTypeView.clear();
    }

    @Override
    public void unloadCurrentResourcePack() {
        if (!VersionHelper.isOrAbove1_20_3()) {
            return;
        }
        if (decoderState() == ConnectionState.PLAY && !this.resourcePackUUID.isEmpty()) {
            for (UUID u : this.resourcePackUUID) {
                sendPacket(FastNMS.INSTANCE.constructor$ClientboundResourcePackPopPacket(u), true);
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
    public double luck() {
        if (VersionHelper.isOrAbove1_21_3()) {
            return Optional.ofNullable(platformPlayer().getAttribute(Attribute.LUCK)).map(AttributeInstance::getValue).orElse(1d);
        } else {
            return LegacyAttributeUtils.getLuck(platformPlayer());
        }
    }

    @Override
    public int foodLevel() {
        return platformPlayer().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int foodLevel) {
        this.platformPlayer().setFoodLevel(Math.min(Math.max(0, foodLevel), 20));
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
    public void addPotionEffect(Key potionEffectType, int duration, int amplifier, boolean ambient, boolean particles) {
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(KeyUtils.toNamespacedKey(potionEffectType));
        if (type == null) return;
        this.platformPlayer().addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
    }

    @Override
    public void removePotionEffect(Key potionEffectType) {
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(KeyUtils.toNamespacedKey(potionEffectType));
        if (type == null) return;
        this.platformPlayer().removePotionEffect(type);
    }

    @Override
    public void clearPotionEffects() {
        this.platformPlayer().clearActivePotionEffects();
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
    public void teleport(WorldPosition worldPosition) {
        Location location = new Location((org.bukkit.World) worldPosition.world().platformWorld(), worldPosition.x(), worldPosition.y(), worldPosition.z(), worldPosition.yRot(), worldPosition.xRot());
        this.platformPlayer().teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    public void damage(double amount, Key damageType, @Nullable Object causeEntity) {
        @SuppressWarnings("deprecation")
        DamageType type = Registry.DAMAGE_TYPE.get(KeyUtils.toNamespacedKey(damageType));
        DamageSource source = DamageSource.builder(type != null ? type : DamageType.GENERIC)
                .withCausingEntity(causeEntity instanceof Entity entity ? entity : this.platformPlayer())
                .withDirectEntity(this.platformPlayer())
                .withDamageLocation(this.platformPlayer().getLocation())
                .build();
        this.platformPlayer().damage(amount, source);
    }

    @Override
    public Object entityData() {
        return FastNMS.INSTANCE.field$Entity$entityData(serverEntity());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEntityData(EntityData<T> data) {
        return (T) FastNMS.INSTANCE.method$SynchedEntityData$get(entityData(), data.entityDataAccessor());
    }

    @Override
    public <T> void setEntityData(EntityData<T> data, T value, boolean force) {
        FastNMS.INSTANCE.method$SynchedEntityData$set(entityData(), data.entityDataAccessor(), value, force);
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
                return Locale.US;
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
        value = Math.min(Math.max(0.125, value), 8);
        this.culling.setDistanceScale(value);
        platformPlayer().getPersistentDataContainer().set(KeyUtils.toNamespacedKey(ENTITY_CULLING_DISTANCE_SCALE), PersistentDataType.DOUBLE, value);
    }

    @Override
    public void setDisplayEntityViewDistanceScale(double value) {
        value = Math.min(Math.max(0.125, value), 8);
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
        return platformPlayer().getExperiencePointsNeededForNextLevel();
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
    public void sendTotemAnimation(Item<?> totem, @Nullable SoundData sound, boolean silent) {
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
    public void clearTrackedBlockEntities() {
        this.trackedBlockEntityRenderers.clear();
    }

    @Override
    public int clearOrCountMatchingInventoryItems(Key itemId, int count) {
        Predicate<Object> predicate = nmsStack -> this.plugin.itemManager().wrap(ItemStackUtils.asCraftMirror(nmsStack)).id().equals(itemId);
        Object inventory = FastNMS.INSTANCE.method$Player$getInventory(serverPlayer());
        Object inventoryMenu = FastNMS.INSTANCE.field$Player$inventoryMenu(serverPlayer());
        Object craftSlots = FastNMS.INSTANCE.method$InventoryMenu$getCraftSlots(inventoryMenu);
        return FastNMS.INSTANCE.method$Inventory$clearOrCountMatchingItems(inventory, predicate, count, craftSlots);
    }

    @Override
    public GameEdition gameEdition() {
        if (this.gameEdition == null) {
            this.gameEdition = this.plugin.compatibilityManager().isBedrockPlayer(this) ? GameEdition.BEDROCK : GameEdition.JAVA;
        }
        return this.gameEdition;
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
    public WorldPosition eyePosition() {
        return LocationUtils.toWorldPosition(this.getEyeLocation());
    }

    @Override
    public Cache<Object, Boolean> receivedMapData() {
        return this.receivedMapData;
    }

    @Override
    public void playParticle(Key particleId, double x, double y, double z) {
        Particle particle = Registry.PARTICLE_TYPE.get(KeyUtils.toNamespacedKey(particleId));
        if (particle != null) {
            platformPlayer().getWorld().spawnParticle(particle, List.of(platformPlayer()), null, x, y, z, 1, 0, 0,0, 0, null, false);
        }
    }

    public Location getEyeLocation() {
        org.bukkit.entity.Player player = platformPlayer();
        Location eyeLocation = player.getEyeLocation();
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            Object mountPos = FastNMS.INSTANCE.method$Entity$getPassengerRidingPosition(FastNMS.INSTANCE.method$CraftEntity$getHandle(vehicle), serverPlayer());
            eyeLocation.set(FastNMS.INSTANCE.field$Vec3$x(mountPos), FastNMS.INSTANCE.field$Vec3$y(mountPos) + player.getEyeHeight(), FastNMS.INSTANCE.field$Vec3$z(mountPos));
        }
        return eyeLocation;
    }

    public Vec3d getEyePos() {
        org.bukkit.entity.Player player = platformPlayer();
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            Object mountPos = FastNMS.INSTANCE.method$Entity$getPassengerRidingPosition(FastNMS.INSTANCE.method$CraftEntity$getHandle(vehicle), serverPlayer());
            return new Vec3d(FastNMS.INSTANCE.field$Vec3$x(mountPos), FastNMS.INSTANCE.field$Vec3$y(mountPos) + player.getEyeHeight(), FastNMS.INSTANCE.field$Vec3$z(mountPos));
        } else {
            Location location = player.getLocation();
            return new Vec3d(location.getX(), location.getY() + player.getEyeHeight(), location.getZ());
        }
    }

    private RayTraceResult rayTrace(Location start, double range, FluidCollisionMode mode) {
        return start.getWorld().rayTraceBlocks(start, start.getDirection(), range, mode);
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
}
