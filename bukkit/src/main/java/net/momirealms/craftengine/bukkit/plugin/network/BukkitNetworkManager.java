package net.momirealms.craftengine.bukkit.plugin.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.impl.*;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.ListMonitor;
import net.momirealms.craftengine.core.util.TriConsumer;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class BukkitNetworkManager implements NetworkManager, Listener {
    private static BukkitNetworkManager instance;
    private static final Map<Class<?>, TriConsumer<NetWorkUser, NMSPacketEvent, Object>> nmsPacketFunctions = new HashMap<>();
    private static final Map<Integer, BiConsumer<NetWorkUser, ByteBufPacketEvent>> byteBufPacketFunctions = new HashMap<>();

    private static void registerNMSPacketConsumer(final TriConsumer<NetWorkUser, NMSPacketEvent, Object> function, @Nullable Class<?> packet) {
        if (packet == null) return;
        nmsPacketFunctions.put(packet, function);
    }

    private static void registerByteBufPacketConsumer(final BiConsumer<NetWorkUser, ByteBufPacketEvent> function, int id) {
        byteBufPacketFunctions.put(id, function);
    }

    private final BiConsumer<Object, List<Object>> packetsConsumer;
    private final BiConsumer<Object, Object> delayedPacketConsumer;
    private final BiConsumer<Object, Object> immediatePacketConsumer;
    private final BukkitCraftEngine plugin;

    private final Map<ChannelPipeline, BukkitServerPlayer> users = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitServerPlayer> onlineUsers = new ConcurrentHashMap<>();
    private final HashSet<Channel> injectedChannels = new HashSet<>();

    private final PacketIds packetIds;

    private static final String CONNECTION_HANDLER_NAME = "craftengine_connection_handler";
    private static final String SERVER_CHANNEL_HANDLER_NAME = "craftengine_server_channel_handler";
    private static final String PLAYER_CHANNEL_HANDLER_NAME = "craftengine_player_packet_handler";
    private static final String PACKET_ENCODER = "craftengine_encoder";
    private static final String PACKET_DECODER = "craftengine_decoder";

    private boolean active;
    private boolean init;

    public BukkitNetworkManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            this.packetIds = new PacketIds1_21_2();
        } else if (VersionHelper.isVersionNewerThan1_20_5()) {
            this.packetIds = new PacketIds1_20_5();
        } else if (VersionHelper.isVersionNewerThan1_20_3()) {
            this.packetIds = new PacketIds1_20_3();
        } else if (VersionHelper.isVersionNewerThan1_20_2()) {
            this.packetIds = new PacketIds1_20_2();
        } else {
            this.packetIds = new PacketIds1_20();
        }
        this.registerConsumers();
        this.packetsConsumer = ((serverPlayer, packets) -> {
            try {
                Object bundle = Reflections.constructor$ClientboundBundlePacket.newInstance(packets);
                Reflections.method$ServerGamePacketListenerImpl$sendPacket.invoke(
                        Reflections.field$ServerPlayer$connection.get(serverPlayer), bundle);
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to create bundle packet", e);
            }
        });
        this.delayedPacketConsumer = (serverPlayer, packet) -> {
            try {
                Reflections.method$ServerGamePacketListenerImpl$sendPacket.invoke(
                        Reflections.field$ServerPlayer$connection.get(serverPlayer), packet);
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to invoke send packet", e);
            }
        };
        this.immediatePacketConsumer = (serverPlayer, packet) -> {
            try {
                Reflections.method$Connection$sendPacketImmediate.invoke(Reflections.field$ServerCommonPacketListenerImpl$connection.get(Reflections.field$ServerPlayer$connection.get(serverPlayer)),
                        packet, null, true
                );
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to invoke send packet", e);
            }
        };
        this.active = true;
        instance = this;
    }

    private void registerConsumers() {
        registerNMSPacketConsumer(PacketConsumers.LEVEL_CHUNK_WITH_LIGHT, Reflections.clazz$ClientboundLevelChunkWithLightPacket);
        registerNMSPacketConsumer(PacketConsumers.PLAYER_ACTION, Reflections.clazz$ServerboundPlayerActionPacket);
        registerNMSPacketConsumer(PacketConsumers.SWING_HAND, Reflections.clazz$ServerboundSwingPacket);
        registerNMSPacketConsumer(PacketConsumers.USE_ITEM_ON, Reflections.clazz$ServerboundUseItemOnPacket);
        registerNMSPacketConsumer(PacketConsumers.PICK_ITEM_FROM_BLOCK, Reflections.clazz$ServerboundPickItemFromBlockPacket);
        registerNMSPacketConsumer(PacketConsumers.SET_CREATIVE_SLOT, Reflections.clazz$ServerboundSetCreativeModeSlotPacket);
        registerNMSPacketConsumer(PacketConsumers.ADD_ENTITY, Reflections.clazz$ClientboundAddEntityPacket);
        registerNMSPacketConsumer(PacketConsumers.LOGIN, Reflections.clazz$ClientboundLoginPacket);
        registerNMSPacketConsumer(PacketConsumers.RESPAWN, Reflections.clazz$ClientboundRespawnPacket);
        registerNMSPacketConsumer(PacketConsumers.INTERACT_ENTITY, Reflections.clazz$ServerboundInteractPacket);
        registerNMSPacketConsumer(PacketConsumers.REMOVE_ENTITY, Reflections.clazz$ClientboundRemoveEntitiesPacket);
        registerNMSPacketConsumer(PacketConsumers.SYNC_ENTITY_POSITION, Reflections.clazz$ClientboundEntityPositionSyncPacket);
        registerNMSPacketConsumer(PacketConsumers.MOVE_ENTITY, Reflections.clazz$ClientboundMoveEntityPacket$Pos);
        registerNMSPacketConsumer(PacketConsumers.PICK_ITEM_FROM_ENTITY, Reflections.clazz$ServerboundPickItemFromEntityPacket);
        registerNMSPacketConsumer(PacketConsumers.SOUND, Reflections.clazz$ClientboundSoundPacket);
        registerByteBufPacketConsumer(PacketConsumers.SECTION_BLOCK_UPDATE, this.packetIds.clientboundSectionBlocksUpdatePacket());
        registerByteBufPacketConsumer(PacketConsumers.BLOCK_UPDATE, this.packetIds.clientboundBlockUpdatePacket());
        registerByteBufPacketConsumer(PacketConsumers.LEVEL_PARTICLE, this.packetIds.clientboundLevelParticlesPacket());
        registerByteBufPacketConsumer(PacketConsumers.LEVEL_EVENT, this.packetIds.clientboundLevelEventPacket());
    }

    public static BukkitNetworkManager instance() {
        return instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer user = (BukkitServerPlayer) getUser(player);
        if (user != null) {
            user.setPlayer(player);
            this.onlineUsers.put(player.getUniqueId(), user);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Channel channel = getChannel(player);
        NetWorkUser user = removeUser(channel);
        if (user == null) return;
        handleDisconnection(channel);
        this.onlineUsers.remove(player.getUniqueId());
    }

    @Override
    public Collection<BukkitServerPlayer> onlineUsers() {
        return onlineUsers.values();
    }

    @Override
    public void init() {
        if (init) return;
        try {
            Object server = Reflections.method$MinecraftServer$getServer.invoke(null);
            Object serverConnection = Reflections.field$MinecraftServer$connection.get(server);
            @SuppressWarnings("unchecked")
            List<ChannelFuture> channels = (List<ChannelFuture>) Reflections.field$ServerConnectionListener$channels.get(serverConnection);
            ListMonitor<ChannelFuture> monitor = new ListMonitor<>(channels, (future) -> {
                if (!active) return;
                Channel channel = future.channel();
                injectServerChannel(channel);
                injectedChannels.add(channel);
            }, (object) -> {});
            Reflections.field$ServerConnectionListener$channels.set(serverConnection, monitor);
            init = true;
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to init server connection", e);
        }
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin.bootstrap());
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        for (Channel channel : injectedChannels) {
            uninjectServerChannel(channel);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleDisconnection(getChannel(player));
        }
        injectedChannels.clear();
        active = false;
    }

    @Override
    public void setUser(Channel channel, NetWorkUser user) {
        ChannelPipeline pipeline = channel.pipeline();
        users.put(pipeline, (BukkitServerPlayer) user);
    }

    @Override
    public NetWorkUser getUser(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        return users.get(pipeline);
    }

    @Override
    public NetWorkUser removeUser(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        return users.remove(pipeline);
    }

    @Override
    public Channel getChannel(net.momirealms.craftengine.core.entity.player.Player player) {
        return getChannel((Player) player.platformPlayer());
    }

    public NetWorkUser getUser(Player player) {
        return getUser(getChannel(player));
    }

    public NetWorkUser getOnlineUser(Player player) {
        return onlineUsers.get(player.getUniqueId());
    }

    public Channel getChannel(Player player) {
        try {
            return (Channel) Reflections.field$Channel.get(
                    Reflections.field$NetworkManager.get(
                            Reflections.field$ServerPlayer$connection.get(
                                    Reflections.method$CraftPlayer$getHandle.invoke(player)
                            )
                    )
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPacket(@NotNull Player player, @NotNull Object packet) {
        try {
            Object serverPlayer = Reflections.method$CraftPlayer$getHandle.invoke(player);
            this.immediatePacketConsumer.accept(serverPlayer, packet);
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to send packet", e);
        }
    }

    public void sendPacket(@NotNull NetWorkUser player, Object packet, boolean immediately) {
        if (immediately) {
            this.immediatePacketConsumer.accept(player.serverPlayer(), packet);
        } else {
            this.delayedPacketConsumer.accept(player.serverPlayer(), packet);
        }
    }

    public void sendPackets(@NotNull NetWorkUser player, List<Object> packet) {
        this.packetsConsumer.accept(player.serverPlayer(), packet);
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
            if (Reflections.clazz$Connection.isAssignableFrom(entry.getValue().getClass())) {
                pipeline.addBefore(entry.getKey(), PLAYER_CHANNEL_HANDLER_NAME, new PluginChannelHandler(user));
                break;
            }
        }

        String decoderName = pipeline.names().contains("inbound_config") ? "inbound_config" : "decoder";
        pipeline.addBefore(decoderName, PACKET_DECODER, new PluginChannelDecoder(user));
        String encoderName = pipeline.names().contains("outbound_config") ? "outbound_config" : "encoder";
        pipeline.addBefore(encoderName, PACKET_ENCODER, new PluginChannelEncoder(user));

        channel.closeFuture().addListener((ChannelFutureListener) future -> handleDisconnection(user.nettyChannel()));
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
                super.write(context, packet, channelPromise);
                channelPromise.addListener((p) -> {
                    for (Runnable task : event.getDelayedTasks()) {
                        task.run();
                    }
                });
            } catch (Throwable e) {
                plugin.logger().severe("An error occurred when reading packets", e);
                super.write(context, packet, channelPromise);
            }
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object packet) throws Exception {
            NMSPacketEvent event = new NMSPacketEvent(packet);
            onNMSPacketReceive(player, event, packet);
            if (event.isCancelled()) return;
            super.channelRead(context, packet);
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
            }
        }

        private boolean handleCompression(ChannelHandlerContext ctx, ByteBuf buffer) {
            if (handledCompression) return false;
            int compressIndex = ctx.pipeline().names().indexOf("compress");
            if (compressIndex == -1) return false;
            handledCompression = true;
            int encoderIndex = ctx.pipeline().names().indexOf(PACKET_ENCODER);
            if (encoderIndex == -1) return false;
            if (compressIndex > encoderIndex) {
                decompress(ctx, buffer, buffer);
                PluginChannelEncoder encoder = (PluginChannelEncoder) ctx.pipeline().remove(PACKET_ENCODER);
                String encoderName = ctx.pipeline().names().contains("outbound_config") ? "outbound_config" : "encoder";
                ctx.pipeline().addBefore(encoderName, PACKET_ENCODER, new PluginChannelEncoder(encoder));
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
                ByteBufPacketEvent event = new ByteBufPacketEvent(packetId, buf);
                BukkitNetworkManager.this.handleByteBufPacket(this.player, event);
                if (event.isCancelled()) {
                    buf.clear();
                } else if (!event.changed()) {
                    buf.readerIndex(preProcessIndex);
                }
            }
        }
    }

    public class PluginChannelDecoder extends MessageToMessageDecoder<ByteBuf> {
        private final NetWorkUser player;

        public PluginChannelDecoder(NetWorkUser player) {
            this.player = player;
        }

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf byteBuf, List<Object> list) {
            if (byteBuf.isReadable()) {
                list.add(byteBuf.retain());
            }
        }
    }

    private void onNMSPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
        handleNMSPacket(user, event, packet);
    }

    @SuppressWarnings("unchecked")
    private void onNMSPacketSend(NetWorkUser player, NMSPacketEvent event, Object packet) throws ReflectiveOperationException {
        if (Reflections.clazz$ClientboundBundlePacket.isInstance(packet)) {
            Iterable<Object> packets = (Iterable<Object>) Reflections.field$BundlePacket$packets.get(packet);
            for (Object p : packets) {
                onNMSPacketSend(player, event, p);
            }
        } else {
            handleNMSPacket(player, event, packet);
        }
    }

    protected void handleNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        Optional.ofNullable(nmsPacketFunctions.get(packet.getClass()))
                .ifPresent(function -> function.accept(user, event, packet));
    }

    protected void handleByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        Optional.ofNullable(byteBufPacketFunctions.get(packetID))
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
            Reflections.method$messageToByteEncoder$encode.invoke(encoder, ctx, msg, output);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call encode", e);
        }
    }

    public static List<Object> callDecode(Object decoder, Object ctx, Object input) {
        List<Object> output = new ArrayList<>();
        try {
            Reflections.method$byteToMessageDecoder$decode.invoke(decoder, ctx, input, output);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call decode", e);
        }
        return output;
    }
}
