package net.momirealms.craftengine.bukkit.plugin.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIdFinder;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds1_20;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds1_20_5;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.LibraryReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.*;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class BukkitNetworkManager implements NetworkManager, Listener, PluginMessageListener {
    private static BukkitNetworkManager instance;
    private static final Map<Class<?>, TriConsumer<NetWorkUser, NMSPacketEvent, Object>> NMS_PACKET_HANDLERS = new HashMap<>();
    // only for game stage for the moment
    private static BiConsumer<NetWorkUser, ByteBufPacketEvent>[] S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS;
    private static BiConsumer<NetWorkUser, ByteBufPacketEvent>[] C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS;

    private static void registerNMSPacketConsumer(final TriConsumer<NetWorkUser, NMSPacketEvent, Object> function, @Nullable Class<?> packet) {
        if (packet == null) return;
        NMS_PACKET_HANDLERS.put(packet, function);
    }

    private static void registerS2CByteBufPacketConsumer(final BiConsumer<NetWorkUser, ByteBufPacketEvent> function, int id) {
        if (id == -1) return;
        if (id < 0 || id >= S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS.length) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }
        S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS[id] = function;
    }

    private static void registerC2SByteBufPacketConsumer(final BiConsumer<NetWorkUser, ByteBufPacketEvent> function, int id) {
        if (id == -1) return;
        if (id < 0 || id >= C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS.length) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }
        C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS[id] = function;
    }

    private final TriConsumer<ChannelHandler, Object, Object> packetConsumer;
    private final TriConsumer<ChannelHandler, List<Object>, Object> packetsConsumer;
    private final TriConsumer<Channel, Object, Runnable> immediatePacketConsumer;
    private final TriConsumer<Channel, List<Object>, Runnable> immediatePacketsConsumer;
    private final BukkitCraftEngine plugin;

    private final Map<ChannelPipeline, BukkitServerPlayer> users = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitServerPlayer> onlineUsers = new ConcurrentHashMap<>();
    private final HashSet<Channel> injectedChannels = new HashSet<>();
    private BukkitServerPlayer[] onlineUserArray = new BukkitServerPlayer[0];

    private final PacketIds packetIds;

    private static final String CONNECTION_HANDLER_NAME = "craftengine_connection_handler";
    private static final String SERVER_CHANNEL_HANDLER_NAME = "craftengine_server_channel_handler";
    private static final String PLAYER_CHANNEL_HANDLER_NAME = "craftengine_player_channel_handler";
    private static final String PACKET_ENCODER = "craftengine_encoder";
    private static final String PACKET_DECODER = "craftengine_decoder";

    private static boolean hasModelEngine;
    private static boolean hasViaVersion;

    @SuppressWarnings("unchecked")
    public BukkitNetworkManager(BukkitCraftEngine plugin) {
        instance = this;
        S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS = new BiConsumer[PacketIdFinder.s2cGamePackets()];
        C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS = new BiConsumer[PacketIdFinder.c2sGamePackets()];
        Arrays.fill(S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS, Handlers.DO_NOTHING);
        Arrays.fill(C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS, Handlers.DO_NOTHING);
        hasModelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine") != null;
        hasViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
        this.plugin = plugin;
        // set up packet id
        this.packetIds = VersionHelper.isOrAbove1_20_5() ? new PacketIds1_20_5() : new PacketIds1_20();
        // register packet handlers
        this.registerPacketHandlers();
        // set up packet senders
        this.packetConsumer = FastNMS.INSTANCE::method$Connection$send;
        this.packetsConsumer = ((connection, packets, sendListener) -> {
            Object bundle = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
            this.packetConsumer.accept(connection, bundle, sendListener);
        });
        this.immediatePacketConsumer = (channel, packet, sendListener) -> {
            ChannelFuture future = channel.writeAndFlush(packet);
            if (sendListener == null) return;
            future.addListener((ChannelFutureListener) channelFuture -> {
                sendListener.run();
                if (!channelFuture.isSuccess()) {
                    channelFuture.channel().pipeline().fireExceptionCaught(channelFuture.cause());
                }
            });
        };
        this.immediatePacketsConsumer = (channel, packets, sendListener) -> {
            Object bundle = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
            this.immediatePacketConsumer.accept(channel, bundle, sendListener);
        };
        // set up mod channel
        this.plugin.javaPlugin().getServer().getMessenger().registerIncomingPluginChannel(this.plugin.javaPlugin(), MOD_CHANNEL, this);
        this.plugin.javaPlugin().getServer().getMessenger().registerOutgoingPluginChannel(this.plugin.javaPlugin(), MOD_CHANNEL);
        // Inject server channel
        try {
            Object server = FastNMS.INSTANCE.method$MinecraftServer$getServer();
            Object serverConnection = CoreReflections.field$MinecraftServer$connection.get(server);
            @SuppressWarnings("unchecked")
            List<ChannelFuture> channels = (List<ChannelFuture>) CoreReflections.field$ServerConnectionListener$channels.get(serverConnection);
            ListMonitor<ChannelFuture> monitor = new ListMonitor<>(channels, (future) -> {
                Channel channel = future.channel();
                injectServerChannel(channel);
                this.injectedChannels.add(channel);
            }, (object) -> {
            });
            CoreReflections.field$ServerConnectionListener$channels.set(serverConnection, monitor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to init server connection", e);
        }
    }

    private void registerPacketHandlers() {
        registerNMSPacketConsumer(PacketConsumers.PLAYER_INFO_UPDATE, NetworkReflections.clazz$ClientboundPlayerInfoUpdatePacket);
        registerNMSPacketConsumer(PacketConsumers.PLAYER_ACTION, NetworkReflections.clazz$ServerboundPlayerActionPacket);
        registerNMSPacketConsumer(PacketConsumers.SWING_HAND, NetworkReflections.clazz$ServerboundSwingPacket);
        registerNMSPacketConsumer(PacketConsumers.HELLO_C2S, NetworkReflections.clazz$ServerboundHelloPacket);
        registerNMSPacketConsumer(PacketConsumers.USE_ITEM_ON, NetworkReflections.clazz$ServerboundUseItemOnPacket);
        registerNMSPacketConsumer(PacketConsumers.PICK_ITEM_FROM_BLOCK, NetworkReflections.clazz$ServerboundPickItemFromBlockPacket);
        registerNMSPacketConsumer(PacketConsumers.SET_CREATIVE_SLOT, NetworkReflections.clazz$ServerboundSetCreativeModeSlotPacket);
        registerNMSPacketConsumer(PacketConsumers.LOGIN, NetworkReflections.clazz$ClientboundLoginPacket);
        registerNMSPacketConsumer(PacketConsumers.RESPAWN, NetworkReflections.clazz$ClientboundRespawnPacket);
        registerNMSPacketConsumer(PacketConsumers.SYNC_ENTITY_POSITION, NetworkReflections.clazz$ClientboundEntityPositionSyncPacket);
        registerNMSPacketConsumer(PacketConsumers.PICK_ITEM_FROM_ENTITY, NetworkReflections.clazz$ServerboundPickItemFromEntityPacket);
        registerNMSPacketConsumer(PacketConsumers.RENAME_ITEM, NetworkReflections.clazz$ServerboundRenameItemPacket);
        registerNMSPacketConsumer(PacketConsumers.SIGN_UPDATE, NetworkReflections.clazz$ServerboundSignUpdatePacket);
        registerNMSPacketConsumer(PacketConsumers.EDIT_BOOK, NetworkReflections.clazz$ServerboundEditBookPacket);
        registerNMSPacketConsumer(PacketConsumers.CUSTOM_PAYLOAD, NetworkReflections.clazz$ServerboundCustomPayloadPacket);
        registerNMSPacketConsumer(PacketConsumers.RESOURCE_PACK_RESPONSE, NetworkReflections.clazz$ServerboundResourcePackPacket);
        registerNMSPacketConsumer(PacketConsumers.ENTITY_EVENT, NetworkReflections.clazz$ClientboundEntityEventPacket);
        registerNMSPacketConsumer(PacketConsumers.MOVE_POS_AND_ROTATE_ENTITY, NetworkReflections.clazz$ClientboundMoveEntityPacket$PosRot);
        registerNMSPacketConsumer(PacketConsumers.MOVE_POS_ENTITY, NetworkReflections.clazz$ClientboundMoveEntityPacket$Pos);
        registerNMSPacketConsumer(PacketConsumers.ROTATE_HEAD, NetworkReflections.clazz$ClientboundRotateHeadPacket);
        registerNMSPacketConsumer(PacketConsumers.SET_ENTITY_MOTION, NetworkReflections.clazz$ClientboundSetEntityMotionPacket);
        registerNMSPacketConsumer(PacketConsumers.FINISH_CONFIGURATION, NetworkReflections.clazz$ClientboundFinishConfigurationPacket);
        registerNMSPacketConsumer(PacketConsumers.LOGIN_FINISHED, NetworkReflections.clazz$ClientboundLoginFinishedPacket);
        registerNMSPacketConsumer(PacketConsumers.UPDATE_TAGS, NetworkReflections.clazz$ClientboundUpdateTagsPacket);
        registerS2CByteBufPacketConsumer(PacketConsumers.LEVEL_CHUNK_WITH_LIGHT, this.packetIds.clientboundLevelChunkWithLightPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SECTION_BLOCK_UPDATE, this.packetIds.clientboundSectionBlocksUpdatePacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.BLOCK_UPDATE, this.packetIds.clientboundBlockUpdatePacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_21_4() ? PacketConsumers.LEVEL_PARTICLE_1_21_4 : (VersionHelper.isOrAbove1_20_5() ? PacketConsumers.LEVEL_PARTICLE_1_20_5 : PacketConsumers.LEVEL_PARTICLE_1_20), this.packetIds.clientboundLevelParticlesPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.LEVEL_EVENT, this.packetIds.clientboundLevelEventPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.OPEN_SCREEN_1_20_3 : PacketConsumers.OPEN_SCREEN_1_20, this.packetIds.clientboundOpenScreenPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.SET_TITLE_TEXT_1_20_3 : PacketConsumers.SET_TITLE_TEXT_1_20, this.packetIds.clientboundSetTitleTextPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.SET_SUBTITLE_TEXT_1_20_3 : PacketConsumers.SET_SUBTITLE_TEXT_1_20, this.packetIds.clientboundSetSubtitleTextPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.SET_ACTIONBAR_TEXT_1_20_3 : PacketConsumers.SET_ACTIONBAR_TEXT_1_20, this.packetIds.clientboundSetActionBarTextPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.BOSS_EVENT_1_20_3 : PacketConsumers.BOSS_EVENT_1_20, this.packetIds.clientboundBossEventPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.SYSTEM_CHAT_1_20_3 : PacketConsumers.SYSTEM_CHAT_1_20, this.packetIds.clientboundSystemChatPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.TAB_LIST_1_20_3 : PacketConsumers.TAB_LIST_1_20, this.packetIds.clientboundTabListPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.TEAM_1_20_3 : PacketConsumers.TEAM_1_20, this.packetIds.clientboundSetPlayerTeamPacket());
        registerS2CByteBufPacketConsumer(VersionHelper.isOrAbove1_20_3() ? PacketConsumers.SET_OBJECTIVE_1_20_3 : PacketConsumers.SET_OBJECTIVE_1_20, this.packetIds.clientboundSetObjectivePacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SET_SCORE_1_20_3, VersionHelper.isOrAbove1_20_3() ? this.packetIds.clientboundSetScorePacket() : -1);
        registerS2CByteBufPacketConsumer(PacketConsumers.ADD_RECIPE_BOOK, this.packetIds.clientboundRecipeBookAddPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.PLACE_GHOST_RECIPE, this.packetIds.clientboundPlaceGhostRecipePacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.UPDATE_RECIPES, this.packetIds.clientboundUpdateRecipesPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.UPDATE_ADVANCEMENTS, this.packetIds.clientboundUpdateAdvancementsPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.REMOVE_ENTITY, this.packetIds.clientboundRemoveEntitiesPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.ADD_ENTITY, this.packetIds.clientboundAddEntityPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SOUND, this.packetIds.clientboundSoundPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SET_ENTITY_DATA, this.packetIds.clientboundSetEntityDataPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.CONTAINER_SET_CONTENT, this.packetIds.clientboundContainerSetContentPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.CONTAINER_SET_SLOT, this.packetIds.clientboundContainerSetSlotPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SET_CURSOR_ITEM, this.packetIds.clientboundSetCursorItemPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SET_EQUIPMENT, this.packetIds.clientboundSetEquipmentPacket());
        registerS2CByteBufPacketConsumer(PacketConsumers.SET_PLAYER_INVENTORY_1_21_2, this.packetIds.clientboundSetPlayerInventoryPacket());
        registerC2SByteBufPacketConsumer(PacketConsumers.SET_CREATIVE_MODE_SLOT, this.packetIds.serverboundSetCreativeModeSlotPacket());
        registerC2SByteBufPacketConsumer(VersionHelper.isOrAbove1_21_5() ? PacketConsumers.CONTAINER_CLICK_1_21_5 : PacketConsumers.CONTAINER_CLICK_1_20, this.packetIds.serverboundContainerClickPacket());
        registerC2SByteBufPacketConsumer(PacketConsumers.INTERACT_ENTITY, this.packetIds.serverboundInteractPacket());
    }

    public static BukkitNetworkManager instance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer user = (BukkitServerPlayer) getUser(player);
        if (user != null) {
            user.setPlayer(player);
            this.onlineUsers.put(player.getUniqueId(), user);
            this.resetUserArray();
            if (VersionHelper.isFolia()) {
                player.getScheduler().runAtFixedRate(plugin.javaPlugin(), (t) -> user.tick(),
                        () -> {
                        }, 1, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = this.onlineUsers.remove(player.getUniqueId());
        if (serverPlayer != null) {
            this.resetUserArray();
            this.saveCooldown(player, serverPlayer.cooldown());
        }
    }

    private void saveCooldown(Player player, CooldownData cd) {
        if (cd != null && player != null) {
            try {
                byte[] data = CooldownData.toBytes(cd);
                player.getPersistentDataContainer().set(KeyUtils.toNamespacedKey(CooldownData.COOLDOWN_KEY), PersistentDataType.BYTE_ARRAY, data);
            } catch (IOException e) {
                player.getPersistentDataContainer().remove(KeyUtils.toNamespacedKey(CooldownData.COOLDOWN_KEY));
                this.plugin.logger().warn("Failed to save cooldown for player " + player.getName(), e);
            }
        }
    }

    private void resetUserArray() {
        this.onlineUserArray = this.onlineUsers.values().toArray(new BukkitServerPlayer[0]);
    }

    @Override
    public BukkitServerPlayer[] onlineUsers() {
        return this.onlineUserArray;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
    }

    @Override
    public void init() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        for (Channel channel : this.injectedChannels) {
            uninjectServerChannel(channel);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleDisconnection(getChannel(player));
        }
        this.injectedChannels.clear();
    }

    @Override
    public void setUser(Channel channel, NetWorkUser user) {
        ChannelPipeline pipeline = channel.pipeline();
        this.users.put(pipeline, (BukkitServerPlayer) user);
    }

    @Override
    public NetWorkUser getUser(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        return this.users.get(pipeline);
    }

    @Override
    public NetWorkUser removeUser(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        return this.users.remove(pipeline);
    }

    @Override
    public Channel getChannel(net.momirealms.craftengine.core.entity.player.Player player) {
        return getChannel((Player) player.platformPlayer());
    }

    public NetWorkUser getUser(Player player) {
        return getUser(getChannel(player));
    }

    public NetWorkUser getOnlineUser(Player player) {
        return this.onlineUsers.get(player.getUniqueId());
    }

    public Channel getChannel(Player player) {
        return FastNMS.INSTANCE.field$Connection$channel(
                FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(
                        FastNMS.INSTANCE.field$Player$connection(
                                FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)
                        )
                )
        );
    }

    @Override
    public void sendPacket(@NotNull NetWorkUser player, Object packet, boolean immediately, Runnable sendListener) {
        if (immediately) {
            this.immediatePacketConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            this.packetConsumer.accept(player.connection(), packet, sendListener != null ? FastNMS.INSTANCE.method$PacketSendListener$thenRun(sendListener) : null);
        }
    }

    @Override
    public void sendPackets(@NotNull NetWorkUser player, List<Object> packet, boolean immediately, Runnable sendListener) {
        if (immediately) {
            this.immediatePacketsConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            this.packetsConsumer.accept(player.connection(), packet, sendListener != null ? FastNMS.INSTANCE.method$PacketSendListener$thenRun(sendListener) : null);
        }
    }

    public static boolean hasModelEngine() {
        return hasModelEngine;
    }

    public static boolean hasViaVersion() {
        return hasViaVersion;
    }

    public void simulatePacket(@NotNull NetWorkUser player, Object packet) {
        Channel channel = player.nettyChannel();
        if (channel.isOpen()) {
            List<String> handlerNames = channel.pipeline().names();
            if (handlerNames.contains("via-encoder")) {
                channel.pipeline().context("via-decoder").fireChannelRead(packet);
            } else if (handlerNames.contains("ps_decoder_transformer")) {
                channel.pipeline().context("ps_decoder_transformer").fireChannelRead(packet);
            } else if (handlerNames.contains("decompress")) {
                channel.pipeline().context("decompress").fireChannelRead(packet);
            } else {
                if (handlerNames.contains("decrypt")) {
                    channel.pipeline().context("decrypt").fireChannelRead(packet);
                } else {
                    channel.pipeline().context("splitter").fireChannelRead(packet);
                }
            }
        } else {
            ((ByteBuf) packet).release();
        }
    }

    private void injectServerChannel(Channel serverChannel) {
        ChannelPipeline pipeline = serverChannel.pipeline();
        ChannelHandler connectionHandler = pipeline.get(CONNECTION_HANDLER_NAME);
        if (connectionHandler != null) {
            pipeline.remove(CONNECTION_HANDLER_NAME);
        }
        if (pipeline.get("SpigotNettyServerChannelHandler#0") != null) {
            pipeline.addAfter("SpigotNettyServerChannelHandler#0", CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        } else if (pipeline.get("floodgate-init") != null) {
            pipeline.addAfter("floodgate-init", CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        } else if (pipeline.get("MinecraftPipeline#0") != null) {
            pipeline.addAfter("MinecraftPipeline#0", CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        } else {
            pipeline.addFirst(CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel channel = getChannel(player);
            NetWorkUser user = getUser(player);
            if (user == null) {
                user = new BukkitServerPlayer(plugin, channel);
                ((BukkitServerPlayer) user).setPlayer(player);
                injectChannel(channel, ConnectionState.PLAY);
            }
        }
    }

    private void uninjectServerChannel(Channel channel) {
        if (channel.pipeline().get(CONNECTION_HANDLER_NAME) != null) {
            channel.pipeline().remove(CONNECTION_HANDLER_NAME);
        }
    }

    public void handleDisconnection(Channel channel) {
        NetWorkUser user = removeUser(channel);
        if (user == null) return;
        if (channel.pipeline().get(PLAYER_CHANNEL_HANDLER_NAME) != null) {
            channel.pipeline().remove(PLAYER_CHANNEL_HANDLER_NAME);
        }
        if (channel.pipeline().get(PACKET_ENCODER) != null) {
            channel.pipeline().remove(PACKET_ENCODER);
        }
        if (channel.pipeline().get(PACKET_DECODER) != null) {
            channel.pipeline().remove(PACKET_DECODER);
        }
    }

    public class ServerChannelHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object c) throws Exception {
            Channel channel = (Channel) c;
            channel.pipeline().addLast(SERVER_CHANNEL_HANDLER_NAME, new PreChannelInitializer());
            super.channelRead(context, c);
        }
    }

    public class PreChannelInitializer extends ChannelInboundHandlerAdapter {

        private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);

        @Override
        public void channelRegistered(ChannelHandlerContext context) {
            try {
                injectChannel(context.channel(), ConnectionState.HANDSHAKING);
            } catch (Throwable t) {
                exceptionCaught(context, t);
            } finally {
                ChannelPipeline pipeline = context.pipeline();
                if (pipeline.context(this) != null) {
                    pipeline.remove(this);
                }
            }
            context.pipeline().fireChannelRegistered();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable t) {
            PreChannelInitializer.logger.warn("Failed to inject channel: " + context.channel(), t);
            context.close();
        }
    }

    public void injectChannel(Channel channel, ConnectionState state) {
        if (isFakeChannel(channel)) {
            return;
        }

        BukkitServerPlayer user = new BukkitServerPlayer(plugin, channel);
        if (channel.pipeline().get("splitter") == null) {
            channel.close();
            return;
        }

        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(PACKET_ENCODER) != null) {
            pipeline.remove(PACKET_ENCODER);
        }
        if (pipeline.get(PACKET_DECODER) != null) {
            pipeline.remove(PACKET_DECODER);
        }
        for (Map.Entry<String, ChannelHandler> entry : pipeline.toMap().entrySet()) {
            if (NetworkReflections.clazz$Connection.isAssignableFrom(entry.getValue().getClass())) {
                pipeline.addBefore(entry.getKey(), PLAYER_CHANNEL_HANDLER_NAME, new PluginChannelHandler(user));
                break;
            }
        }

        String decoderName = pipeline.names().contains("inbound_config") ? "inbound_config" : "decoder";
        pipeline.addBefore(decoderName, PACKET_DECODER, new PluginChannelDecoder(user));
        String encoderName = pipeline.names().contains("outbound_config") ? "outbound_config" : "encoder";
        pipeline.addBefore(encoderName, PACKET_ENCODER, new PluginChannelEncoder(user));

        channel.closeFuture().addListener((ChannelFutureListener) future -> {
            handleDisconnection(user.nettyChannel());
        });
        setUser(channel, user);
    }

    public static boolean isFakeChannel(Object channel) {
        return channel.getClass().getSimpleName().equals("FakeChannel")
                || channel.getClass().getSimpleName().equals("SpoofedChannel");
    }

    public class PluginChannelHandler extends ChannelDuplexHandler {

        private final NetWorkUser player;

        public PluginChannelHandler(NetWorkUser player) {
            this.player = player;
        }

        @Override
        public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) throws Exception {
            try {
                NMSPacketEvent event = new NMSPacketEvent(packet);
                onNMSPacketSend(player, event, packet);
                if (event.isCancelled()) return;
                if (event.isUsingNewPacket()) {
                    super.write(context, event.optionalNewPacket(), channelPromise);
                } else {
                    super.write(context, packet, channelPromise);
                }
            } catch (Throwable e) {
                plugin.logger().severe("An error occurred when reading packets. Packet class: " + packet.getClass(), e);
                super.write(context, packet, channelPromise);
            }
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object packet) throws Exception {
            NMSPacketEvent event = new NMSPacketEvent(packet);
            onNMSPacketReceive(player, event, packet);
            if (event.isCancelled()) return;
            if (event.isUsingNewPacket()) {
                super.channelRead(context, event.optionalNewPacket());
            } else {
                super.channelRead(context, packet);
            }
        }
    }

    public class PluginChannelEncoder extends MessageToMessageEncoder<ByteBuf> {
        private final NetWorkUser player;
        private boolean handledCompression = false;

        public PluginChannelEncoder(NetWorkUser player) {
            this.player = player;
        }

        public PluginChannelEncoder(PluginChannelEncoder encoder) {
            this.player = encoder.player;
            this.handledCompression = encoder.handledCompression;
        }

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
            boolean needCompression = !handledCompression && handleCompression(channelHandlerContext, byteBuf);
            this.onByteBufSend(byteBuf);
            if (needCompression) {
                compress(channelHandlerContext, byteBuf);
            }
            if (byteBuf.isReadable()) {
                list.add(byteBuf.retain());
            } else {
                throw CancelPacketException.INSTANCE;
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (ExceptionUtils.hasException(cause, CancelPacketException.INSTANCE)) {
                return;
            }
            super.exceptionCaught(ctx, cause);
        }

        private boolean handleCompression(ChannelHandlerContext ctx, ByteBuf buffer) {
            if (this.handledCompression) return false;
            int compressIndex = ctx.pipeline().names().indexOf("compress");
            if (compressIndex == -1) return false;
            this.handledCompression = true;
            int encoderIndex = ctx.pipeline().names().indexOf(PACKET_ENCODER);
            if (encoderIndex == -1) return false;
            if (compressIndex > encoderIndex) {
                decompress(ctx, buffer, buffer);
                PluginChannelDecoder decoder = (PluginChannelDecoder) ctx.pipeline().get(PACKET_DECODER);
                if (decoder != null) {
                    if (decoder.relocated) return true;
                    decoder.relocated = true;
                }
                PluginChannelEncoder encoder = (PluginChannelEncoder) ctx.pipeline().remove(PACKET_ENCODER);
                String encoderName = ctx.pipeline().names().contains("outbound_config") ? "outbound_config" : "encoder";
                ctx.pipeline().addBefore(encoderName, PACKET_ENCODER, new PluginChannelEncoder(encoder));
                decoder = (PluginChannelDecoder) ctx.pipeline().remove(PACKET_DECODER);
                String decoderName = ctx.pipeline().names().contains("inbound_config") ? "inbound_config" : "decoder";
                ctx.pipeline().addBefore(decoderName, PACKET_DECODER, new PluginChannelDecoder(decoder));
                return true;
            }
            return false;
        }

        private void onByteBufSend(ByteBuf buffer) {
            // I don't care packets before PLAY for the moment
            if (player.encoderState() != ConnectionState.PLAY) return;
            int size = buffer.readableBytes();
            if (size != 0) {
                FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
                int preProcessIndex = buf.readerIndex();
                int packetId = buf.readVarInt();
                int preIndex = buf.readerIndex();
                try {
                    ByteBufPacketEvent event = new ByteBufPacketEvent(packetId, buf, preIndex);
                    BukkitNetworkManager.this.handleS2CByteBufPacket(this.player, event);
                    if (event.isCancelled()) {
                        buf.clear();
                    } else if (!event.changed()) {
                        buf.readerIndex(preProcessIndex);
                    }
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("An error occurred when writing packet " + packetId, e);
                    buf.readerIndex(preProcessIndex);
                }
            }
        }
    }

    public class PluginChannelDecoder extends MessageToMessageDecoder<ByteBuf> {
        private final NetWorkUser player;
        public boolean relocated = false;

        public PluginChannelDecoder(NetWorkUser player) {
            this.player = player;
        }

        public PluginChannelDecoder(PluginChannelDecoder decoder) {
            this.player = decoder.player;
            this.relocated = decoder.relocated;
        }

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
            this.onByteBufReceive(byteBuf);
            if (byteBuf.isReadable()) {
                list.add(byteBuf.retain());
            }
        }

        private void onByteBufReceive(ByteBuf buffer) {
            // I don't care packets before PLAY for the moment
            if (player.decoderState() != ConnectionState.PLAY) return;
            int size = buffer.readableBytes();
            if (size != 0) {
                FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
                int preProcessIndex = buf.readerIndex();
                int packetId = buf.readVarInt();
                int preIndex = buf.readerIndex();
                try {
                    ByteBufPacketEvent event = new ByteBufPacketEvent(packetId, buf, preIndex);
                    BukkitNetworkManager.this.handleC2SByteBufPacket(this.player, event);
                    if (event.isCancelled()) {
                        buf.clear();
                    } else if (!event.changed()) {
                        buf.readerIndex(preProcessIndex);
                    }
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("An error occurred when reading packet " + packetId, e);
                    buf.readerIndex(preProcessIndex);
                }
            }
        }
    }

    private void onNMSPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
        Debugger.PACKET.debug(() -> "[C->S]" + packet.getClass());
        handleNMSPacket(user, event, packet);
    }

    private void onNMSPacketSend(NetWorkUser player, NMSPacketEvent event, Object packet) {
        if (NetworkReflections.clazz$ClientboundBundlePacket.isInstance(packet)) {
            Iterable<Object> packets = FastNMS.INSTANCE.method$ClientboundBundlePacket$subPackets(packet);
            for (Object p : packets) {
                onNMSPacketSend(player, event, p);
            }
        } else {
            Debugger.PACKET.debug(() -> "[S->C]" + packet.getClass());
            handleNMSPacket(player, event, packet);
        }
    }

    protected void handleNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        Optional.ofNullable(NMS_PACKET_HANDLERS.get(packet.getClass()))
                .ifPresent(function -> function.accept(user, event, packet));
    }

    protected void handleS2CByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        Optional.ofNullable(S2C_GAME_BYTE_BUFFER_PACKET_HANDLERS[packetID])
                .ifPresent(function -> function.accept(user, event));
    }

    protected void handleC2SByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        Optional.ofNullable(C2S_GAME_BYTE_BUFFER_PACKET_HANDLERS[packetID])
                .ifPresent(function -> function.accept(user, event));
    }

    private void compress(ChannelHandlerContext ctx, ByteBuf input) {
        ChannelHandler compressor = ctx.pipeline().get("compress");
        ByteBuf temp = ctx.alloc().buffer();
        try {
            if (compressor != null) {
                callEncode(compressor, ctx, input, temp);
            }
        } finally {
            input.clear().writeBytes(temp);
            temp.release();
        }
    }

    private void decompress(ChannelHandlerContext ctx, ByteBuf input, ByteBuf output) {
        ChannelHandler decompressor = ctx.pipeline().get("decompress");
        if (decompressor != null) {
            ByteBuf temp = (ByteBuf) callDecode(decompressor, ctx, input).get(0);
            try {
                output.clear().writeBytes(temp);
            } finally {
                temp.release();
            }
        }
    }

    private static void callEncode(Object encoder, ChannelHandlerContext ctx, ByteBuf msg, ByteBuf output) {
        try {
            LibraryReflections.method$messageToByteEncoder$encode.invoke(encoder, ctx, msg, output);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call encode", e);
        }
    }

    public static List<Object> callDecode(Object decoder, Object ctx, Object input) {
        List<Object> output = new ArrayList<>();
        try {
            LibraryReflections.method$byteToMessageDecoder$decode.invoke(decoder, ctx, input, output);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call decode", e);
        }
        return output;
    }

    @FunctionalInterface
    public interface Handlers extends BiConsumer<NetWorkUser, ByteBufPacketEvent> {
        Handlers DO_NOTHING = doNothing();

        static Handlers doNothing() {
            return (user, byteBufPacketEvent) -> {
            };
        }
    }
}
