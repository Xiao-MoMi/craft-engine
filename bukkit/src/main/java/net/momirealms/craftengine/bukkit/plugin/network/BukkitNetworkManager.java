package net.momirealms.craftengine.bukkit.plugin.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.TotemAnimationCommand;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIdHelper;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds;
import net.momirealms.craftengine.bukkit.plugin.network.listener.common.*;
import net.momirealms.craftengine.bukkit.plugin.network.listener.configuration.FinishConfigurationListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.configuration.NMSFinishConfigurationListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.configuration.RegistryDataListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.game.*;
import net.momirealms.craftengine.bukkit.plugin.network.listener.handshake.IntentionListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.login.HelloListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.login.LoginAcknowledgedListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.login.LoginFinishedListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.status.StatusResponseListener;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.plugin.user.FakeBukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.util.TagUtils;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.AbstractNetworkManager;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.PacketFlow;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListenerHolder;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.leaves.bot.BotListProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.ConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.PacketSendListenerProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.BundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundResourcePackPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ServerboundContainerClickPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.dedicated.DedicatedServerPropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.server.dedicated.DedicatedServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.dedicated.DedicatedServerSettingsProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerCommonPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerConnectionListenerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerGamePacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagNetworkSerializationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.netty.handler.codec.ByteToMessageDecoderProxy;
import net.momirealms.craftengine.proxy.netty.handler.codec.MessageToByteEncoderProxy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class BukkitNetworkManager extends AbstractNetworkManager implements Listener {
    private static final Map<Class<?>, NMSPacketListener> nmsPacketListeners = new IdentityHashMap<>(128);
    private static final ByteBufferPacketListenerHolder[] s2cHandshakingPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.CLIENTBOUND, ConnectionState.HANDSHAKING)];
    private static final ByteBufferPacketListenerHolder[] c2sHandshakingPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.SERVERBOUND, ConnectionState.HANDSHAKING)];
    private static final ByteBufferPacketListenerHolder[] s2cStatusPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.CLIENTBOUND, ConnectionState.STATUS)];
    private static final ByteBufferPacketListenerHolder[] c2sStatusPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.SERVERBOUND, ConnectionState.STATUS)];
    private static final ByteBufferPacketListenerHolder[] s2cLoginPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.CLIENTBOUND, ConnectionState.LOGIN)];
    private static final ByteBufferPacketListenerHolder[] c2sLoginPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.SERVERBOUND, ConnectionState.LOGIN)];
    private static final ByteBufferPacketListenerHolder[] s2cPlayPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.CLIENTBOUND, ConnectionState.PLAY)];
    private static final ByteBufferPacketListenerHolder[] c2sPlayPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.SERVERBOUND, ConnectionState.PLAY)];
    private static final ByteBufferPacketListenerHolder[] s2cConfigurationPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.CLIENTBOUND, ConnectionState.CONFIGURATION)];
    private static final ByteBufferPacketListenerHolder[] c2sConfigurationPacketListeners = new ByteBufferPacketListenerHolder[PacketIdHelper.count(PacketFlow.SERVERBOUND, ConnectionState.CONFIGURATION)];
    private static final ByteBufferPacketListenerHolder[][] s2cPacketListeners = new ByteBufferPacketListenerHolder[][]{
            s2cHandshakingPacketListeners,
            s2cStatusPacketListeners,
            s2cLoginPacketListeners,
            s2cPlayPacketListeners,
            s2cConfigurationPacketListeners
    };
    private static final ByteBufferPacketListenerHolder[][] c2sPacketListeners = new ByteBufferPacketListenerHolder[][]{
            c2sHandshakingPacketListeners,
            c2sStatusPacketListeners,
            c2sLoginPacketListeners,
            c2sPlayPacketListeners,
            c2sConfigurationPacketListeners
    };
    private static final String CONNECTION_HANDLER_NAME = "craftengine_connection_handler";
    private static final String SERVER_CHANNEL_HANDLER_NAME = "craftengine_server_channel_handler";
    private static final String PLAYER_CHANNEL_HANDLER_NAME = "craftengine_player_channel_handler";
    private static final String PACKET_ENCODER = "craftengine_encoder";
    private static final String PACKET_DECODER = "craftengine_decoder";
    private static BukkitNetworkManager instance;
    private final BukkitCraftEngine plugin;
    private final TriConsumer<ChannelHandler, Object, Object> packetConsumer;
    private final TriConsumer<ChannelHandler, List<Object>, Object> packetsConsumer;
    private final TriConsumer<Channel, Object, Runnable> immediatePacketConsumer;
    private final TriConsumer<Channel, List<Object>, Runnable> immediatePacketsConsumer;
    private final Map<ChannelPipeline, BukkitServerPlayer> users = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitServerPlayer> onlineUsers = new ConcurrentHashMap<>();
    private final HashSet<Channel> injectedChannels = new HashSet<>();
    private final boolean hasAntiPopup;
    private BukkitServerPlayer[] onlineUserArray = new BukkitServerPlayer[0];
    private int[] blockStateRemapper;
    private int[] modBlockStateRemapper;

    public BukkitNetworkManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.hasAntiPopup = Bukkit.getPluginManager().getPlugin("AntiPopup") != null;
        this.plugin = plugin;
        // register packet handlers
        this.registerPacketListeners();
        // set up packet senders
        this.packetConsumer = VersionHelper.isOrAbove1_21_6
                ? (target, packet, sendListener) -> ConnectionProxy.INSTANCE.send$0(target, packet, (ChannelFutureListener) sendListener)
                : ConnectionProxy.INSTANCE::send$1;
        this.packetsConsumer = (connection, packets, sendListener) -> {
            Object bundle = ClientboundBundlePacketProxy.INSTANCE.newInstance(packets);
            this.packetConsumer.accept(connection, bundle, sendListener);
        };
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
            Object bundle = ClientboundBundlePacketProxy.INSTANCE.newInstance(packets);
            this.immediatePacketConsumer.accept(channel, bundle, sendListener);
        };
        // Inject server channel
        {
            Object server = MinecraftServerProxy.INSTANCE.getServer();
            Object serverConnection = MinecraftServerProxy.INSTANCE.getConnection(server);
            List<ChannelFuture> channels = ServerConnectionListenerProxy.INSTANCE.getChannels(serverConnection);
            ListMonitor<ChannelFuture> monitor = new ListMonitor<>(channels, (future) -> {
                Channel channel = future.channel();
                injectServerChannel(channel);
                this.injectedChannels.add(channel);
            }, (object) -> {
            });
            ServerConnectionListenerProxy.INSTANCE.setChannels(serverConnection, monitor);
        }
        // Inject Leaves bot list
        if (VersionHelper.isLeaves) {
            this.injectLeavesBotList();
        }
    }

    public static BukkitNetworkManager instance() {
        return instance;
    }

    private static void registerNMSPacketConsumer(@Nullable NMSPacketListener listener, @Nullable Class<?> packet) {
        if (listener == null || packet == null) return;
        nmsPacketListeners.put(packet, listener);
    }

    private static void registerByteBufferPacketListener(@Nullable ByteBufferPacketListener listener, int id, String name, ConnectionState state, PacketFlow direction) {
        if (listener == null || id == -1) return;
        ByteBufferPacketListenerHolder[] listeners = direction == PacketFlow.SERVERBOUND ? c2sPacketListeners[state.ordinal()] : s2cPacketListeners[state.ordinal()];
        if (id < 0 || id >= listeners.length) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }
        listeners[id] = new ByteBufferPacketListenerHolder(name, listener);
    }

    private static void uninjectServerChannel(Channel channel) {
        if (channel.pipeline().get(CONNECTION_HANDLER_NAME) != null) {
            channel.pipeline().remove(CONNECTION_HANDLER_NAME);
        }
    }

    // 再次进行重定位保证和 packetevents 的相对位置
    public static void relocateChannelHandler(Channel channel) {
        if (channel == null || isFakeChannel(channel)) return;

        ChannelPipeline pipeline = channel.pipeline();
        int encoderIndex = pipeline.names().indexOf(PACKET_ENCODER);
        if (encoderIndex == -1) return;

        PluginChannelEncoder encoder = (PluginChannelEncoder) pipeline.remove(PACKET_ENCODER);
        PluginChannelDecoder decoder = (PluginChannelDecoder) pipeline.remove(PACKET_DECODER);
        addToPipeline(pipeline, encoder, decoder);
    }

    private static void addToPipeline(ChannelPipeline pipeline, PluginChannelEncoder encoder, PluginChannelDecoder decoder) {
        boolean addedDecoder = false;
        String lastPEEncoderName = null;
        List<String> names = pipeline.names();
        for (String name : names) {
            if (!addedDecoder) {
                if (name.startsWith("pe-decoder-")) {
                    pipeline.addBefore(name, PACKET_DECODER, decoder);
                    addedDecoder = true;
                } else if (name.equals("inbound_config") || name.equals("decoder")) {
                    pipeline.addBefore(name, PACKET_DECODER, decoder);
                    addedDecoder = true;
                }
            } else {
                if (name.startsWith("pe-encoder-")) {
                    lastPEEncoderName = name;
                }
            }
        }

        if (lastPEEncoderName != null) {
            pipeline.addAfter(lastPEEncoderName, PACKET_ENCODER, encoder);
        } else {
            String encoderName = pipeline.names().contains("outbound_config") ? "outbound_config" : "encoder";
            pipeline.addBefore(encoderName, PACKET_ENCODER, encoder);
        }

        Debugger.PACKET.debug(() -> "pipelines: " + pipeline.names());
    }

    private static boolean isFakeChannel(Object channel) {
        return channel.getClass().getSimpleName().equals("FakeChannel")
                || channel.getClass().getSimpleName().equals("SpoofedChannel");
    }

    private static void compress(ChannelHandlerContext ctx, ByteBuf input) {
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

    private static void decompress(ChannelHandlerContext ctx, ByteBuf input, ByteBuf output) {
        ChannelHandler decompressor = ctx.pipeline().get("decompress");
        if (decompressor != null) {
            ByteBuf temp = (ByteBuf) callDecode(decompressor, ctx, input).getFirst();
            try {
                output.clear().writeBytes(temp);
            } finally {
                temp.release();
            }
        }
    }

    private static void callEncode(Object encoder, ChannelHandlerContext ctx, ByteBuf msg, ByteBuf output) {
        MessageToByteEncoderProxy.INSTANCE.encode(encoder, ctx, msg, output);
    }

    private static List<Object> callDecode(Object decoder, ChannelHandlerContext ctx, ByteBuf input) {
        List<Object> output = new ArrayList<>();
        ByteToMessageDecoderProxy.INSTANCE.decode(decoder, ctx, input, output);
        return output;
    }

    @Override
    public void init() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        if (Config.disableChatReport()) {
            updateEnforceSecureProfile();
        }
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
    public int remapBlockState(int stateId, boolean enableMod) {
        return enableMod ? this.modBlockStateRemapper[stateId] : this.blockStateRemapper[stateId];
    }

    @Override
    public void delayedLoad() {
        super.delayedLoad();
        this.resendTags();
    }

    public void resendTags() {
        Object packet = TagUtils.createUpdateTagsPacket(
                Map.of(RegistriesProxy.BLOCK, BukkitBlockManager.instance().cachedUpdateTags()),
                TagNetworkSerializationProxy.INSTANCE.serializeTagsToNetwork(MinecraftServerProxy.INSTANCE.registries(MinecraftServerProxy.INSTANCE.getServer()))
        );
        for (BukkitServerPlayer player : onlineUsers()) {
            player.sendPacket(packet, false);
        }
    }

    public void addFakePlayer(Player player) {
        FakeBukkitServerPlayer fakePlayer = new FakeBukkitServerPlayer(this.plugin);
        fakePlayer.setConnectionState(ConnectionState.PLAY);
        fakePlayer.setPlayer(player);
        this.onlineUsers.put(player.getUniqueId(), fakePlayer);
        this.resetUserArray();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeFakePlayer(Player player) {
        BukkitServerPlayer fakePlayer = this.onlineUsers.get(player.getUniqueId());
        if (!(fakePlayer instanceof FakeBukkitServerPlayer)) {
            return false;
        }
        this.onlineUsers.remove(player.getUniqueId());
        this.resetUserArray();
        this.saveCooldown(player, fakePlayer.cooldown());
        return true;
    }

    private void injectLeavesBotList() {
        Object botList = BotListProxy.INSTANCE.getInstance();
        List<Object> bots = BotListProxy.INSTANCE.getBots(botList);
        ListMonitor<Object> monitor = new ListMonitor<>(bots,
                (bot) -> addFakePlayer(ServerPlayerProxy.INSTANCE.getBukkitEntity(bot)),
                (bot) -> removeFakePlayer(ServerPlayerProxy.INSTANCE.getBukkitEntity(bot))
        );
        BotListProxy.INSTANCE.setBots(botList, monitor);
    }

    public void registerBlockStatePacketListeners(int[] blockStateMappings, Predicate<Integer> occlusionPredicate) {
        int stoneId = BlockStateUtils.blockStateToId(BlocksProxy.STONE$defaultState);
        int vanillaBlocks = BlockStateUtils.vanillaBlockStateCount();
        int[] newMappings = new int[blockStateMappings.length];
        int[] newMappingsMOD = new int[blockStateMappings.length];
        for (int i = 0; i < vanillaBlocks; i++) {
            int mappedId = blockStateMappings[i];
            if (mappedId != -1) {
                newMappings[i] = mappedId;
                newMappingsMOD[i] = mappedId;
            } else {
                newMappings[i] = i;
                newMappingsMOD[i] = i;
            }
        }
        for (int i = vanillaBlocks; i < blockStateMappings.length; i++) {
            int mappedId = blockStateMappings[i];
            if (mappedId != -1) {
                newMappings[i] = mappedId;
            } else {
                newMappings[i] = stoneId;
            }
            newMappingsMOD[i] = i;
        }
        this.blockStateRemapper = newMappings;
        this.modBlockStateRemapper = newMappingsMOD;
        registerByteBufferPacketListener(new LevelChunkWithLightListener(
                newMappings,
                newMappingsMOD,
                newMappings.length,
                RegistryUtils.currentBiomeRegistrySize(),
                occlusionPredicate
        ), PacketIds.INSTANCE.clientboundLevelChunkWithLightPacket(), "ClientboundLevelChunkWithLightPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(new SectionBlocksUpdateListener(
                newMappings,
                newMappingsMOD,
                occlusionPredicate
        ), PacketIds.INSTANCE.clientboundSectionBlocksUpdatePacket(), "ClientboundSectionBlocksUpdatePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(new BlockUpdateListener(
                newMappings,
                newMappingsMOD,
                occlusionPredicate
        ), PacketIds.INSTANCE.clientboundBlockUpdatePacket(), "ClientboundBlockUpdatePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(new LevelParticleListener(
                newMappings,
                newMappingsMOD
        ), PacketIds.INSTANCE.clientboundLevelParticlesPacket(), "ClientboundLevelParticlesPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(new LevelEventListener(
                newMappings,
                newMappingsMOD
        ), PacketIds.INSTANCE.clientboundLevelEventPacket(), "ClientboundLevelEventPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
    }

    private void registerPacketListeners() {
        // nms - 需要在服务器处理前处理的请放这里
        registerNMSPacketConsumer(NMSContainerClickListener.INSTANCE, ServerboundContainerClickPacketProxy.CLASS);
        registerNMSPacketConsumer(NMSFinishConfigurationListener.INSTANCE, ClientboundFinishConfigurationPacketProxy.CLASS);
        registerNMSPacketConsumer(NMSResourcePackListener.INSTANCE, ServerboundResourcePackPacketProxy.CLASS);
        // bytebuffer
        // 状态切换相关监听器 - 开始
        registerByteBufferPacketListener(FinishConfigurationListener.INSTANCE, PacketIds.INSTANCE.serverboundFinishConfigurationPacket(), "ServerboundFinishConfigurationPacket", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND); // 1.20.2+ s2c to play (configuration)
        registerByteBufferPacketListener(LoginListener.INSTANCE, PacketIds.INSTANCE.clientboundLoginPacket(), "ClientboundLoginPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND); // 1.20.2+ c2s to play (configuration -> play)
        registerByteBufferPacketListener(LoginAcknowledgedListener.INSTANCE, PacketIds.INSTANCE.serverboundLoginAcknowledgedPacket(), "ServerboundLoginAcknowledgedPacket", ConnectionState.LOGIN, PacketFlow.SERVERBOUND); // 1.20.2+ to configuration (login)
        registerByteBufferPacketListener(LoginFinishedListener.INSTANCE, PacketIds.INSTANCE.clientboundLoginFinishedPacket(), "ClientboundLoginFinishedPacket", ConnectionState.LOGIN, PacketFlow.CLIENTBOUND); // 1.20.1 to play (login)
        registerByteBufferPacketListener(StartConfigurationListener.INSTANCE, PacketIds.INSTANCE.clientboundStartConfigurationPacket(), "ClientboundStartConfigurationPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND); // 1.20.2+ s2c to configuration (play)
        registerByteBufferPacketListener(ConfigurationAcknowledgedListener.INSTANCE, PacketIds.INSTANCE.serverboundConfigurationAcknowledgedPacket(), "ServerboundConfigurationAcknowledgedPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND); // 1.20.2+ c2s to configuration (play)
        registerByteBufferPacketListener(IntentionListener.INSTANCE, PacketIds.INSTANCE.clientIntentionPacket(), "ClientIntentionPacket", ConnectionState.HANDSHAKING, PacketFlow.SERVERBOUND); // to status or login (handshaking)
        // 状态切换相关监听器 - 结束
        registerByteBufferPacketListener(PlayerInfoUpdateListener.INSTANCE, PacketIds.INSTANCE.clientboundPlayerInfoUpdatePacket(), "ClientboundPlayerInfoUpdatePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ClientInformationListener.INSTANCE, PacketIds.INSTANCE.serverboundClientInformationPacket$play(), "ServerboundClientInformationPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(ClientInformationListener.INSTANCE, PacketIds.INSTANCE.serverboundClientInformationPacket$configuration(), "ServerboundClientInformationPacket", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(RespawnListener.INSTANCE, PacketIds.INSTANCE.clientboundRespawnPacket(), "ClientboundRespawnPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(EntityPositionSyncListener.INSTANCE, PacketIds.INSTANCE.clientboundEntityPositionSyncPacket(), "ClientboundEntityPositionSyncPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(MoveEntityPosRotListener.INSTANCE, PacketIds.INSTANCE.clientboundMoveEntityPacket$PosRot(), "ClientboundMoveEntityPacket$PosRot", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(MoveEntityPosListener.INSTANCE, PacketIds.INSTANCE.clientboundMoveEntityPacket$Pos(), "ClientboundMoveEntityPacket$Pos", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(RenameItemListener.INSTANCE, PacketIds.INSTANCE.serverboundRenameItemPacket(), "ServerboundRenameItemPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(SignUpdateListener.INSTANCE, PacketIds.INSTANCE.serverboundSignUpdatePacket(), "ServerboundSignUpdatePacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(EditBookListener.INSTANCE, PacketIds.INSTANCE.serverboundEditBookPacket(), "ServerboundEditBookPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(EntityEventListener.INSTANCE, PacketIds.INSTANCE.clientboundEntityEventPacket(), "ClientboundEntityEventPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(UpdateTagsListener.INSTANCE, PacketIds.INSTANCE.clientboundUpdateTagsPacket$play(), "ClientboundUpdateTagsPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(UpdateTagsListener.INSTANCE, PacketIds.INSTANCE.clientboundupdatetagspacket$configuration(), "ClientboundUpdateTagsPacket", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(PlayerActionListener.INSTANCE, PacketIds.INSTANCE.serverboundPlayerActionPacket(), "ServerboundPlayerActionPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(SwingListener.INSTANCE, PacketIds.INSTANCE.serverboundSwingPacket(), "ServerboundSwingPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(HelloListener.INSTANCE, PacketIds.INSTANCE.serverboundHelloPacket(), "ServerboundHelloPacket", ConnectionState.LOGIN, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(UseItemOnListener.INSTANCE, PacketIds.INSTANCE.serverboundUseItemOnPacket(), "ServerboundUseItemOnPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(PickItemFromBlockListener.INSTANCE, PacketIds.INSTANCE.serverboundPickItemFromBlockPacket(), "ServerboundPickItemFromBlockPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(PickItemFromEntityListener.INSTANCE, PacketIds.INSTANCE.serverboundPickItemFromEntityPacket(), "ServerboundPickItemFromEntityPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(ServerDataListener.INSTANCE, PacketIds.INSTANCE.clientboundServerDataPacket(), "ClientboundServerDataPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ChatSessionUpdateListener.INSTANCE, PacketIds.INSTANCE.serverboundChatSessionUpdatePacket(), "ServerboundChatSessionUpdatePacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(CustomChatCompletionsListener.INSTANCE, PacketIds.INSTANCE.clientboundCustomChatCompletionsPacket(), "ClientboundCustomChatCompletionsPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(StatusResponseListener.INSTANCE, PacketIds.INSTANCE.clientboundStatusResponsePacket(), "ClientboundStatusResponsePacket", ConnectionState.STATUS, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ForgetLevelChunkListener.INSTANCE, PacketIds.INSTANCE.clientboundForgetLevelChunkPacket(), "ClientboundForgetLevelChunkPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetScoreListener.INSTANCE, PacketIds.INSTANCE.clientboundSetScorePacket(), "ClientboundSetScorePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(RecipeBookAddListener.INSTANCE, PacketIds.INSTANCE.clientboundRecipeBookAddPacket(), "ClientboundRecipeBookAddPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(PlaceGhostRecipeListener.INSTANCE, PacketIds.INSTANCE.clientboundPlaceGhostRecipePacket(), "ClientboundPlaceGhostRecipePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(UpdateAdvancementsListener.INSTANCE, PacketIds.INSTANCE.clientboundUpdateAdvancementsPacket(), "ClientboundUpdateAdvancementsPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(RemoveEntitiesListener.INSTANCE, PacketIds.INSTANCE.clientboundRemoveEntitiesPacket(), "ClientboundRemoveEntitiesPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SoundListener.INSTANCE, PacketIds.INSTANCE.clientboundSoundPacket(), "ClientboundSoundPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ContainerSetContentListener.INSTANCE, PacketIds.INSTANCE.clientboundContainerSetContentPacket(), "ClientboundContainerSetContentPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ContainerSetSlotListener.INSTANCE, PacketIds.INSTANCE.clientboundContainerSetSlotPacket(), "ClientboundContainerSetSlotPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetCursorItemListener.INSTANCE, PacketIds.INSTANCE.clientboundSetCursorItemPacket(), "ClientboundSetCursorItemPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetEquipmentListener.INSTANCE, PacketIds.INSTANCE.clientboundSetEquipmentPacket(), "ClientboundSetEquipmentPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetPlayerInventoryListener.INSTANCE, PacketIds.INSTANCE.clientboundSetPlayerInventoryPacket(), "ClientboundSetPlayerInventoryPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetEntityDataListener.INSTANCE, PacketIds.INSTANCE.clientboundSetEntityDataPacket(), "ClientboundSetEntityDataPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetCreativeModeSlotListener.INSTANCE, PacketIds.INSTANCE.serverboundSetCreativeModeSlotPacket(), "ServerboundSetCreativeModeSlotPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(ContainerClickListener.INSTANCE, PacketIds.INSTANCE.serverboundContainerClickPacket(), "ServerboundContainerClickPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(AttackListener.INSTANCE, PacketIds.INSTANCE.serverboundAttackPacket(), "ServerboundAttackPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(AddEntityListener.INSTANCE, PacketIds.INSTANCE.clientboundAddEntityPacket(), "ClientboundAddEntityPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(BlockEntityDataListener.INSTANCE, PacketIds.INSTANCE.clientboundBlockEntityDataPacket(), "ClientboundBlockEntityDataPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(CustomPayloadListener.INSTANCE, PacketIds.INSTANCE.serverboundCustomPayloadPacket$play(), "ServerboundCustomPayloadPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(CustomPayloadListener.INSTANCE, PacketIds.INSTANCE.serverboundCustomPayloadPacket$configuration(), "ServerboundCustomPayloadPacket", ConnectionState.CONFIGURATION, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(CustomPayloadListener.INSTANCE, PacketIds.INSTANCE.clientboundCustomPayloadPacket$play(), "ClientboundCustomPayloadPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(CustomPayloadListener.INSTANCE, PacketIds.INSTANCE.clientboundCustomPayloadPacket$configuration(), "ClientboundCustomPayloadPacket", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(InteractListener.INSTANCE, PacketIds.INSTANCE.serverboundInteractPacket(), "ServerboundInteractPacket", ConnectionState.PLAY, PacketFlow.SERVERBOUND);
        registerByteBufferPacketListener(UpdateRecipesListener.INSTANCE, PacketIds.INSTANCE.clientboundUpdateRecipesPacket(), "ClientboundUpdateRecipesPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(MerchantOffersListener.INSTANCE, PacketIds.INSTANCE.clientBoundMerchantOffersPacket(), "ClientboundMerchantOffersPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(OpenScreenListener.INSTANCE, PacketIds.INSTANCE.clientboundOpenScreenPacket(), "ClientboundOpenScreenPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SystemChatListener.INSTANCE, PacketIds.INSTANCE.clientboundSystemChatPacket(), "ClientboundSystemChatPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetActionBarTextListener.INSTANCE, PacketIds.INSTANCE.clientboundSetActionBarTextPacket(), "ClientboundSetActionBarTextPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(TabListListener.INSTANCE, PacketIds.INSTANCE.clientboundTabListPacket(), "ClientboundTabListPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetTitleTextListener.INSTANCE, PacketIds.INSTANCE.clientboundSetTitleTextPacket(), "ClientboundSetTitleTextPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetSubtitleTextListener.INSTANCE, PacketIds.INSTANCE.clientboundSetSubtitleTextPacket(), "ClientboundSetSubtitleTextPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(BossEventListener.INSTANCE, PacketIds.INSTANCE.clientboundBossEventPacket(), "ClientboundBossEventPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetPlayerTeamListener.INSTANCE, PacketIds.INSTANCE.clientboundSetPlayerTeamPacket(), "ClientboundSetPlayerTeamPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(SetObjectiveListener.INSTANCE, PacketIds.INSTANCE.clientboundSetObjectivePacket(), "ClientboundSetObjectivePacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(PlayerChatListener.INSTANCE, PacketIds.INSTANCE.clientboundPlayerChatPacket(), "ClientboundPlayerChatPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(RegistryDataListener.INSTANCE, PacketIds.INSTANCE.clientboundRegistryDataPacket(), "ClientboundRegistryDataPacket", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ShowDialogListener.INSTANCE, PacketIds.INSTANCE.clientboundShowDialogPacket$play(), "ClientboundShowDialogPacket", ConnectionState.PLAY, PacketFlow.CLIENTBOUND);
        registerByteBufferPacketListener(ShowDialogListener.INSTANCE, PacketIds.INSTANCE.clientboundShowDialogPacket$configuration(), "ClientboundShowDialogPacket", ConnectionState.CONFIGURATION, PacketFlow.CLIENTBOUND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer user = (BukkitServerPlayer) getUser(player);
        if (user != null) {
            user.setPlayer(player);
            this.onlineUsers.put(player.getUniqueId(), user);
            this.resetUserArray();
            // folia在此tick每个玩家
            if (VersionHelper.isFolia) {
                player.getScheduler().runAtFixedRate(plugin.javaPlugin(), (t) -> user.tick(), null, 1, 1);
            }
            // 发送修复图腾音效
            user.sendPacket(TotemAnimationCommand.FIX_TOTEM_SOUND_PACKET, false);
            // 发送颜色队伍
            for (Object packet : BukkitTeamManager.instance().addTeamsPackets()) {
                user.sendPacket(packet, false);
            }
            Channel channel = user.nettyChannel();
            if (this.hasAntiPopup && Config.disableChatReport() && channel != null) {
                if (Locale.getDefault() == Locale.SIMPLIFIED_CHINESE) {
                    plugin.logger().warn("CraftEngine 的禁用聊天举报功能和 AntiPopup 冲突，可能会导致 Emoji 解析异常，请卸载 AntiPopup 或关闭禁用聊天举报功能");
                } else {
                    plugin.logger().warn("The Disable Chat Report feature conflicts with AntiPopup, potentially causing abnormal emoji parsing.");
                    plugin.logger().warn("Please uninstall AntiPopup or disable the 'disable-chat-report' option.");
                }
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

    private void updateEnforceSecureProfile() {
        // 更新聊天验证
        Object settings = DedicatedServerProxy.INSTANCE.getSettings(MinecraftServerProxy.INSTANCE.getServer());
        Object properties = DedicatedServerSettingsProxy.INSTANCE.getProperties(settings);
        DedicatedServerPropertiesProxy.INSTANCE.setEnforceSecureProfile(properties, false);
    }

    @Override
    public void setUser(Channel channel, NetWorkUser user) {
        ChannelPipeline pipeline = channel.pipeline();
        this.users.put(pipeline, (BukkitServerPlayer) user);
    }

    @Override
    public NetWorkUser getUser(@NotNull Channel channel) {
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

    @Override
    @Nullable
    public NetWorkUser getOnlineUser(UUID uuid) {
        return this.onlineUsers.get(uuid);
    }

    @Nullable
    public NetWorkUser getUser(Player player) {
        return getUser(getChannel(player));
    }

    // 当假人的时候channel为null
    @NotNull
    public Channel getChannel(Player player) {
        SimpleChannelInboundHandler<Object> connection;
        if (VersionHelper.isOrAbove1_20_2) {
            connection = ServerCommonPacketListenerImplProxy.INSTANCE.getConnection(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)));
        } else {
            connection = ServerGamePacketListenerImplProxy.INSTANCE.getConnection(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)));
        }
        return ConnectionProxy.INSTANCE.getChannel(connection);
    }

    @Override
    public void sendPacket(@NotNull NetWorkUser player, Object packet, boolean immediately, Runnable sendListener) {
        if (player.isFakePlayer()) return;
        if (immediately) {
            this.immediatePacketConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            if (VersionHelper.isOrAbove1_21_6) {
                this.packetConsumer.accept(player.connection(), packet, sendListener != null ? (ChannelFutureListener) $ -> sendListener.run() : null);
            } else {
                this.packetConsumer.accept(player.connection(), packet, sendListener != null ? PacketSendListenerProxy.INSTANCE.thenRun(sendListener) : null);
            }
        }
    }

    @Override
    public void sendPackets(@NotNull NetWorkUser player, List<Object> packet, boolean immediately, Runnable sendListener) {
        if (player.isFakePlayer()) return;
        if (immediately) {
            this.immediatePacketsConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            if (VersionHelper.isOrAbove1_21_6) {
                this.packetsConsumer.accept(player.connection(), packet, sendListener != null ? (ChannelFutureListener) $ -> sendListener.run() : null);
            } else {
                this.packetsConsumer.accept(player.connection(), packet, sendListener != null ? PacketSendListenerProxy.INSTANCE.thenRun(sendListener) : null);
            }
        }
    }

    @Override
    public void simulatePacket(@NotNull NetWorkUser player, Object packet) {
        Channel channel = player.nettyChannel();
        if (channel != null && channel.isOpen()) {
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
    }

    private void handleDisconnection(Channel channel) {
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

    public void injectChannel(Channel channel) {
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
            if (ConnectionProxy.CLASS.isAssignableFrom(entry.getValue().getClass())) {
                pipeline.addBefore(entry.getKey(), PLAYER_CHANNEL_HANDLER_NAME, new PluginChannelHandler(user));
                break;
            }
        }

        addToPipeline(pipeline, new PluginChannelEncoder(user), new PluginChannelDecoder(user));
        channel.closeFuture().addListener((ChannelFutureListener) future -> handleDisconnection(user.nettyChannel()));
        setUser(channel, user);
    }

    private void onNMSPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (VersionHelper.IS_RUNNING_IN_DEV) {
            Debugger.PACKET.debug(() -> {
                if (Config.isPacketIgnored(packet.getClass())) {
                    return null;
                }
                return "[C->S]" + packet.getClass();
            });
        }
        handleReceiveNMSPacket(user, event, packet);
    }

    private void onNMSPacketSend(NetWorkUser player, NMSPacketEvent event, Object packet) {
        if (ClientboundBundlePacketProxy.CLASS.isInstance(packet)) {
            Iterable<Object> packets = BundlePacketProxy.INSTANCE.getPackets(packet);
            for (Object p : packets) {
                onNMSPacketSend(player, event, p);
            }
        } else {
            if (VersionHelper.IS_RUNNING_IN_DEV) {
                Debugger.PACKET.debug(() -> {
                    if (Config.isPacketIgnored(packet.getClass())) {
                        return null;
                    }
                    return "[S->C]" + packet.getClass();
                });
            }
            handleSendNMSPacket(player, event, packet);
        }
    }

    private void handleReceiveNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        NMSPacketListener nmsPacketListener = nmsPacketListeners.get(packet.getClass());
        if (nmsPacketListener != null) {
            try {
                nmsPacketListener.onPacketReceive(user, event, packet);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + packet.getClass(), t);
            }
        }
    }

    private void handleSendNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        NMSPacketListener nmsPacketListener = nmsPacketListeners.get(packet.getClass());
        if (nmsPacketListener != null) {
            try {
                nmsPacketListener.onPacketSend(user, event, packet);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + packet.getClass(), t);
            }
        }
    }

    // outbound(encode|s2c)
    private void handleS2CByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        ByteBufferPacketListenerHolder[] listener = s2cPacketListeners[user.encoderState().ordinal()];
        if (packetID >= listener.length) {
            Debugger.PACKET.debug(() -> "Failed to convert the packet " + packetID + " for player " + user.name() +
                    ". Packet Flow: S->C, Encoder State: " + user.decoderState() + ", " +
                    "Server version: " + VersionHelper.MINECRAFT_VERSION.version() + ", Bytes: " + Arrays.toString(event.getBuffer().array()));
            return;
        }
        ByteBufferPacketListenerHolder holder = listener[packetID];
        if (holder != null) {
            try {
                holder.listener().onPacketSend(user, event);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + holder.id(), t);
            }
        }
    }

    // inbound(decode|c2s)
    private void handleC2SByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        ByteBufferPacketListenerHolder[] listener = c2sPacketListeners[user.decoderState().ordinal()];
        if (packetID >= listener.length) {
            Debugger.PACKET.debug(() -> "Failed to convert the packet " + packetID + " for player " + user.name() +
                    ". Packet Flow: C->S, Decoder State: " + user.decoderState() + ", " +
                    "Server version: " + VersionHelper.MINECRAFT_VERSION.version() + ", Bytes: " + event.getBuffer());
            return;
        }
        ByteBufferPacketListenerHolder holder = listener[packetID];
        if (holder != null) {
            try {
                holder.listener().onPacketReceive(user, event);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + holder.id(), t);
            }
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
                injectChannel(context.channel());
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
                plugin.logger().error("An error occurred when reading packets. Packet class: " + packet.getClass(), e);
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

    @ChannelHandler.Sharable
    public class PluginChannelEncoder extends MessageToMessageEncoder<ByteBuf> {
        private final NetWorkUser player;
        private boolean handledCompression = false;

        public PluginChannelEncoder(NetWorkUser player) {
            this.player = player;
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
            ChannelPipeline pipeline = ctx.pipeline();
            int compressIndex = pipeline.names().indexOf("compress");
            if (compressIndex == -1) return false;
            this.handledCompression = true;
            int encoderIndex = pipeline.names().indexOf(PACKET_ENCODER);
            if (encoderIndex == -1) return false;
            if (compressIndex > encoderIndex) {
                decompress(ctx, buffer, buffer);
                PluginChannelDecoder decoder = (PluginChannelDecoder) pipeline.get(PACKET_DECODER);
                if (decoder != null) {
                    if (decoder.relocated) return true;
                    decoder.relocated = true;
                }
                PluginChannelEncoder encoder = (PluginChannelEncoder) pipeline.remove(PACKET_ENCODER);
                decoder = (PluginChannelDecoder) ctx.pipeline().remove(PACKET_DECODER);
                addToPipeline(ctx.pipeline(), encoder, decoder);
                return true;
            }
            return false;
        }

        private void onByteBufSend(ByteBuf buffer) {
            if (buffer.readableBytes() == 0) {
                return;
            }
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

    @ChannelHandler.Sharable
    public class PluginChannelDecoder extends MessageToMessageDecoder<ByteBuf> {
        private final NetWorkUser player;
        public boolean relocated = false;

        public PluginChannelDecoder(NetWorkUser player) {
            this.player = player;
        }

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
            this.onByteBufReceive(byteBuf);
            if (byteBuf.isReadable()) {
                list.add(byteBuf.retain());
            }
        }

        private void onByteBufReceive(ByteBuf buffer) {
            if (buffer.readableBytes() == 0) {
                return;
            }
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
