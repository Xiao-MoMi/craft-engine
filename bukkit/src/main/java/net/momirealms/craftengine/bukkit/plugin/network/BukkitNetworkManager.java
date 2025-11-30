package net.momirealms.craftengine.bukkit.plugin.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitProjectileManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.FurnitureItemBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.TotemAnimationCommand;
import net.momirealms.craftengine.bukkit.plugin.injector.ProtectedFieldVisitor;
import net.momirealms.craftengine.bukkit.plugin.network.handler.*;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds1_20;
import net.momirealms.craftengine.bukkit.plugin.network.id.PacketIds1_20_5;
import net.momirealms.craftengine.bukkit.plugin.network.id.PlayPacketIdHelper;
import net.momirealms.craftengine.bukkit.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.ByteBufferPacketListenerHolder;
import net.momirealms.craftengine.bukkit.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.bukkit.plugin.network.payload.DiscardedPayload;
import net.momirealms.craftengine.bukkit.plugin.network.payload.Payload;
import net.momirealms.craftengine.bukkit.plugin.network.payload.PayloadHelper;
import net.momirealms.craftengine.bukkit.plugin.network.payload.UnknownPayload;
import net.momirealms.craftengine.bukkit.plugin.reflection.leaves.LeavesReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.*;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.plugin.user.FakeBukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.advancement.network.AdvancementHolder;
import net.momirealms.craftengine.core.advancement.network.AdvancementProgress;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.furniture.HitBox;
import net.momirealms.craftengine.core.entity.furniture.HitBoxPart;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.font.IllegalCharacterProcessResult;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.item.recipe.network.legacy.LegacyRecipeHolder;
import net.momirealms.craftengine.core.item.recipe.network.modern.RecipeBookEntry;
import net.momirealms.craftengine.core.item.recipe.network.modern.SingleInputButtonDisplay;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.RecipeDisplay;
import net.momirealms.craftengine.core.item.trade.MerchantOffer;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.*;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.Palette;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.chunk.client.ClientSection;
import net.momirealms.craftengine.core.world.chunk.client.PackedOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.client.SingularOcclusionStorage;
import net.momirealms.craftengine.core.world.chunk.packet.BlockEntityData;
import net.momirealms.craftengine.core.world.chunk.packet.MCSection;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTDataComponentValue;
import org.bukkit.*;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class BukkitNetworkManager implements NetworkManager, Listener, PluginMessageListener {
    private static BukkitNetworkManager instance;
    private final BukkitCraftEngine plugin;
    private final Map<Class<?>, NMSPacketListener> nmsPacketListeners = new IdentityHashMap<>(128);

    private final ByteBufferPacketListenerHolder[] s2cGamePacketListeners;
    private final ByteBufferPacketListenerHolder[] c2sGamePacketListeners;

    private final TriConsumer<ChannelHandler, Object, Object> packetConsumer;
    private final TriConsumer<ChannelHandler, List<Object>, Object> packetsConsumer;
    private final TriConsumer<Channel, Object, Runnable> immediatePacketConsumer;
    private final TriConsumer<Channel, List<Object>, Runnable> immediatePacketsConsumer;

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

    private final boolean hasModelEngine;

    private int[] blockStateRemapper;
    private int[] modBlockStateRemapper;

    @SuppressWarnings("unchecked")
    public BukkitNetworkManager(BukkitCraftEngine plugin) {
        instance = this;
        this.s2cGamePacketListeners = new ByteBufferPacketListenerHolder[PlayPacketIdHelper.count(PacketFlow.CLIENTBOUND)];
        this.c2sGamePacketListeners = new ByteBufferPacketListenerHolder[PlayPacketIdHelper.count(PacketFlow.SERVERBOUND)];
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        this.hasModelEngine = modelEngine != null && modelEngine.getPluginMeta().getVersion().startsWith("R4");
        this.plugin = plugin;
        // set up packet id
        this.packetIds = VersionHelper.isOrAbove1_20_5() ? new PacketIds1_20_5() : new PacketIds1_20();
        // register packet handlers
        this.registerPacketListeners();
        PayloadHelper.registerDataTypes();
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
        // Inject Leaves bot list
        if (VersionHelper.isLeaves()) {
            this.injectLeavesBotList();
        }
    }

    public static BukkitNetworkManager instance() {
        return instance;
    }

    @Override
    public int remapBlockState(int stateId, boolean enableMod) {
        return enableMod ? this.modBlockStateRemapper[stateId] : this.blockStateRemapper[stateId];
    }

    private void registerNMSPacketConsumer(final NMSPacketListener listener, @Nullable Class<?> packet) {
        if (packet == null) return;
        this.nmsPacketListeners.put(packet, listener);
    }

    private void registerS2CGamePacketListener(final ByteBufferPacketListener listener, int id, String name) {
        if (id == -1) return;
        if (id < 0 || id >= this.s2cGamePacketListeners.length) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }
        this.s2cGamePacketListeners[id] = new ByteBufferPacketListenerHolder(name, listener);
    }

    private void registerC2SGamePacketListener(final ByteBufferPacketListener listener, int id, String name) {
        if (id == -1) return;
        if (id < 0 || id >= this.c2sGamePacketListeners.length) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }
        this.c2sGamePacketListeners[id] = new ByteBufferPacketListenerHolder(name, listener);
    }

    @Override
    public void delayedLoad() {
        this.resendTags();
    }

    @SuppressWarnings("unchecked")
    public void resendTags() {
        Object packet = TagUtils.createUpdateTagsPacket(Map.of(MRegistries.BLOCK, BukkitBlockManager.instance().cachedUpdateTags()), FastNMS.INSTANCE.method$TagNetworkSerialization$serializeTagsToNetwork());
        for (BukkitServerPlayer player : onlineUsers()) {
            player.sendPacket(packet, false);
        }
    }

    public void addFakePlayer(Player player) {
        FakeBukkitServerPlayer fakePlayer = new FakeBukkitServerPlayer(this.plugin);
        fakePlayer.setPlayer(player);
        this.onlineUsers.put(player.getUniqueId(), fakePlayer);
        this.resetUserArray();
    }

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

    @SuppressWarnings("unchecked")
    private void injectLeavesBotList() {
        try {
            Object botList = LeavesReflections.field$BotList$INSTANCE.get(null);
            List<Object> bots = (List<Object>) LeavesReflections.field$BotList$bots.get(botList);
            ListMonitor<Object> monitor = new ListMonitor<>(bots,
                    (bot) -> addFakePlayer(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(bot)),
                    (bot) -> removeFakePlayer(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(bot))
            );
            LeavesReflections.field$BotList$bots.set(botList, monitor);
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().severe("Failed to inject leaves bot list");
        }
    }

    public void registerBlockStatePacketListeners(int[] blockStateMappings, Predicate<Integer> occlusionPredicate) {
        int stoneId = BlockStateUtils.blockStateToId(MBlocks.STONE$defaultState);
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
        registerS2CGamePacketListener(new LevelChunkWithLightListener(
                newMappings,
                newMappingsMOD,
                newMappings.length,
                RegistryUtils.currentBiomeRegistrySize(),
                occlusionPredicate
        ), this.packetIds.clientboundLevelChunkWithLightPacket(), "ClientboundLevelChunkWithLightPacket");
        registerS2CGamePacketListener(new SectionBlockUpdateListener(newMappings, newMappingsMOD, occlusionPredicate), this.packetIds.clientboundSectionBlocksUpdatePacket(), "ClientboundSectionBlocksUpdatePacket");
        registerS2CGamePacketListener(new BlockUpdateListener(newMappings, newMappingsMOD, occlusionPredicate), this.packetIds.clientboundBlockUpdatePacket(), "ClientboundBlockUpdatePacket");
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_21_4() ?
                new LevelParticleListener1_21_4(newMappings, newMappingsMOD) :
                (VersionHelper.isOrAbove1_20_5() ?
                new LevelParticleListener1_20_5(newMappings, newMappingsMOD) :
                new LevelParticleListener1_20(newMappings, newMappingsMOD)),
                this.packetIds.clientboundLevelParticlesPacket(), "ClientboundLevelParticlesPacket"
        );
        registerS2CGamePacketListener(new LevelEventListener(newMappings, newMappingsMOD), this.packetIds.clientboundLevelEventPacket(), "ClientboundLevelEventPacket");
    }

    private void registerPacketListeners() {
        registerNMSPacketConsumer(new PlayerInfoUpdateListener(), NetworkReflections.clazz$ClientboundPlayerInfoUpdatePacket);
        registerNMSPacketConsumer(new PlayerActionListener(), NetworkReflections.clazz$ServerboundPlayerActionPacket);
        registerNMSPacketConsumer(new SwingListener(), NetworkReflections.clazz$ServerboundSwingPacket);
        registerNMSPacketConsumer(new HelloListener(), NetworkReflections.clazz$ServerboundHelloPacket);
        registerNMSPacketConsumer(new UseItemOnListener(), NetworkReflections.clazz$ServerboundUseItemOnPacket);
        registerNMSPacketConsumer(new PickItemFromBlockListener(), NetworkReflections.clazz$ServerboundPickItemFromBlockPacket);
        registerNMSPacketConsumer(new PickItemFromEntityListener(), NetworkReflections.clazz$ServerboundPickItemFromEntityPacket);
        registerNMSPacketConsumer(new SetCreativeSlotListener(), NetworkReflections.clazz$ServerboundSetCreativeModeSlotPacket);
        registerNMSPacketConsumer(new LoginListener(), NetworkReflections.clazz$ClientboundLoginPacket);
        registerNMSPacketConsumer(new RespawnListener(), NetworkReflections.clazz$ClientboundRespawnPacket);
        registerNMSPacketConsumer(new SyncEntityPositionListener(), NetworkReflections.clazz$ClientboundEntityPositionSyncPacket);
        registerNMSPacketConsumer(new RenameItemListener(), NetworkReflections.clazz$ServerboundRenameItemPacket);
        registerNMSPacketConsumer(new SignUpdateListener(), NetworkReflections.clazz$ServerboundSignUpdatePacket);
        registerNMSPacketConsumer(new EditBookListener(), NetworkReflections.clazz$ServerboundEditBookPacket);
        registerNMSPacketConsumer(new CustomPayloadListener1_20_2(), VersionHelper.isOrAbove1_20_2() ? NetworkReflections.clazz$ServerboundCustomPayloadPacket : null);
        registerNMSPacketConsumer(new ResourcePackResponseListener(), NetworkReflections.clazz$ServerboundResourcePackPacket);
        registerNMSPacketConsumer(new EntityEventListener(), NetworkReflections.clazz$ClientboundEntityEventPacket);
        registerNMSPacketConsumer(new MovePosAndRotateEntityListener(), NetworkReflections.clazz$ClientboundMoveEntityPacket$PosRot);
        registerNMSPacketConsumer(new MovePosEntityListener(), NetworkReflections.clazz$ClientboundMoveEntityPacket$Pos);
        registerNMSPacketConsumer(new RotateHeadListener(), NetworkReflections.clazz$ClientboundRotateHeadPacket);
        registerNMSPacketConsumer(new SetEntityMotionListener(), NetworkReflections.clazz$ClientboundSetEntityMotionPacket);
        registerNMSPacketConsumer(new FinishConfigurationListener(), NetworkReflections.clazz$ClientboundFinishConfigurationPacket);
        registerNMSPacketConsumer(new LoginFinishedListener(), NetworkReflections.clazz$ClientboundLoginFinishedPacket);
        registerNMSPacketConsumer(new UpdateTagsListener(), NetworkReflections.clazz$ClientboundUpdateTagsPacket);
        registerNMSPacketConsumer(new ClientInformationListener(), VersionHelper.isOrAbove1_20_2() ? NetworkReflections.clazz$ServerboundClientInformationPacket1 : NetworkReflections.clazz$ServerboundClientInformationPacket0);
        registerNMSPacketConsumer(new ContainerClickListener1_21_5(), VersionHelper.isOrAbove1_21_5() ? NetworkReflections.clazz$ServerboundContainerClickPacket : null);
        registerS2CGamePacketListener(new ForgetLevelChunkListener(), this.packetIds.clientboundForgetLevelChunkPacket(), "ClientboundForgetLevelChunkPacket");
        registerS2CGamePacketListener(new SetScoreListener1_20_3(), VersionHelper.isOrAbove1_20_3() ? this.packetIds.clientboundSetScorePacket() : -1, "ClientboundSetScorePacket");
        registerS2CGamePacketListener(new AddRecipeBookListener(), this.packetIds.clientboundRecipeBookAddPacket(), "ClientboundRecipeBookAddPacket");
        registerS2CGamePacketListener(new PlaceGhostRecipeListener(), this.packetIds.clientboundPlaceGhostRecipePacket(), "ClientboundPlaceGhostRecipePacket");
        registerS2CGamePacketListener(VersionHelper.isOrAbove1_21_2() ? new UpdateRecipesListener1_21_2() : new UpdateRecipesListener1_20(), this.packetIds.clientboundUpdateRecipesPacket(), "ClientboundUpdateRecipesPacket");
        registerS2CGamePacketListener(new UpdateAdvancementsListener(), this.packetIds.clientboundUpdateAdvancementsPacket(), "ClientboundUpdateAdvancementsPacket");
        registerS2CGamePacketListener(new RemoveEntityListener(), this.packetIds.clientboundRemoveEntitiesPacket(), "ClientboundRemoveEntitiesPacket");
        registerS2CGamePacketListener(new SoundListener(), this.packetIds.clientboundSoundPacket(), "ClientboundSoundPacket");
        registerS2CGamePacketListener(new ContainerSetContentListener(), this.packetIds.clientboundContainerSetContentPacket(), "ClientboundContainerSetContentPacket");
        registerS2CGamePacketListener(new ContainerSetSlotListener(), this.packetIds.clientboundContainerSetSlotPacket(), "ClientboundContainerSetSlotPacket");
        registerS2CGamePacketListener(new SetCursorItemListener(), this.packetIds.clientboundSetCursorItemPacket(), "ClientboundSetCursorItemPacket");
        registerS2CGamePacketListener(new SetEquipmentListener(), this.packetIds.clientboundSetEquipmentPacket(), "ClientboundSetEquipmentPacket");
        registerS2CGamePacketListener(new SetPlayerInventoryListener1_21_2(), VersionHelper.isOrAbove1_21_2() ? this.packetIds.clientboundSetPlayerInventoryPacket() : -1, "ClientboundSetPlayerInventoryPacket");
        registerS2CGamePacketListener(new SetEntityDataListener(), this.packetIds.clientboundSetEntityDataPacket(), "ClientboundSetEntityDataPacket");
        registerC2SGamePacketListener(new SetCreativeModeSlotListener(), this.packetIds.serverboundSetCreativeModeSlotPacket(), "ServerboundSetCreativeModeSlotPacket");
        registerC2SGamePacketListener(new ContainerClick1_20(), VersionHelper.isOrAbove1_21_5() ? -1 : this.packetIds.serverboundContainerClickPacket(), "ServerboundContainerClickPacket");
        registerC2SGamePacketListener(new InteractEntityListener(), this.packetIds.serverboundInteractPacket(), "ServerboundInteractPacket");
        registerC2SGamePacketListener(new CustomPayloadListener1_20(), VersionHelper.isOrAbove1_20_2() ? -1 : this.packetIds.serverboundCustomPayloadPacket(), "ServerboundCustomPayloadPacket");
        registerS2CGamePacketListener(VersionHelper.isOrAbove1_20_5() ? new MerchantOffersListener1_20_5() : new MerchantOffersListener1_20(), this.packetIds.clientBoundMerchantOffersPacket(), "ClientboundMerchantOffersPacket");
        registerS2CGamePacketListener(new AddEntityListener(RegistryUtils.currentEntityTypeRegistrySize()), this.packetIds.clientboundAddEntityPacket(), "ClientboundAddEntityPacket");
        registerS2CGamePacketListener(new BlockEntityDataListener(), this.packetIds.clientboundBlockEntityDataPacket(), "ClientboundBlockEntityDataPacket");
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new OpenScreenListener1_20_3() :
                new OpenScreenListener1_20(),
                this.packetIds.clientboundOpenScreenPacket(), "ClientboundOpenScreenPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new SystemChatListener1_20_3() :
                new SystemChatListener1_20(),
                this.packetIds.clientboundSystemChatPacket(), "ClientboundSystemChatPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new SetActionBarListener1_20_3() :
                new SetActionBarListener1_20(),
                this.packetIds.clientboundSetActionBarTextPacket(), "ClientboundSetActionBarTextPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new TabListListener1_20_3() :
                new TabListListener1_20(),
                this.packetIds.clientboundTabListPacket(), "ClientboundTabListPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new SetTitleListener1_20_3() :
                new SetTitleListener1_20(),
                this.packetIds.clientboundSetTitleTextPacket(), "ClientboundSetTitleTextPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new SetSubtitleListener1_20_3() :
                new SetSubtitleListener1_20(),
                this.packetIds.clientboundSetSubtitleTextPacket(), "ClientboundSetSubtitleTextPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new BossEventListener1_20_3() :
                new BossEventListener1_20(),
                this.packetIds.clientboundBossEventPacket(), "ClientboundBossEventPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new TeamListener1_20_3() :
                new TeamListener1_20(),
                this.packetIds.clientboundSetPlayerTeamPacket(), "ClientboundSetPlayerTeamPacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new SetObjectiveListener1_20_3() :
                new SetObjectiveListener1_20(),
                this.packetIds.clientboundSetObjectivePacket(), "ClientboundSetObjectivePacket"
        );
        registerS2CGamePacketListener(
                VersionHelper.isOrAbove1_20_3() ?
                new PlayerChatListener_1_20_3() :
                new PlayerChatListener_1_20(),
                this.packetIds.clientboundPlayerChatPacket(), "ClientboundPlayerChatPacket");
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
            if (VersionHelper.isFolia()) {
                player.getScheduler().runAtFixedRate(plugin.javaPlugin(), (t) -> user.tick(),
                        () -> {}, 1, 1);
            }
            user.sendPacket(TotemAnimationCommand.FIX_TOTEM_SOUND_PACKET, false);
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

    @Nullable
    public NetWorkUser getUser(Player player) {
        return getUser(getChannel(player));
    }

    @Nullable
    public NetWorkUser getOnlineUser(Player player) {
        return this.onlineUsers.get(player.getUniqueId());
    }

    // 当假人的时候channel为null
    @NotNull
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
        if (player.isFakePlayer()) return;
        if (immediately) {
            this.immediatePacketConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            this.packetConsumer.accept(player.connection(), packet, sendListener != null ? FastNMS.INSTANCE.method$PacketSendListener$thenRun(sendListener) : null);
        }
    }

    @Override
    public void sendPackets(@NotNull NetWorkUser player, List<Object> packet, boolean immediately, Runnable sendListener) {
        if (player.isFakePlayer()) return;
        if (immediately) {
            this.immediatePacketsConsumer.accept(player.nettyChannel(), packet, sendListener);
        } else {
            this.packetsConsumer.accept(player.connection(), packet, sendListener != null ? FastNMS.INSTANCE.method$PacketSendListener$thenRun(sendListener) : null);
        }
    }

    public boolean hasModelEngine() {
        return hasModelEngine;
    }

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
        handleReceiveNMSPacket(user, event, packet);
    }

    private void onNMSPacketSend(NetWorkUser player, NMSPacketEvent event, Object packet) {
        if (NetworkReflections.clazz$ClientboundBundlePacket.isInstance(packet)) {
            Iterable<Object> packets = FastNMS.INSTANCE.method$ClientboundBundlePacket$subPackets(packet);
            for (Object p : packets) {
                onNMSPacketSend(player, event, p);
            }
        } else {
            Debugger.PACKET.debug(() -> "[S->C]" + packet.getClass());
            handleSendNMSPacket(player, event, packet);
        }
    }

    protected void handleReceiveNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        NMSPacketListener nmsPacketListener = this.nmsPacketListeners.get(packet.getClass());
        if (nmsPacketListener != null) {
            try {
                nmsPacketListener.onPacketReceive(user, event, packet);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + packet.getClass(), t);
            }
        }
    }

    protected void handleSendNMSPacket(NetWorkUser user, NMSPacketEvent event, Object packet) {
        NMSPacketListener nmsPacketListener = this.nmsPacketListeners.get(packet.getClass());
        if (nmsPacketListener != null) {
            try {
                nmsPacketListener.onPacketSend(user, event, packet);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + packet.getClass(), t);
            }
        }
    }

    protected void handleS2CByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        ByteBufferPacketListenerHolder holder = this.s2cGamePacketListeners[packetID];
        if (holder != null) {
            try {
                holder.listener().onPacketSend(user, event);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + holder.id(), t);
            }
        }
    }

    protected void handleC2SByteBufPacket(NetWorkUser user, ByteBufPacketEvent event) {
        int packetID = event.packetID();
        ByteBufferPacketListenerHolder holder = this.c2sGamePacketListeners[packetID];
        if (holder != null) {
            try {
                holder.listener().onPacketReceive(user, event);
            } catch (Throwable t) {
                this.plugin.logger().warn("An error occurred when handling packet " + holder.id(), t);
            }
        }
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
            ByteBuf temp = (ByteBuf) callDecode(decompressor, ctx, input).getFirst();
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

    /*
     *
     *
     *  Packet Listener Implementations
     *
     *
     */
    public static class HelloListener implements NMSPacketListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            String name;
            try {
                name = (String) NetworkReflections.methodHandle$ServerboundHelloPacket$nameGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().severe("Failed to get name from ServerboundHelloPacket", t);
                return;
            }
            player.setUnverifiedName(name);
            if (VersionHelper.isOrAbove1_20_2()) {
                UUID uuid;
                try {
                    uuid = (UUID) NetworkReflections.methodHandle$ServerboundHelloPacket$uuidGetter.invokeExact(packet);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().severe("Failed to get uuid from ServerboundHelloPacket", t);
                    return;
                }
                player.setUnverifiedUUID(uuid);
            } else {
                Optional<UUID> uuid;
                try {
                    uuid = (Optional<UUID>) NetworkReflections.methodHandle$ServerboundHelloPacket$uuidGetter.invokeExact(packet);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().severe("Failed to get uuid from ServerboundHelloPacket", t);
                    return;
                }
                if (uuid.isPresent()) {
                    player.setUnverifiedUUID(uuid.get());
                } else {
                    player.setUnverifiedUUID(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    public static class PlayerActionListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Player platformPlayer = player.platformPlayer();
            World world = platformPlayer.getWorld();
            Object blockPos = FastNMS.INSTANCE.field$ServerboundPlayerActionPacket$pos(packet);
            BlockPos pos = LocationUtils.fromBlockPos(blockPos);
            if (VersionHelper.isFolia()) {
                platformPlayer.getScheduler().run(BukkitCraftEngine.instance().javaPlugin(), (t) -> {
                    try {
                        handlePlayerActionPacketOnMainThread(player, world, pos, packet);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPlayerActionPacket", e);
                    }
                }, () -> {});
            } else {
                handlePlayerActionPacketOnMainThread(player, world, pos, packet);
            }
        }

        private static void handlePlayerActionPacketOnMainThread(BukkitServerPlayer player, World world, BlockPos pos, Object packet) {
            Object action = FastNMS.INSTANCE.field$ServerboundPlayerActionPacket$action(packet);
            if (action == NetworkReflections.instance$ServerboundPlayerActionPacket$Action$START_DESTROY_BLOCK) {
                Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world);
                Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, LocationUtils.toBlockPos(pos));
                int stateId = BlockStateUtils.blockStateToId(blockState);
                // not a custom block
                if (BlockStateUtils.isVanillaBlock(stateId)) {
                    if (Config.enableSoundSystem()) {
                        Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
                        Object soundEvent = FastNMS.INSTANCE.field$SoundType$hitSound(soundType);
                        Object soundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
                        if (BukkitBlockManager.instance().isHitSoundMissing(soundId)) {
                            player.startMiningBlock(pos, blockState, null);
                            return;
                        }
                    }
                    if (player.isMiningBlock()) {
                        player.stopMiningBlock();
                    } else {
                        player.setClientSideCanBreakBlock(true);
                    }
                    return;
                }
                if (player.isAdventureMode()) {
                    if (Config.simplifyAdventureBreakCheck()) {
                        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId);
                        if (!player.canBreak(pos, state.visualBlockState().literalObject())) {
                            player.preventMiningBlock();
                            return;
                        }
                    } else {
                        if (!player.canBreak(pos, null)) {
                            player.preventMiningBlock();
                            return;
                        }
                    }
                }
                player.startMiningBlock(pos, blockState, BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId));
            } else if (action == NetworkReflections.instance$ServerboundPlayerActionPacket$Action$ABORT_DESTROY_BLOCK) {
                if (player.isMiningBlock()) {
                    player.abortMiningBlock();
                }
            } else if (action == NetworkReflections.instance$ServerboundPlayerActionPacket$Action$STOP_DESTROY_BLOCK) {
                if (player.isMiningBlock()) {
                    player.stopMiningBlock();
                }
            }
        }
    }

    public static class SwingListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Object hand = FastNMS.INSTANCE.field$ServerboundSwingPacket$hand(packet);
            if (hand == CoreReflections.instance$InteractionHand$MAIN_HAND) {
                player.onSwingHand();
            }
        }
    }

    public static class UseItemOnListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (player.isMiningBlock()) {
                player.stopMiningBlock();
            }
        }
    }

    public static class PlayerInfoUpdateListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!Config.interceptPlayerInfo()) return;
            List<Object> entries = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$entries(packet);
            if (entries instanceof MarkedArrayList) {
                return;
            }
            EnumSet<? extends Enum<?>> enums = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$actions(packet);
            outer: {
                for (Object entry : enums) {
                    if (entry == NetworkReflections.instance$ClientboundPlayerInfoUpdatePacket$Action$UPDATE_DISPLAY_NAME) {
                        break outer;
                    }
                }
                return;
            }
            boolean isChanged = false;
            List<Object> newEntries = new MarkedArrayList<>();
            for (Object entry : entries) {
                Object mcComponent = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$Entry$displayName(entry);
                if (mcComponent == null) {
                    newEntries.add(entry);
                } else {
                    String json = ComponentUtils.minecraftToJson(mcComponent);
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
                    if (tokens.isEmpty()) {
                        newEntries.add(entry);
                    } else {
                        Object newEntry = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket$Entry(entry,
                                ComponentUtils.adventureToMinecraft(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
                        newEntries.add(newEntry);
                        isChanged = true;
                    }
                }
            }
            if (isChanged) {
                event.replacePacket(FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket(enums, newEntries));
            }
        }
    }

    public static class PickItemFromBlockListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            Player player = (Player) user.platformPlayer();
            if (player == null) return;
            Object pos;
            try {
                pos = NetworkReflections.methodHandle$ServerboundPickItemFromBlockPacket$posGetter.invokeExact(packet);
            } catch (Throwable e) {
                CraftEngine.instance().logger().warn("Failed to get pos from ServerboundPickItemFromBlockPacket", e);
                return;
            }
            if (VersionHelper.isFolia()) {
                int x = FastNMS.INSTANCE.field$Vec3i$x(pos);
                int z = FastNMS.INSTANCE.field$Vec3i$z(pos);
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromBlockPacketOnMainThread(player, pos);
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromBlockPacket on region thread", e);
                    }
                }, player.getWorld(), x >> 4, z >> 4);
            } else {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromBlockPacketOnMainThread(player, pos);
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromBlockPacket on main thread", e);
                    }
                });
            }
        }

        private static void handlePickItemFromBlockPacketOnMainThread(Player player, Object pos) throws Throwable {
            Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld());
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, pos);
            ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
            if (state == null) return;
            Key itemId = state.settings().itemId();
            if (itemId == null) return;
            pickItem(player, itemId, pos, null);
        }
    }

    public static class PickItemFromEntityListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            int entityId;
            try {
                entityId = (int) NetworkReflections.methodHandle$ServerboundPickItemFromEntityPacket$idGetter.invokeExact(packet);
            } catch (Throwable e) {
                CraftEngine.instance().logger().warn("Failed to get entityId from ServerboundPickItemFromEntityPacket", e);
                return;
            }
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByEntityId(entityId);
            if (furniture == null) return;
            Player player = (Player) user.platformPlayer();
            if (player == null) return;
            if (VersionHelper.isFolia()) {
                player.getScheduler().run(BukkitCraftEngine.instance().javaPlugin(), (t) -> {
                    try {
                        handlePickItemFromEntityOnMainThread(player, furniture);
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromEntityPacket on region thread", e);
                    }
                }, () -> {});
            } else {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromEntityOnMainThread(player, furniture);
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromEntityPacket on main thread", e);
                    }
                });
            }
        }

        private static void handlePickItemFromEntityOnMainThread(Player player, BukkitFurniture furniture) throws Throwable {
            Key itemId = furniture.config().settings().itemId();
            if (itemId == null) return;
            pickItem(player, itemId, null, FastNMS.INSTANCE.method$CraftEntity$getHandle(furniture.baseEntity()));
        }
    }

    private static void pickItem(Player player, Key itemId, @Nullable Object blockPos, @Nullable Object entity) throws Throwable {
        ItemStack itemStack = BukkitCraftEngine.instance().itemManager().buildItemStack(itemId, BukkitCraftEngine.instance().adapt(player));
        if (itemStack == null) {
            CraftEngine.instance().logger().warn("Item: " + itemId + " is not a valid item");
            return;
        }
        assert CoreReflections.method$ServerGamePacketListenerImpl$tryPickItem != null;
        if (VersionHelper.isOrAbove1_21_5()) {
            CoreReflections.method$ServerGamePacketListenerImpl$tryPickItem.invoke(
                    CoreReflections.methodHandle$ServerPlayer$connectionGetter.invokeExact(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)),
                    FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack), blockPos, entity, true);
        } else {
            CoreReflections.method$ServerGamePacketListenerImpl$tryPickItem.invoke(
                    CoreReflections.methodHandle$ServerPlayer$connectionGetter.invokeExact(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)), FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack));
        }
    }

    public static class ClientInformationListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            try {
                if (VersionHelper.isOrAbove1_20_2()) {
                    Object clientInfo = NetworkReflections.field$ServerboundClientInformationPacket$information.get(packet);
                    if (clientInfo == null) return;
                    String locale = (String) CoreReflections.field$ClientInformation$language.get(clientInfo);
                    if (locale == null) return;
                    ((BukkitServerPlayer) user).setClientLocale(TranslationManager.parseLocale(locale));
                } else {
                    String locale = (String) NetworkReflections.field$ServerboundClientInformationPacket$language.get(packet);
                    if (locale == null) return;
                    ((BukkitServerPlayer) user).setClientLocale(TranslationManager.parseLocale(locale));
                }
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to handle ServerboundClientInformationPacket", e);
            }
        }
    }

    public static class SetCreativeSlotListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (VersionHelper.isOrAbove1_21_4()) return;
            if (!user.isOnline()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (VersionHelper.isFolia()) {
                player.platformPlayer().getScheduler().run(BukkitCraftEngine.instance().javaPlugin(), (t) -> {
                    try {
                        handleSetCreativeSlotPacketOnMainThread(player, packet);
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundSetCreativeModeSlotPacket on region thread", e);
                    }
                }, () -> {});
            } else {
                try {
                    handleSetCreativeSlotPacketOnMainThread(player, packet);
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("Failed to handle ServerboundSetCreativeModeSlotPacket on main thread", e);
                }
            }
        }

        private static void handleSetCreativeSlotPacketOnMainThread(BukkitServerPlayer player, Object packet) throws Throwable {
            Player bukkitPlayer = player.platformPlayer();
            if (bukkitPlayer == null) return;
            if (bukkitPlayer.getGameMode() != GameMode.CREATIVE) return;
            int slot = VersionHelper.isOrAbove1_20_5() ? (short) NetworkReflections.methodHandle$ServerboundSetCreativeModeSlotPacket$slotNumGetter.invokeExact(packet) : (int) NetworkReflections.methodHandle$ServerboundSetCreativeModeSlotPacket$slotNumGetter.invokeExact(packet);
            if (slot < 36 || slot > 44) return;
            ItemStack item = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(NetworkReflections.methodHandle$ServerboundSetCreativeModeSlotPacket$itemStackGetter.invokeExact(packet));
            if (ItemStackUtils.isEmpty(item)) return;
            if (slot - 36 != bukkitPlayer.getInventory().getHeldItemSlot()) {
                return;
            }
            double interactionRange = player.getCachedInteractionRange();
            // do ray trace to get current block
            RayTraceResult result = bukkitPlayer.rayTraceBlocks(interactionRange, FluidCollisionMode.NEVER);
            if (result == null) return;
            Block hitBlock = result.getHitBlock();
            if (hitBlock == null) return;
            ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(hitBlock);
            // not a custom block
            if (state == null || state.isEmpty()) return;
            Key itemId = state.settings().itemId();
            // no item available
            if (itemId == null) return;
            Object vanillaBlock = FastNMS.INSTANCE.method$BlockState$getBlock(state.visualBlockState().literalObject());
            Object vanillaBlockItem = FastNMS.INSTANCE.method$Block$asItem(vanillaBlock);
            if (vanillaBlockItem == null) return;
            Key addItemId = KeyUtils.namespacedKey2Key(item.getType().getKey());
            Key blockItemId = KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ITEM, vanillaBlockItem));
            if (!addItemId.equals(blockItemId)) return;
            ItemStack itemStack = BukkitCraftEngine.instance().itemManager().buildCustomItemStack(itemId, player);
            if (ItemStackUtils.isEmpty(itemStack)) {
                CraftEngine.instance().logger().warn("Item: " + itemId + " is not a valid item");
                return;
            }
            PlayerInventory inventory = bukkitPlayer.getInventory();
            int sameItemSlot = -1;
            int emptySlot = -1;
            for (int i = 0; i < 9 + 27; i++) {
                ItemStack invItem = inventory.getItem(i);
                if (ItemStackUtils.isEmpty(invItem)) {
                    if (emptySlot == -1 && i < 9) emptySlot = i;
                    continue;
                }
                if (invItem.getType().equals(itemStack.getType()) && invItem.getItemMeta().equals(itemStack.getItemMeta())) {
                    if (sameItemSlot == -1) sameItemSlot = i;
                }
            }
            if (sameItemSlot != -1) {
                if (sameItemSlot < 9) {
                    inventory.setHeldItemSlot(sameItemSlot);
                    ItemStack previousItem = inventory.getItem(slot - 36);
                    BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, previousItem));
                } else {
                    ItemStack sameItem = inventory.getItem(sameItemSlot);
                    int finalSameItemSlot = sameItemSlot;
                    BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> {
                        inventory.setItem(finalSameItemSlot, new ItemStack(Material.AIR));
                        inventory.setItem(slot - 36, sameItem);
                    });
                }
            } else {
                if (item.getAmount() == 1) {
                    if (ItemStackUtils.isEmpty(inventory.getItem(slot - 36))) {
                        BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, itemStack));
                        return;
                    }
                    if (emptySlot != -1) {
                        inventory.setHeldItemSlot(emptySlot);
                        inventory.setItem(emptySlot, itemStack);
                    } else {
                        BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, itemStack));
                    }
                }
            }
        }
    }

    public static class LoginListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            player.setConnectionState(ConnectionState.PLAY);
            Object dimensionKey;
            try {
                if (!VersionHelper.isOrAbove1_20_2()) {
                    dimensionKey = NetworkReflections.methodHandle$ClientboundLoginPacket$dimensionGetter.invokeExact(packet);
                } else {
                    Object commonInfo = NetworkReflections.methodHandle$ClientboundLoginPacket$commonPlayerSpawnInfoGetter.invokeExact(packet);
                    dimensionKey = NetworkReflections.methodHandle$CommonPlayerSpawnInfo$dimensionGetter.invokeExact(commonInfo);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get dimensionKey from ClientboundLoginPacket", t);
                return;
            }
            Object location = FastNMS.INSTANCE.field$ResourceKey$location(dimensionKey);
            World world = Bukkit.getWorld(Objects.requireNonNull(NamespacedKey.fromString(location.toString())));
            if (world != null) {
                player.setClientSideWorld(BukkitAdaptors.adapt(world));
            } else {
                CraftEngine.instance().logger().warn("Failed to handle ClientboundLoginPacket: World " + location + " does not exist");
            }
        }
    }

    public static class RespawnListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            player.clearView();
            Object dimensionKey;
            try {
                if (!VersionHelper.isOrAbove1_20_2()) {
                    dimensionKey = NetworkReflections.methodHandle$ClientboundRespawnPacket$dimensionGetter.invokeExact(packet);
                } else {
                    Object commonInfo = NetworkReflections.methodHandle$ClientboundRespawnPacket$commonPlayerSpawnInfoGetter.invokeExact(packet);
                    dimensionKey = NetworkReflections.methodHandle$CommonPlayerSpawnInfo$dimensionGetter.invokeExact(commonInfo);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get dimensionKey from ClientboundRespawnPacket", t);
                return;
            }
            Object location = FastNMS.INSTANCE.field$ResourceKey$location(dimensionKey);
            World world = Bukkit.getWorld(Objects.requireNonNull(NamespacedKey.fromString(location.toString())));
            if (world != null) {
                player.setClientSideWorld(BukkitAdaptors.adapt(world));
                player.clearTrackedChunks();
                player.clearTrackedBlockEntities();
            } else {
                CraftEngine.instance().logger().warn("Failed to handle ClientboundRespawnPacket: World " + location + " does not exist");
            }
        }
    }

    // 1.21.2+
    public static class SyncEntityPositionListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            int entityId = FastNMS.INSTANCE.method$ClientboundEntityPositionSyncPacket$id(packet);
            EntityPacketHandler handler = user.entityPacketHandlers().get(entityId);
            if (handler != null) {
                handler.handleSyncEntityPosition(user, event, packet);
            }
        }
    }

    public static class RenameItemListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!Config.filterAnvil()) return;
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_ANVIL)) {
                return;
            }
            String message;
            try {
                message = (String) NetworkReflections.methodHandle$ServerboundRenameItemPacket$nameGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get message from ServerboundRenameItemPacket", t);
                return;
            }
            if (message != null && !message.isEmpty()) {
                // check bypass
                FontManager manager = CraftEngine.instance().fontManager();
                IllegalCharacterProcessResult result = manager.processIllegalCharacters(message);
                if (result.has()) {
                    try {
                        NetworkReflections.methodHandle$ServerboundRenameItemPacket$nameSetter.invokeExact(packet, result.text());
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Failed to set field 'name' for ServerboundRenameItemPacket", e);
                    }
                }
            }
        }
    }

    public static class SignUpdateListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!Config.filterSign()) return;
            // check bypass
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_SIGN)) {
                return;
            }
            String[] lines;
            try {
                lines = (String[]) NetworkReflections.methodHandle$ServerboundSignUpdatePacket$linesGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get lines from ServerboundSignUpdatePacket", t);
                return;
            }
            FontManager manager = CraftEngine.instance().fontManager();
            if (!manager.isDefaultFontInUse()) return;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line != null && !line.isEmpty()) {
                    IllegalCharacterProcessResult result = manager.processIllegalCharacters(line);
                    if (result.has()) {
                        lines[i] = result.text();
                    }
                }
            }
        }
    }

    public static class EditBookListener implements NMSPacketListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!Config.filterBook()) return;
            FontManager manager = CraftEngine.instance().fontManager();
            if (!manager.isDefaultFontInUse()) return;
            // check bypass
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_BOOK)) {
                return;
            }

            boolean changed = false;

            List<String> pages;
            try {
                pages = (List<String>) NetworkReflections.methodHandle$ServerboundEditBookPacket$pagesGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get pages from ServerboundEditBookPacket", t);
                return;
            }
            List<String> newPages = new ArrayList<>(pages.size());
            Optional<String> title;
            try {
                title = (Optional<String>) NetworkReflections.methodHandle$ServerboundEditBookPacket$titleGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get title from ServerboundEditBookPacket", t);
                return;
            }
            Optional<String> newTitle;

            if (title.isPresent()) {
                String titleStr = title.get();
                Pair<Boolean, String> result = processClientString(titleStr, manager);
                newTitle = Optional.of(result.right());
                if (result.left()) {
                    changed = true;
                }
            } else {
                newTitle = Optional.empty();
            }

            for (String page : pages) {
                Pair<Boolean, String> result = processClientString(page, manager);
                newPages.add(result.right());
                if (result.left()) {
                    changed = true;
                }
            }

            if (changed) {
                try {
                    Object newPacket = NetworkReflections.constructor$ServerboundEditBookPacket.newInstance(
                            (int) NetworkReflections.methodHandle$ServerboundEditBookPacket$slotGetter.invokeExact(packet),
                            newPages,
                            newTitle
                    );
                    event.replacePacket(newPacket);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().warn("Failed to construct ServerboundEditBookPacket", t);
                }
            }
        }

        private static Pair<Boolean, String> processClientString(String original, FontManager manager) {
            if (original.isEmpty()) {
                return Pair.of(false, original);
            }
            int[] codepoints = CharacterUtils.charsToCodePoints(original.toCharArray());
            int[] newCodepoints = new int[codepoints.length];
            boolean hasIllegal = false;
            for (int i = 0; i < codepoints.length; i++) {
                int codepoint = codepoints[i];
                if (manager.isIllegalCodepoint(codepoint)) {
                    newCodepoints[i] = '*';
                    hasIllegal = true;
                } else {
                    newCodepoints[i] = codepoint;
                }
            }
            return hasIllegal ? Pair.of(true, new String(newCodepoints, 0, newCodepoints.length)) : Pair.of(false, original);
        }
    }

    public static class CustomPayloadListener1_20_2 implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!VersionHelper.isOrAbove1_20_2()) return;
            Object payload;
            try {
                payload = NetworkReflections.methodHandle$ServerboundCustomPayloadPacket$payloadGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get payload from ServerboundCustomPayloadPacket", t);
                return;
            }
            Payload clientPayload;
            if (VersionHelper.isOrAbove1_20_5() && NetworkReflections.clazz$DiscardedPayload.isInstance(payload)) {
                clientPayload = DiscardedPayload.from(payload);
            } else if (!VersionHelper.isOrAbove1_20_5() && NetworkReflections.clazz$ServerboundCustomPayloadPacket$UnknownPayload.isInstance(payload)) {
                clientPayload = UnknownPayload.from(payload);
            } else {
                return;
            }
            if (clientPayload == null) return;
            PayloadHelper.handleReceiver(clientPayload, user);
        }
    }

    public static class ResourcePackResponseListener implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            Object action = FastNMS.INSTANCE.field$ServerboundResourcePackPacket$action(packet);

            if (VersionHelper.isOrAbove1_20_3()) {
                UUID uuid = FastNMS.INSTANCE.field$ServerboundResourcePackPacket$id(packet);
                if (!user.isResourcePackLoading(uuid)) {
                    // 不是CraftEngine发送的资源包,不管
                    return;
                }
            }

            if (action == null) {
                user.kick(Component.text("Corrupted ResourcePackResponse Packet"));
                return;
            }

            // 检查是否是拒绝
            if (Config.kickOnDeclined()) {
                if (action == NetworkReflections.instance$ServerboundResourcePackPacket$Action$DECLINED || action == NetworkReflections.instance$ServerboundResourcePackPacket$Action$DISCARDED) {
                    user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                    return;
                }
            }

            // 检查是否失败
            if (Config.kickOnFailedApply()) {
                if (action == NetworkReflections.instance$ServerboundResourcePackPacket$Action$FAILED_DOWNLOAD
                        || (VersionHelper.isOrAbove1_20_3() && action == NetworkReflections.instance$ServerboundResourcePackPacket$Action$INVALID_URL)) {
                    user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                    return;
                }
            }

            boolean isTerminal = action != NetworkReflections.instance$ServerboundResourcePackPacket$Action$ACCEPTED && action != NetworkReflections.instance$ServerboundResourcePackPacket$Action$DOWNLOADED;
            if (isTerminal && VersionHelper.isOrAbove1_20_2()) {
                event.setCancelled(true);
                Object packetListener = FastNMS.INSTANCE.method$Connection$getPacketListener(user.connection());
                if (!CoreReflections.clazz$ServerConfigurationPacketListenerImpl.isInstance(packetListener)) return;
                // 主线程上处理这个包
                CraftEngine.instance().scheduler().executeSync(() -> {
                    try {
                        // 当客户端发出多次成功包的时候，finish会报错，我们忽略他
                        NetworkReflections.methodHandle$ServerCommonPacketListener$handleResourcePackResponse.invokeExact(packetListener, packet);
                        CoreReflections.methodHandle$ServerConfigurationPacketListenerImpl$finishCurrentTask.invokeExact(packetListener, CoreReflections.instance$ServerResourcePackConfigurationTask$TYPE);
                    } catch (Throwable e) {
                        Debugger.RESOURCE_PACK.warn(() -> "Cannot finish current task", e);
                    }
                });
            }
        }
    }

    public static class EntityEventListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            Object player = user.serverPlayer();
            if (player == null) return;
            int entityId;
            try {
                entityId = (int) NetworkReflections.methodHandle$ClientboundEntityEventPacket$entityIdGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get entity id from ClientboundEntityEventPacket", t);
                return;
            }
            if (entityId != FastNMS.INSTANCE.method$Entity$getId(player)) return;
            byte eventId;
            try {
                eventId = (byte) NetworkReflections.methodHandle$ClientboundEntityEventPacket$eventIdGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get event id from ClientboundEntityEventPacket", t);
                return;
            }
            if (eventId >= 24 && eventId <= 28) {
                CraftEngine.instance().fontManager().refreshEmojiSuggestions(user.uuid());
            }
        }
    }

    public static class MovePosAndRotateEntityListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            int entityId = ProtectedFieldVisitor.get().field$ClientboundMoveEntityPacket$entityId(packet);
            if (BukkitFurnitureManager.instance().isFurnitureRealEntity(entityId)) {
                event.setCancelled(true);
            }
            EntityPacketHandler handler = user.entityPacketHandlers().get(entityId);
            if (handler != null) {
                handler.handleMoveAndRotate(user, event, packet);
            }
        }
    }

    public static class MovePosEntityListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            int entityId = ProtectedFieldVisitor.get().field$ClientboundMoveEntityPacket$entityId(packet);
            EntityPacketHandler handler = user.entityPacketHandlers().get(entityId);
            if (handler != null) {
                handler.handleMove(user, event, packet);
            }
        }
    }

    public static class RotateHeadListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            int entityId;
            try {
                entityId = (int) NetworkReflections.methodHandle$ClientboundRotateHeadPacket$entityIdGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get entity id from ClientboundRotateHeadPacket", t);
                return;
            }
            if (BukkitFurnitureManager.instance().isFurnitureRealEntity(entityId)) {
                event.setCancelled(true);
            }
        }
    }

    public static class SetEntityMotionListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!VersionHelper.isOrAbove1_21_6()) return;
            int entityId;
            try {
                entityId = (int) NetworkReflections.methodHandle$ClientboundSetEntityMotionPacket$idGetter.invokeExact(packet);
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to get entity id from ClientboundSetEntityMotionPacket", t);
                return;
            }
            if (BukkitFurnitureManager.instance().isFurnitureRealEntity(entityId)) {
                event.setCancelled(true);
            }
        }
    }

    public static class FinishConfigurationListener implements NMSPacketListener {

        private void returnToWorld(NetWorkUser user, Queue<Object> configurationTasks, Object packetListener) {
            configurationTasks.add(CoreReflections.instance$JoinWorldTask);
            try {
                CoreReflections.methodHandle$ServerConfigurationPacketListenerImpl$startNextTask.invokeExact(packetListener);
            } catch (Throwable e) {
                CraftEngine.instance().logger().warn("Cannot return to world for " + user.name(), e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (!VersionHelper.isOrAbove1_20_2() || !Config.sendPackOnJoin()) {
                // 防止后期调试进配置阶段造成问题
                user.setShouldProcessFinishConfiguration(false);
                return;
            }

            if (!user.shouldProcessFinishConfiguration()) return;
            Object packetListener = FastNMS.INSTANCE.method$Connection$getPacketListener(user.connection());
            if (!CoreReflections.clazz$ServerConfigurationPacketListenerImpl.isInstance(packetListener)) {
                return;
            }

            // 防止后续加入的JoinWorldTask再次处理
            user.setShouldProcessFinishConfiguration(false);

            // 检查用户UUID是否已经校验
            if (!user.isUUIDVerified()) {
                if (Config.strictPlayerUuidValidation()) {
                    TranslationManager.instance().log("warning.network.resource_pack.unverified_uuid", user.name(), user.uuid().toString());
                    user.kick(Component.translatable("disconnect.loginFailedInfo").arguments(Component.translatable("argument.uuid.invalid")));
                    return;
                }
                if (Config.debugResourcePack()) {
                    TranslationManager.instance().log("warning.network.resource_pack.unverified_uuid", user.name(), user.uuid().toString());
                }
            }

            // 取消 ClientboundFinishConfigurationPacket，让客户端发呆，并结束掉当前的进入世界任务
            event.setCancelled(true);
            try {
                CoreReflections.methodHandle$ServerConfigurationPacketListenerImpl$finishCurrentTask.invokeExact(packetListener, CoreReflections.instance$JoinWorldTask$TYPE);
            } catch (Throwable e) {
                CraftEngine.instance().logger().warn("Failed to finish current task for " + user.name(), e);
            }

            if (VersionHelper.isOrAbove1_20_5()) {
                // 1.20.5+开始会检查是否结束需要重新设置回去，不然不会发keepAlive包
                try {
                    CoreReflections.methodHandle$ServerCommonPacketListenerImpl$closedSetter.invokeExact(packetListener, false);
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("Failed to set the 'closed' field of ServerCommonPacketListenerImpl for" + user.name(), e);
                }
            }

            // 请求资源包
            ResourcePackHost host = CraftEngine.instance().packManager().resourcePackHost();
            host.requestResourcePackDownloadLink(user.uuid()).whenComplete((dataList, t) -> {
                Queue<Object> configurationTasks;
                try {
                    configurationTasks = (Queue<Object>) CoreReflections.methodHandle$ServerConfigurationPacketListenerImpl$configurationTasksGetter.invokeExact(packetListener);
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("Failed to get configuration tasks for player " + user.name(), e);
                    FastNMS.INSTANCE.method$ServerConfigurationPacketListenerImpl$returnToWorld(packetListener);
                    return;
                }
                if (t != null) {
                    CraftEngine.instance().logger().warn("Failed to get pack data for player " + user.name(), t);
                    returnToWorld(user, configurationTasks, packetListener);
                    return;
                }
                if (dataList.isEmpty()) {
                    returnToWorld(user, configurationTasks, packetListener);
                    return;
                }
                // 向配置阶段连接的任务重加入资源包的任务
                for (ResourcePackDownloadData data : dataList) {
                    configurationTasks.add(FastNMS.INSTANCE.constructor$ServerResourcePackConfigurationTask(ResourcePackUtils.createServerResourcePackInfo(data.uuid(), data.url(), data.sha1())));
                    user.addResourcePackUUID(data.uuid());
                }
                // 最后再加入一个 JoinWorldTask 并开始资源包任务
                returnToWorld(user, configurationTasks, packetListener);
            });
        }
    }

    public static class LoginFinishedListener implements NMSPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            GameProfile gameProfile = FastNMS.INSTANCE.field$ClientboundLoginFinishedPacket$gameProfile(packet);
            if (VersionHelper.isOrAbove1_21_9()) {
                user.setVerifiedName(gameProfile.name());
                user.setVerifiedUUID(gameProfile.id());
            } else {
                user.setVerifiedName(LegacyAuthLibUtils.getName(gameProfile));
                user.setVerifiedUUID(LegacyAuthLibUtils.getId(gameProfile));
            }
        }
    }

    public static class UpdateTagsListener implements NMSPacketListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
            List<TagUtils.TagEntry> cachedUpdateTags = BukkitBlockManager.instance().cachedUpdateTags();
            if (cachedUpdateTags.isEmpty()) return;
            Map<Object, Object> tags = FastNMS.INSTANCE.field$ClientboundUpdateTagsPacket$tags(packet);
            // 已经替换过了
            if (tags instanceof MarkedHashMap<Object, Object>) return;
            // 需要虚假的block
            if (tags.get(MRegistries.BLOCK) == null) return;
            event.replacePacket(TagUtils.createUpdateTagsPacket(Map.of(MRegistries.BLOCK, cachedUpdateTags), tags));
        }
    }

    public static class ContainerClickListener1_21_5 implements NMSPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
            if (Config.disableItemOperations()) return;
            if (!VersionHelper.PREMIUM && !Config.interceptItem()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            int containerId = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$containerId(packet);
            int stateId = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$stateId(packet);
            short slotNum = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$slotNum(packet);
            byte buttonNum = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$buttonNum(packet);
            Object clickType = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$clickType(packet);
            @SuppressWarnings("unchecked")
            Int2ObjectMap<Object> changedSlots = FastNMS.INSTANCE.field$ServerboundContainerClickPacket$changedSlots(packet);
            Int2ObjectMap<Object> newChangedSlots = new Int2ObjectOpenHashMap<>(changedSlots.size());
            for (Int2ObjectMap.Entry<Object> entry : changedSlots.int2ObjectEntrySet()) {
                newChangedSlots.put(entry.getIntKey(), FastNMS.INSTANCE.constructor$InjectedHashedStack(entry.getValue(), player));
            }
            Object carriedItem = FastNMS.INSTANCE.constructor$InjectedHashedStack(FastNMS.INSTANCE.field$ServerboundContainerClickPacket$carriedItem(packet), player);
            event.replacePacket(FastNMS.INSTANCE.constructor$ServerboundContainerClickPacket(containerId, stateId, slotNum, buttonNum, clickType, Int2ObjectMaps.unmodifiable(newChangedSlots), carriedItem));
        }
    }

    public static class ForgetLevelChunkListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            FriendlyByteBuf buf = event.getBuffer();
            CEWorld ceWorld = BukkitWorldManager.instance().getWorld(player.world().uuid());
            if (VersionHelper.isOrAbove1_20_2()) {
                long chunkPos = buf.readLong();
                user.removeTrackedChunk(chunkPos);
                CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(chunkPos);
                if (ceChunk != null) {
                    ceChunk.despawnBlockEntities(player);
                }
            } else {
                int x = buf.readInt();
                int y = buf.readInt();
                user.removeTrackedChunk(ChunkPos.asLong(x, y));
                CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(x, y);
                if (ceChunk != null) {
                    ceChunk.despawnBlockEntities(player);
                }
            }
        }
    }

    public static class LevelChunkWithLightListener implements ByteBufferPacketListener {
        private static BiFunction<NetWorkUser, PalettedContainer<Integer>, Boolean> biomeRemapper = null;

        public static void setBiomeRemapper(BiFunction<NetWorkUser, PalettedContainer<Integer>, Boolean> remapper) {
            biomeRemapper = remapper;
        }

        public static boolean remapBiomes(NetWorkUser user, PalettedContainer<Integer> biomes) {
            if (biomeRemapper != null) {
                return biomeRemapper.apply(user, biomes);
            }
            return false;
        }

        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;
        private final IntIdentityList biomeList;
        private final IntIdentityList blockList;
        private final boolean needsDowngrade;
        private final Predicate<Integer> occlusionPredicate;

        public LevelChunkWithLightListener(int[] blockStateMapper, int[] modBlockStateMapper, int blockRegistrySize, int biomeRegistrySize, Predicate<Integer> occlusionPredicate) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
            this.biomeList = new IntIdentityList(biomeRegistrySize);
            this.blockList = new IntIdentityList(blockRegistrySize);
            this.needsDowngrade = MiscUtils.ceilLog2(BlockStateUtils.vanillaBlockStateCount()) != MiscUtils.ceilLog2(blockRegistrySize);
            this.occlusionPredicate = occlusionPredicate;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            FriendlyByteBuf buf = event.getBuffer();
            int chunkX = buf.readInt();
            int chunkZ = buf.readInt();
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            boolean named = !VersionHelper.isOrAbove1_20_2();

            int[] remapper = user.clientModEnabled() ? this.modBlockStateMapper : this.blockStateMapper;

            // 读取区块数据
            int heightmapsCount = 0;
            Map<Integer, long[]> heightmapsMap = null;
            net.momirealms.sparrow.nbt.Tag heightmaps = null;
            if (VersionHelper.isOrAbove1_21_5()) {
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

            // 客户端侧section数量很重要，不能读取此时玩家所在的真实世界，包具有滞后性
            net.momirealms.craftengine.core.world.World clientSideWorld = player.clientSideWorld();
            WorldHeight worldHeight = clientSideWorld.worldHeight();
            int count = worldHeight.getSectionsCount();
            MCSection[] sections = new MCSection[count];
            FriendlyByteBuf chunkDataByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(chunkDataBytes));

            boolean hasChangedAnyBlock = false;
            boolean hasGlobalPalette = false;

            // 创建客户端侧世界（只在开启实体情况下创建）
            ClientSection[] clientSections = Config.entityCullingRayTracing() ? new ClientSection[count] : null;

            for (int i = 0; i < count; i++) {
                MCSection mcSection = new MCSection(user.clientBlockList(), this.blockList, this.biomeList);
                mcSection.readPacket(chunkDataByteBuf);

                PalettedContainer<Integer> container = mcSection.blockStateContainer();
                // 重定向生物群系
                if (remapBiomes(user, mcSection.biomeContainer())) {
                    hasChangedAnyBlock = true;
                }

                Palette<Integer> palette = container.data().palette();
                if (palette.canRemap()) {

                    // 重定向方块
                    if (palette.remapAndCheck(s -> remapper[s])) {
                        hasChangedAnyBlock = true;
                    }

                    // 处理客户端侧哪些方块有阻挡
                    if (clientSections != null) {
                        int size = palette.getSize();
                        // 单个元素的情况下，使用优化的存储方案
                        if (size == 1) {
                            clientSections[i] = new ClientSection(new SingularOcclusionStorage(this.occlusionPredicate.test(palette.get(0))));
                        } else {
                            boolean hasOcclusions = false;
                            boolean hasNoOcclusions = false;
                            for (int h = 0; h < size; h++) {
                                int entry = palette.get(h);
                                if (this.occlusionPredicate.test(entry)) {
                                    hasOcclusions = true;
                                } else {
                                    hasNoOcclusions = true;
                                }
                            }
                            // 两种情况都有，那么需要一个个遍历处理视线遮挡数据
                            if (hasOcclusions && hasNoOcclusions) {
                                PackedOcclusionStorage storage = new PackedOcclusionStorage(false);
                                clientSections[i] = new ClientSection(storage);
                                for (int j = 0; j < 4096; j++) {
                                    int state = container.get(j);
                                    storage.set(j, this.occlusionPredicate.test(state));
                                }
                            }
                            // 全遮蔽或全透视则使用优化存储方案
                            else {
                                clientSections[i] = new ClientSection(new SingularOcclusionStorage(hasOcclusions));
                            }
                        }
                    }
                } else {
                    hasGlobalPalette = true;

                    PackedOcclusionStorage storage = null;
                    if (clientSections != null) {
                        storage = new PackedOcclusionStorage(false);
                        clientSections[i] = new ClientSection(storage);
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
                        if (storage != null) {
                            storage.set(j, this.occlusionPredicate.test(state));
                        }
                    }
                }

                sections[i] = mcSection;
            }

            // 只有被修改了，才读后续内容，并改写
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
                if (VersionHelper.isOrAbove1_21_5()) {
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

            // 记录加载的区块
            player.addTrackedChunk(chunkPos.longKey, new ClientChunk(clientSections, worldHeight));

            // 生成方块实体
            CEWorld ceWorld = clientSideWorld.storageWorld();
            // 世界可能被卸载，因为包滞后
            if (ceWorld != null) {
                CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(chunkPos.longKey);
                if (ceChunk != null) {
                    // 生成方块实体
                    ceChunk.spawnBlockEntities(player);
                }
            }
        }
    }

    public static class SectionBlockUpdateListener implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;
        private final Predicate<Integer> occlusionPredicate;

        public SectionBlockUpdateListener(int[] blockStateMapper, int[] modBlockStateMapper, Predicate<Integer> occlusionPredicate) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
            this.occlusionPredicate = occlusionPredicate;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            int[] remapper = user.clientModEnabled() ? this.modBlockStateMapper : this.blockStateMapper;
            FriendlyByteBuf buf = event.getBuffer();
            long sPos = buf.readLong();
            int blocks = buf.readVarInt();
            short[] positions = new short[blocks];
            int[] states = new int[blocks];

            // 获取客户端侧区域
            ClientSection clientSection = null;
            if (Config.entityCullingRayTracing()) {
                SectionPos sectionPos = SectionPos.of(sPos);
                ClientChunk trackedChunk = user.getTrackedChunk(sectionPos.asChunkPos().longKey);
                clientSection = trackedChunk.sectionById(sectionPos.y);
            }

            for (int i = 0; i < blocks; i++) {
                long k = buf.readVarLong();
                short posIndex = (short) ((int) (k & 4095L));
                positions[i] = posIndex;
                int beforeState = ((int) (k >>> 12));
                states[i] = remapper[beforeState];
                if (clientSection != null) {
                    // 设置遮蔽状态
                    BlockPos pos = SectionPos.unpackSectionRelativePos(posIndex);
                    clientSection.setOccluding(pos.x, pos.y, pos.z, this.occlusionPredicate.test(beforeState));
                }
            }

            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeLong(sPos);
            buf.writeVarInt(blocks);
            for (int i = 0; i < blocks; i++) {
                buf.writeVarLong((long) states[i] << 12 | positions[i]);
            }
            event.setChanged(true);
        }
    }

    public static class BlockUpdateListener implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;
        private final Predicate<Integer> occlusionPredicate;

        public BlockUpdateListener(int[] blockStateMapper, int[] modBlockStateMapper, Predicate<Integer> occlusionPredicate) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
            this.occlusionPredicate = occlusionPredicate;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            BlockPos pos = buf.readBlockPos();
            int before = buf.readVarInt();
            if (Config.entityCullingRayTracing()) {
                ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(pos.x >> 4, pos.z >> 4));
                if (trackedChunk != null) {
                    trackedChunk.setOccluding(pos.x, pos.y, pos.z, this.occlusionPredicate.test(before));
                }
            }
            if (user.clientModEnabled() && !BlockStateUtils.isVanillaBlock(before)) {
                return;
            }
            int state = user.clientModEnabled() ? modBlockStateMapper[before] : blockStateMapper[before];
            if (state == before) {
                return;
            }
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBlockPos(pos);
            buf.writeVarInt(state);
        }
    }

    public static class LevelParticleListener1_21_4 implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;

        public LevelParticleListener1_21_4(int[] blockStateMapper, int[] modBlockStateMapper) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            boolean overrideLimiter = buf.readBoolean();
            boolean alwaysShow = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = FastNMS.INSTANCE.method$StreamDecoder$decode(NetworkReflections.instance$ParticleTypes$STREAM_CODEC, FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source()));
            if (option == null) return;
            if (!CoreReflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBoolean(overrideLimiter);
            buf.writeBoolean(alwaysShow);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            FastNMS.INSTANCE.method$StreamEncoder$encode(NetworkReflections.instance$ParticleTypes$STREAM_CODEC, FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source()), remappedOption);
        }
    }

    public static class LevelParticleListener1_20_5 implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;

        public LevelParticleListener1_20_5(int[] blockStateMapper, int[] modBlockStateMapper) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            boolean overrideLimiter = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = FastNMS.INSTANCE.method$StreamDecoder$decode(NetworkReflections.instance$ParticleTypes$STREAM_CODEC, FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source()));
            if (option == null) return;
            if (!CoreReflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBoolean(overrideLimiter);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            FastNMS.INSTANCE.method$StreamEncoder$encode(NetworkReflections.instance$ParticleTypes$STREAM_CODEC, FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source()), remappedOption);
        }
    }

    public static class LevelParticleListener1_20 implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;

        public LevelParticleListener1_20(int[] blockStateMapper, int[] modBlockStateMapper) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            Object particleType = FastNMS.INSTANCE.method$FriendlyByteBuf$readById(buf, MBuiltInRegistries.PARTICLE_TYPE);
            boolean overrideLimiter = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = FastNMS.INSTANCE.method$ClientboundLevelParticlesPacket$readParticle(buf, particleType);
            if (option == null) return;
            if (!CoreReflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            FastNMS.INSTANCE.method$FriendlyByteBuf$writeId(buf, remappedOption, MBuiltInRegistries.PARTICLE_TYPE);
            buf.writeBoolean(overrideLimiter);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            FastNMS.INSTANCE.method$ParticleOptions$writeToNetwork(remappedOption, buf);
        }
    }

    public static class LevelEventListener implements ByteBufferPacketListener {
        private final int[] blockStateMapper;
        private final int[] modBlockStateMapper;

        public LevelEventListener(int[] blockStateMapper, int[] modBlockStateMapper) {
            this.blockStateMapper = blockStateMapper;
            this.modBlockStateMapper = modBlockStateMapper;
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            int eventId = buf.readInt();
            if (eventId != WorldEvents.BLOCK_BREAK_EFFECT) return;
            BlockPos blockPos = buf.readBlockPos();
            int state = buf.readInt();
            // 移除不透明设置
            if (Config.entityCullingRayTracing()) {
                ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(blockPos.x >> 4, blockPos.z >> 4));
                if (trackedChunk != null) {
                    trackedChunk.setOccluding(blockPos.x, blockPos.y, blockPos.z, false);
                }
            }
            boolean global = buf.readBoolean();
            int newState = user.clientModEnabled() ? modBlockStateMapper[state] : blockStateMapper[state];
            Object blockState = BlockStateUtils.idToBlockState(state);
            Object soundType = FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$getSoundType(blockState);
            Object soundEvent = FastNMS.INSTANCE.field$SoundType$breakSound(soundType);
            Object rawSoundId = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            if (BlockStateUtils.isVanillaBlock(state)) {
                if (BukkitBlockManager.instance().isBreakSoundMissing(rawSoundId)) {
                    Key mappedSoundId = BukkitBlockManager.instance().replaceSoundIfExist(KeyUtils.resourceLocationToKey(rawSoundId));
                    if (mappedSoundId != null) {
                        Object packet = FastNMS.INSTANCE.constructor$ClientboundSoundPacket(
                                FastNMS.INSTANCE.method$Holder$direct(FastNMS.INSTANCE.constructor$SoundEvent(KeyUtils.toResourceLocation(mappedSoundId), Optional.empty())),
                                CoreReflections.instance$SoundSource$BLOCKS,
                                blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5, 1f, 0.8F,
                                RandomUtils.generateRandomLong()
                        );
                        user.sendPacket(packet, true);
                    }
                }
            } else {
                Key soundId = KeyUtils.resourceLocationToKey(rawSoundId);
                Key mappedSoundId = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                Object finalSoundId = KeyUtils.toResourceLocation(mappedSoundId == null ? soundId : mappedSoundId);
                Object packet = FastNMS.INSTANCE.constructor$ClientboundSoundPacket(
                        FastNMS.INSTANCE.method$Holder$direct(FastNMS.INSTANCE.constructor$SoundEvent(finalSoundId, Optional.empty())),
                        CoreReflections.instance$SoundSource$BLOCKS,
                        blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5, 1f, 0.8F,
                        RandomUtils.generateRandomLong()
                );
                user.sendPacket(packet, true);
            }
            if (newState == state) {
                return;
            }
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeInt(eventId);
            buf.writeBlockPos(blockPos);
            buf.writeInt(newState);
            buf.writeBoolean(global);
        }
    }

    public static class OpenScreenListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptContainer()) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readVarInt();
            int type = buf.readVarInt();
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(containerId);
            buf.writeVarInt(type);
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
        }
    }

    public static class OpenScreenListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptContainer()) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readVarInt();
            int type = buf.readVarInt();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(containerId);
            buf.writeVarInt(type);
            buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
        }
    }

    @SuppressWarnings({"deprecation", "all"})
    public HoverEvent.ShowItem replaceShowItem(HoverEvent.ShowItem showItem, BukkitServerPlayer player) {
        Object nmsItemStack;
        if (VersionHelper.COMPONENT_RELEASE) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("count", showItem.count());
            itemTag.putString("id", showItem.item().asMinimalString());
            Map<net.kyori.adventure.key.Key, DataComponentValue> components = showItem.dataComponents();
            if (!components.isEmpty()) {
                CompoundTag componentsTag = new CompoundTag();
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = showItem.dataComponentsAs(NBTDataComponentValue.class);
                for (Map.Entry<net.kyori.adventure.key.Key, NBTDataComponentValue> entry : componentsMap.entrySet()) {
                    componentsTag.put(entry.getKey().asMinimalString(), entry.getValue().tag());
                }
                itemTag.put("components", componentsTag);
            }
            DataResult<Object> nmsItemStackResult = CoreReflections.instance$ItemStack$CODEC.parse(MRegistryOps.SPARROW_NBT, itemTag);
            Optional<Object> result = nmsItemStackResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            nmsItemStack = result.get();
        } else {
            Object compoundTag = FastNMS.INSTANCE.constructor$CompoundTag();
            FastNMS.INSTANCE.method$CompoundTag$put(compoundTag, "Count", FastNMS.INSTANCE.constructor$IntTag(showItem.count()));
            FastNMS.INSTANCE.method$CompoundTag$put(compoundTag, "id", FastNMS.INSTANCE.constructor$StringTag(showItem.item().asMinimalString()));
            BinaryTagHolder nbt = showItem.nbt();
            if (nbt != null) {
                try {
                    Object nmsTag = FastNMS.INSTANCE.method$TagParser$parseCompoundFully(nbt.string());
                    FastNMS.INSTANCE.method$CompoundTag$put(compoundTag, "tag", nmsTag);
                } catch (CommandSyntaxException ignored) {
                    return showItem;
                }
            }
            nmsItemStack = FastNMS.INSTANCE.method$ItemStack$of(compoundTag);
        }

        Item<ItemStack> wrap = this.plugin.itemManager().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsItemStack));
        Optional<Item<ItemStack>> remapped = this.plugin.itemManager().s2c(wrap, player);
        if (remapped.isEmpty()) {
            return showItem;
        }

        Item<ItemStack> clientBoundItem = remapped.get();
        net.kyori.adventure.key.Key id = KeyUtils.toAdventureKey(clientBoundItem.vanillaId());
        int count = clientBoundItem.count();
        if (VersionHelper.COMPONENT_RELEASE) {
            DataResult<Tag> tagDataResult = CoreReflections.instance$ItemStack$CODEC.encodeStart(MRegistryOps.SPARROW_NBT, clientBoundItem.getLiteralObject());
            Optional<Tag> result = tagDataResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            CompoundTag itemTag = (CompoundTag) result.get();
            CompoundTag componentsTag = itemTag.getCompound("components");
            if (componentsTag != null) {
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = new HashMap<>();
                for (Map.Entry<String, Tag> entry : componentsTag.entrySet()) {
                    componentsMap.put(net.kyori.adventure.key.Key.key(entry.getKey()), NBTDataComponentValue.of(entry.getValue()));
                }
                return HoverEvent.ShowItem.showItem(id, count, componentsMap);
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        } else {
            Object tag = FastNMS.INSTANCE.method$ItemStack$getTag(clientBoundItem.getLiteralObject());
            if (tag != null) {
                return HoverEvent.ShowItem.showItem(id, count, BinaryTagHolder.binaryTagHolder(tag.toString()));
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        }
    }

    public class SystemChatListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptSystemChat() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String jsonOrPlainString = buf.readUtf();
            Tag tag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(jsonOrPlainString, JsonElement.class));
            Component component = AdventureHelper.nbtToComponent(tag);
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            if (Config.interceptSystemChat()) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(jsonOrPlainString);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> replaceShowItem(s, (BukkitServerPlayer) user));
            }
            buf.writeUtf(MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString());
            buf.writeBoolean(overlay);
        }
    }

    public class SystemChatListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptSystemChat() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            Component component = AdventureHelper.tagToComponent(nbt);
            if (Config.interceptSystemChat()) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> replaceShowItem(s, (BukkitServerPlayer) user));
            }
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
            buf.writeBoolean(overlay);
        }
    }

    public static class TabListListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTabList()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json1 = buf.readUtf();
            String json2 = buf.readUtf();
            Map<String, ComponentProvider> tokens1 = CraftEngine.instance().fontManager().matchTags(json1);
            Map<String, ComponentProvider> tokens2 = CraftEngine.instance().fontManager().matchTags(json2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);
            buf.writeUtf(tokens1.isEmpty() ? json1 : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json1), tokens1, context)));
            buf.writeUtf(tokens2.isEmpty() ? json2 : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json2), tokens2, context)));
        }
    }

    public static class TabListListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTabList()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt1 = buf.readNbt(false);
            if (nbt1 == null) return;
            Tag nbt2 = buf.readNbt(false);
            if (nbt2 == null) return;
            Map<String, ComponentProvider> tokens1 = CraftEngine.instance().fontManager().matchTags(nbt1);
            Map<String, ComponentProvider> tokens2 = CraftEngine.instance().fontManager().matchTags(nbt2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);
            buf.writeNbt(tokens1.isEmpty() ? nbt1 : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt1), tokens1, context)), false);
            buf.writeNbt(tokens2.isEmpty() ? nbt2 : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt2), tokens2, context)), false);
        }
    }

    public static class SetActionBarListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptActionBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
        }
    }

    public static class SetActionBarListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptActionBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
        }
    }

    public static class SetTitleListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTitle()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
        }
    }

    public static class SetTitleListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTitle()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
        }
    }

    public static class SetSubtitleListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTitle()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
        }
    }

    public static class SetSubtitleListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTitle()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
        }
    }

    public static class BossEventListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptBossBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                String json = buf.readUtf();
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
                if (tokens.isEmpty()) return;
                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
                buf.writeFloat(health);
                buf.writeVarInt(color);
                buf.writeVarInt(division);
                buf.writeByte(flag);
            } else if (actionType == 3) {
                String json = buf.readUtf();
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
            }
        }
    }

    public static class BossEventListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptBossBar()) return;
            FriendlyByteBuf buf = event.getBuffer();
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
                if (tokens.isEmpty()) return;
                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                buf.writeFloat(health);
                buf.writeVarInt(color);
                buf.writeVarInt(division);
                buf.writeByte(flag);
            } else if (actionType == 3) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(nbt);
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(nbt), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
            }
        }
    }

    public static class TeamListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTeam()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0)
                return;
            String displayName = buf.readUtf();
            byte friendlyFlags = buf.readByte();
            String nameTagVisibility = buf.readUtf(40);
            String collisionRule = buf.readUtf(40);
            int color = buf.readVarInt();
            String prefix = buf.readUtf();
            String suffix = buf.readUtf();

            Map<String, ComponentProvider> tokens1 = CraftEngine.instance().fontManager().matchTags(displayName);
            Map<String, ComponentProvider> tokens2 = CraftEngine.instance().fontManager().matchTags(prefix);
            Map<String, ComponentProvider> tokens3 = CraftEngine.instance().fontManager().matchTags(suffix);
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;
            event.setChanged(true);
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);

            List<String> entities = method == 0 ? buf.readStringList() : null;
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(name);
            buf.writeByte(method);
            buf.writeUtf(tokens1.isEmpty() ? displayName : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(displayName), tokens1, context)));
            buf.writeByte(friendlyFlags);
            buf.writeUtf(nameTagVisibility);
            buf.writeUtf(collisionRule);
            buf.writeVarInt(color);
            buf.writeUtf(tokens2.isEmpty() ? prefix : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(prefix), tokens2, context)));
            buf.writeUtf(tokens3.isEmpty() ? suffix : AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(suffix), tokens3, context)));
            if (entities != null) {
                buf.writeStringList(entities);
            }
        }
    }

    public static class TeamListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptTeam()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0) return;
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            byte friendlyFlags = buf.readByte();
            Either<String, Integer> eitherVisibility = VersionHelper.isOrAbove1_21_5() ? Either.right(buf.readVarInt()) : Either.left(buf.readUtf(40));
            Either<String, Integer> eitherCollisionRule = VersionHelper.isOrAbove1_21_5() ? Either.right(buf.readVarInt()) : Either.left(buf.readUtf(40));
            int color = buf.readVarInt();
            Tag prefix = buf.readNbt(false);
            if (prefix == null) return;
            Tag suffix = buf.readNbt(false);
            if (suffix == null) return;
            Map<String, ComponentProvider> tokens1 = CraftEngine.instance().fontManager().matchTags(displayName);
            Map<String, ComponentProvider> tokens2 = CraftEngine.instance().fontManager().matchTags(prefix);
            Map<String, ComponentProvider> tokens3 = CraftEngine.instance().fontManager().matchTags(suffix);
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;
            NetworkTextReplaceContext context = NetworkTextReplaceContext.of((BukkitServerPlayer) user);
            List<String> entities = method == 0 ? buf.readStringList() : null;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(name);
            buf.writeByte(method);
            buf.writeNbt(tokens1.isEmpty() ? displayName : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens1, context)), false);
            buf.writeByte(friendlyFlags);
            eitherVisibility.ifLeft(buf::writeUtf).ifRight(buf::writeVarInt);
            eitherCollisionRule.ifLeft(buf::writeUtf).ifRight(buf::writeVarInt);
            buf.writeVarInt(color);
            buf.writeNbt(tokens2.isEmpty() ? prefix : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(prefix), tokens2, context)), false);
            buf.writeNbt(tokens3.isEmpty() ? suffix : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(suffix), tokens3, context)), false);
            if (entities != null) {
                buf.writeStringList(entities);
            }
        }
    }

    public static class SetObjectiveListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptScoreboard()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            String displayName = buf.readUtf();
            int renderType = buf.readVarInt();
            Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(displayName);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(objective);
            buf.writeByte(mode);
            buf.writeUtf(AdventureHelper.componentToJson(AdventureHelper.replaceText(AdventureHelper.jsonToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))));
            buf.writeVarInt(renderType);
        }
    }

    public static class SetObjectiveListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptScoreboard()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            int renderType = buf.readVarInt();
            boolean optionalNumberFormat = buf.readBoolean();
            if (optionalNumberFormat) {
                int format = buf.readVarInt();
                if (format == 0) {
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(displayName);
                    if (tokens.isEmpty()) return;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(0);
                } else if (format == 1) {
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(displayName);
                    if (tokens.isEmpty()) return;
                    Tag style = buf.readNbt(false);
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(1);
                    buf.writeNbt(style, false);
                } else if (format == 2) {
                    Tag fixed = buf.readNbt(false);
                    if (fixed == null) return;
                    Map<String, ComponentProvider> tokens1 = CraftEngine.instance().fontManager().matchTags(displayName);
                    Map<String, ComponentProvider> tokens2 = CraftEngine.instance().fontManager().matchTags(fixed);
                    if (tokens1.isEmpty() && tokens2.isEmpty()) return;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(tokens1.isEmpty() ? displayName : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens1, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(2);
                    buf.writeNbt(tokens2.isEmpty() ? fixed : AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(fixed), tokens2, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                }
            } else {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(displayName);
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUtf(objective);
                buf.writeByte(mode);
                buf.writeNbt(AdventureHelper.componentToTag(AdventureHelper.replaceText(AdventureHelper.tagToComponent(displayName), tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user))), false);
                buf.writeVarInt(renderType);
                buf.writeBoolean(false);
            }
        }
    }

    public static class SetScoreListener1_20_3 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptSetScore()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            boolean isChanged = false;
            FriendlyByteBuf buf = event.getBuffer();
            String owner = buf.readUtf();
            String objectiveName = buf.readUtf();
            int score = buf.readVarInt();
            boolean hasDisplay = buf.readBoolean();
            Tag displayName = null;
            if (hasDisplay) {
                displayName = buf.readNbt(false);
            }
            outside:
            if (displayName != null) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(displayName);
                if (tokens.isEmpty()) break outside;
                Component component = AdventureHelper.tagToComponent(displayName);
                component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
                displayName = AdventureHelper.componentToTag(component);
                isChanged = true;
            }
            boolean hasNumberFormat = buf.readBoolean();
            int format = -1;
            Tag style = null;
            Tag fixed = null;
            if (hasNumberFormat) {
                format = buf.readVarInt();
                if (format == 0) {
                    if (displayName == null) return;
                } else if (format == 1) {
                    if (displayName == null) return;
                    style = buf.readNbt(false);
                } else if (format == 2) {
                    fixed = buf.readNbt(false);
                    if (fixed == null) return;
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(fixed);
                    if (tokens.isEmpty() && !isChanged) return;
                    if (!tokens.isEmpty()) {
                        Component component = AdventureHelper.tagToComponent(fixed);
                        component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
                        fixed = AdventureHelper.componentToTag(component);
                        isChanged = true;
                    }
                }
            }
            if (isChanged) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUtf(owner);
                buf.writeUtf(objectiveName);
                buf.writeVarInt(score);
                if (hasDisplay) {
                    buf.writeBoolean(true);
                    buf.writeNbt(displayName, false);
                } else {
                    buf.writeBoolean(false);
                }
                if (hasNumberFormat) {
                    buf.writeBoolean(true);
                    buf.writeVarInt(format);
                    if (format == 1) {
                        buf.writeNbt(style, false);
                    } else if (format == 2) {
                        buf.writeNbt(fixed, false);
                    }
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

    public static class AddRecipeBookListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (!player.isOnline()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            List<RecipeBookEntry<ItemStack>> entries = buf.readCollection(ArrayList::new, byteBuf -> {
                RecipeBookEntry<ItemStack> entry = RecipeBookEntry.read(byteBuf, __ -> itemManager.wrap(FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf)));
                entry.applyClientboundData(item -> {
                    Optional<Item<ItemStack>> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
                return entry;
            });
            boolean replace = buf.readBoolean();
            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeCollection(entries, ((byteBuf, recipeBookEntry) -> recipeBookEntry.write(byteBuf,
                        (__, item) -> FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, item.getItem()))));
                buf.writeBoolean(replace);
            }
        }
    }

    public static class PlaceGhostRecipeListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!VersionHelper.isOrAbove1_21_2()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            BukkitItemManager itemManager = BukkitItemManager.instance();
            int containerId = buf.readContainerId();
            RecipeDisplay<ItemStack> display = RecipeDisplay.read(buf, __ -> itemManager.wrap(FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf)));
            display.applyClientboundData(item -> {
                Optional<Item<ItemStack>> remapped = itemManager.s2c(item, player);
                if (remapped.isEmpty()) {
                    return item;
                }
                changed.set(true);
                return remapped.get();
            });

            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                display.write(buf, (__, item) -> FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, item.getItem()));
            }
        }
    }

    public static class UpdateRecipesListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            List<LegacyRecipeHolder<ItemStack>> holders = buf.readCollection(ArrayList::new, byteBuf -> {
                LegacyRecipeHolder<ItemStack> holder = LegacyRecipeHolder.read(byteBuf, __ -> itemManager.wrap(FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf)));
                holder.recipe().applyClientboundData(item -> {
                    Optional<Item<ItemStack>> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
                return holder;
            });
            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeCollection(holders, ((byteBuf, recipeHolder)
                        -> recipeHolder.write(byteBuf,
                        (__, item) -> FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, item.getItem()))));
            }
        }
    }

    public static class UpdateRecipesListener1_21_2 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            Map<Key, List<Integer>> itemSets = buf.readMap(
                    FriendlyByteBuf::readKey,
                    b -> b.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt)
            );
            List<SingleInputButtonDisplay<ItemStack>> displays = buf.readCollection(ArrayList::new, b -> {
                SingleInputButtonDisplay<ItemStack> display = SingleInputButtonDisplay.read(b, __ -> itemManager.wrap(FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf)));
                display.applyClientboundData(item -> {
                    Optional<Item<ItemStack>> remapped = itemManager.s2c(item, player);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
                return display;
            });
            if (changed.booleanValue()) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeMap(itemSets,
                        FriendlyByteBuf::writeKey,
                        (b, c) -> b.writeCollection(c, FriendlyByteBuf::writeVarInt)
                );
                buf.writeCollection(displays, (b, d) -> {
                    d.write(b, (__, item) -> FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, item.getItem()));
                });
            }
        }
    }

    public static class UpdateAdvancementsListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations() && !Config.interceptAdvancement()) return;
            MutableBoolean changed = new MutableBoolean(false);
            FriendlyByteBuf buf = event.getBuffer();
            BukkitItemManager itemManager = BukkitItemManager.instance();
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            boolean reset = buf.readBoolean();
            List<AdvancementHolder<ItemStack>> added = buf.readCollection(ArrayList::new, byteBuf -> {
                AdvancementHolder<ItemStack> holder = AdvancementHolder.read(byteBuf, __ -> itemManager.wrap(FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf)));
                if (!Config.disableItemOperations()) {
                    holder.applyClientboundData(item -> {
                        Optional<Item<ItemStack>> remapped = itemManager.s2c(item, player);
                        if (remapped.isEmpty()) {
                            return item;
                        }
                        changed.set(true);
                        return remapped.get();
                    });
                }
                if (Config.interceptAdvancement()) {
                    holder.replaceNetworkTags(component -> {
                        Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(AdventureHelper.componentToJson(component));
                        if (tokens.isEmpty()) return component;
                        changed.set(true);
                        return AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(player));
                    });
                }
                return holder;
            });

            if (changed.booleanValue()) {
                Set<Key> removed = buf.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readKey);
                Map<Key, AdvancementProgress> progress = buf.readMap(FriendlyByteBuf::readKey, AdvancementProgress::read);

                boolean showAdvancement = false;
                if (VersionHelper.isOrAbove1_21_5()) {
                    showAdvancement = buf.readBoolean();
                }

                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());

                buf.writeBoolean(reset);
                buf.writeCollection(added, (byteBuf, advancementHolder) -> advancementHolder.write(byteBuf,
                        (__, item) -> FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, item.getItem())));
                buf.writeCollection(removed, FriendlyByteBuf::writeKey);
                buf.writeMap(progress, FriendlyByteBuf::writeKey, (byteBuf, advancementProgress) -> advancementProgress.write(byteBuf));
                if (VersionHelper.isOrAbove1_21_5()) {
                    buf.writeBoolean(showAdvancement);
                }
            }
        }
    }

    public static class RemoveEntityListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            IntList intList = buf.readIntIdList();
            for (int i = 0, size = intList.size(); i < size; i++) {
                int entityId = intList.getInt(i);
                EntityPacketHandler handler = user.entityPacketHandlers().remove(entityId);
                if (handler != null && handler.handleEntitiesRemove(intList)) {
                    changed = true;
                }
            }
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeIntIdList(intList);
            }
        }
    }

    public static class SoundListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            if (id == 0) {
                Key soundId = buf.readKey();
                Float range = null;
                if (buf.readBoolean()) {
                    range = buf.readFloat();
                }
                int source = buf.readVarInt();
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                float volume = buf.readFloat();
                float pitch = buf.readFloat();
                long seed = buf.readLong();
                Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                if (mapped != null) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(0);
                    buf.writeKey(mapped);
                    if (range != null) {
                        buf.writeBoolean(true);
                        buf.writeFloat(range);
                    } else {
                        buf.writeBoolean(false);
                    }
                    buf.writeVarInt(source);
                    buf.writeInt(x);
                    buf.writeInt(y);
                    buf.writeInt(z);
                    buf.writeFloat(volume);
                    buf.writeFloat(pitch);
                    buf.writeLong(seed);
                }
            } else {
                Optional<Object> optionalSound = FastNMS.INSTANCE.method$IdMap$byId(MBuiltInRegistries.SOUND_EVENT, id - 1);
                if (optionalSound.isEmpty()) return;
                Object soundEvent = optionalSound.get();
                Key soundId = KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$SoundEvent$location(soundEvent));
                int source = buf.readVarInt();
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                float volume = buf.readFloat();
                float pitch = buf.readFloat();
                long seed = buf.readLong();
                Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                if (mapped != null) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(0);
                    Object newId = KeyUtils.toResourceLocation(mapped);
                    Object newSoundEvent = FastNMS.INSTANCE.constructor$SoundEvent(newId, FastNMS.INSTANCE.method$SoundEvent$fixedRange(soundEvent));
                    FastNMS.INSTANCE.method$SoundEvent$directEncode(buf, newSoundEvent);
                    buf.writeVarInt(source);
                    buf.writeInt(x);
                    buf.writeInt(y);
                    buf.writeInt(z);
                    buf.writeFloat(volume);
                    buf.writeFloat(pitch);
                    buf.writeLong(seed);
                }
            }
        }
    }

    public static class ContainerSetContentListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            int stateId = buf.readVarInt();
            int listSize = buf.readVarInt();
            List<ItemStack> items = new ArrayList<>(listSize);
            boolean changed = false;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            for (int i = 0; i < listSize; i++) {
                ItemStack itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, serverPlayer);
                if (optional.isPresent()) {
                    items.add(optional.get());
                    changed = true;
                } else {
                    items.add(itemStack);
                }
            }
            ItemStack carriedItem = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
            ItemStack newCarriedItem = carriedItem;
            Optional<ItemStack> optional = BukkitItemManager.instance().s2c(carriedItem, serverPlayer);
            if (optional.isPresent()) {
                changed = true;
                newCarriedItem = optional.get();
            }
            if (!changed) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeContainerId(containerId);
            buf.writeVarInt(stateId);
            buf.writeVarInt(listSize);
            for (ItemStack itemStack : items) {
                FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, itemStack);
            }
            FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, newCarriedItem);
        }
    }

    public static class ContainerSetSlotListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            int stateId = buf.readVarInt();
            int slot = buf.readShort();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            ItemStack itemStack;
            try {
                itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
            } catch (Exception e) {
                // 其他插件干的，发送了非法的物品
                return;
            }
            BukkitItemManager.instance().s2c(itemStack, serverPlayer).ifPresent((newItemStack) -> {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeVarInt(stateId);
                buf.writeShort(slot);
                FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, newItemStack);
            });
        }
    }

    public static class SetCursorItemListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            ItemStack itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);

            // 为了避免其他插件造成的手感冲突
            if (VersionHelper.isOrAbove1_21_5()) {
                Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(itemStack);
                // 发出来的是非空物品
                if (!wrapped.isEmpty()) {
                    Object containerMenu = FastNMS.INSTANCE.field$Player$containerMenu(serverPlayer.serverPlayer());
                    if (containerMenu != null) {
                        ItemStack carried = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.method$AbstractContainerMenu$getCarried(containerMenu));
                        // 但服务端上实际确是空气，就把它写成空气，避免因为其他插件导致手感问题
                        if (ItemStackUtils.isEmpty(carried)) {
                            event.setChanged(true);
                            buf.clear();
                            buf.writeVarInt(event.packetID());
                            Object newFriendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf);
                            FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(newFriendlyBuf, carried);
                            return;
                        }
                    }
                }
            }

            BukkitItemManager.instance().s2c(itemStack, serverPlayer).ifPresent((newItemStack) -> {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, newItemStack);
            });
        }
    }

    public static class SetEquipmentListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            int entity = buf.readVarInt();
            List<com.mojang.datafixers.util.Pair<Object, ItemStack>> slots = Lists.newArrayList();
            int slotMask;
            do {
                slotMask = buf.readByte();
                Object equipmentSlot = CoreReflections.instance$EquipmentSlot$values[slotMask & 127];
                ItemStack itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, serverPlayer);
                if (optional.isPresent()) {
                    changed = true;
                    itemStack = optional.get();
                }
                slots.add(com.mojang.datafixers.util.Pair.of(equipmentSlot, itemStack));
            } while ((slotMask & -128) != 0);
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(entity);
                int i = slots.size();
                for (int j = 0; j < i; ++j) {
                    com.mojang.datafixers.util.Pair<Object, ItemStack> pair = slots.get(j);
                    Enum<?> equipmentSlot = (Enum<?>) pair.getFirst();
                    boolean bl = j != i - 1;
                    int k = equipmentSlot.ordinal();
                    buf.writeByte(bl ? k | -128 : k);
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, pair.getSecond());
                }
            }
        }
    }

    public static class SetPlayerInventoryListener1_21_2 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int slot = buf.readVarInt();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            ItemStack itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
            BukkitItemManager.instance().s2c(itemStack, serverPlayer).ifPresent((newItemStack) -> {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(slot);
                FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, newItemStack);
            });
        }
    }

    public static class SetCreativeModeSlotListener implements ByteBufferPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            if (!serverPlayer.isCreativeMode()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            short slotNum = buf.readShort();
            ItemStack itemStack;
            try {
                itemStack = VersionHelper.isOrAbove1_20_5() ?
                        FastNMS.INSTANCE.method$FriendlyByteBuf$readUntrustedItem(friendlyBuf) : FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
            } catch (Exception e) {
                return;
            }
            BukkitItemManager.instance().c2s(itemStack).ifPresent((newItemStack) -> {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeShort(slotNum);
                if (VersionHelper.isOrAbove1_20_5()) {
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeUntrustedItem(friendlyBuf, newItemStack);
                } else {
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, newItemStack);
                }
            });
        }
    }

    public static class ContainerClick1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!VersionHelper.PREMIUM && !Config.interceptItem()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            int containerId = buf.readContainerId();
            int stateId = buf.readVarInt();
            short slotNum = buf.readShort();
            byte buttonNum = buf.readByte();
            int clickType = buf.readVarInt();
            int i = buf.readVarInt();
            Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>(i);
            for (int j = 0; j < i; ++j) {
                int k = buf.readShort();
                ItemStack itemStack = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                Optional<ItemStack> optional = BukkitItemManager.instance().c2s(itemStack);
                if (optional.isPresent()) {
                    changed = true;
                    itemStack = optional.get();
                }
                changedSlots.put(k, itemStack);
            }
            ItemStack carriedItem = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
            Optional<ItemStack> optional = BukkitItemManager.instance().c2s(carriedItem);
            if (optional.isPresent()) {
                changed = true;
                carriedItem = optional.get();
            }
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeVarInt(stateId);
                buf.writeShort(slotNum);
                buf.writeByte(buttonNum);
                buf.writeVarInt(clickType);
                buf.writeVarInt(changedSlots.size());
                for (Map.Entry<Integer, ItemStack> entry : changedSlots.int2ObjectEntrySet()) {
                    buf.writeShort(entry.getKey());
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, entry.getValue());
                }
                FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, carriedItem);
            }
        }
    }

    public class InteractEntityListener implements ByteBufferPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            int entityId = hasModelEngine() ? plugin.compatibilityManager().interactionToBaseEntity(buf.readVarInt()) : buf.readVarInt();
            BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByEntityId(entityId);
            if (furniture == null) return;
            int actionType = buf.readVarInt();
            BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
            if (serverPlayer.isSpectatorMode()) return;
            Player platformPlayer = serverPlayer.platformPlayer();
            Location location = furniture.baseEntity().getLocation();

            Runnable mainThreadTask;
            if (actionType == 1) {
                // ATTACK
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.baseEntityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.baseEntityId());
                    buf.writeVarInt(actionType);
                    buf.writeBoolean(usingSecondaryAction);
                }

                mainThreadTask = () -> {
                    // todo 冒险模式破坏工具白名单
                    if (serverPlayer.isAdventureMode() ||
                            !furniture.isValid()) return;

                    // todo 重构家具时候注意，需要准备加载好的hitbox类，以获取hitbox坐标
                    if (!serverPlayer.canInteractPoint(new Vec3d(location.getX(), location.getY(), location.getZ()), 16d)) {
                        return;
                    }

                    FurnitureAttemptBreakEvent preBreakEvent = new FurnitureAttemptBreakEvent(serverPlayer.platformPlayer(), furniture);
                    if (EventUtils.fireAndCheckCancel(preBreakEvent))
                        return;

                    if (!BukkitCraftEngine.instance().antiGriefProvider().canBreak(platformPlayer, location))
                        return;

                    FurnitureBreakEvent breakEvent = new FurnitureBreakEvent(serverPlayer.platformPlayer(), furniture);
                    if (EventUtils.fireAndCheckCancel(breakEvent))
                        return;

                    Cancellable cancellable = Cancellable.of(breakEvent::isCancelled, breakEvent::setCancelled);
                    // execute functions
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.FURNITURE, furniture)
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withParameter(DirectContextParameters.HAND, InteractionHand.MAIN_HAND)
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(InteractionHand.MAIN_HAND))
                            .withParameter(DirectContextParameters.POSITION, furniture.position())
                    );
                    furniture.config().execute(context, EventTrigger.LEFT_CLICK);
                    furniture.config().execute(context, EventTrigger.BREAK);
                    if (cancellable.isCancelled()) {
                        return;
                    }

                    CraftEngineFurniture.remove(furniture, serverPlayer, !serverPlayer.isCreativeMode(), true);
                };
            } else if (actionType == 2) {
                // INTERACT_AT
                float x = buf.readFloat();
                float y = buf.readFloat();
                float z = buf.readFloat();
                InteractionHand hand = buf.readVarInt() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.baseEntityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.baseEntityId());
                    buf.writeVarInt(actionType);
                    buf.writeFloat(x).writeFloat(y).writeFloat(z);
                    buf.writeVarInt(hand == InteractionHand.MAIN_HAND ? 0 : 1);
                    buf.writeBoolean(usingSecondaryAction);
                }

                mainThreadTask = () -> {
                    if (!furniture.isValid()) {
                        return;
                    }

                    // 先检查碰撞箱部分是否存在
                    HitBoxPart hitBoxPart = furniture.hitBoxPartByEntityId(entityId);
                    if (hitBoxPart == null) return;
                    Vec3d pos = hitBoxPart.pos();
                    // 检测距离
                    if (!serverPlayer.canInteractPoint(pos, 16d)) {
                        return;
                    }
                    // 检测
                    Location eyeLocation = platformPlayer.getEyeLocation();
                    Vector direction = eyeLocation.getDirection();
                    Location endLocation = eyeLocation.clone();
                    endLocation.add(direction.multiply(serverPlayer.getCachedInteractionRange()));
                    Optional<EntityHitResult> result = hitBoxPart.aabb().clip(LocationUtils.toVec3d(eyeLocation), LocationUtils.toVec3d(endLocation));
                    if (result.isEmpty()) {
                        return;
                    }
                    EntityHitResult hitResult = result.get();
                    Vec3d hitLocation = hitResult.hitLocation();
                    // 获取正确的交互点
                    Location interactionPoint = new Location(platformPlayer.getWorld(), hitLocation.x, hitLocation.y, hitLocation.z);

                    HitBox hitbox = furniture.hitBoxByEntityId(entityId);
                    if (hitbox == null) {
                        return;
                    }

                    // 触发事件
                    FurnitureInteractEvent interactEvent = new FurnitureInteractEvent(serverPlayer.platformPlayer(), furniture, hand, interactionPoint, hitbox);
                    if (EventUtils.fireAndCheckCancel(interactEvent)) {
                        return;
                    }

                    // 执行事件动作
                    Item<ItemStack> itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                    Cancellable cancellable = Cancellable.of(interactEvent::isCancelled, interactEvent::setCancelled);
                    // execute functions
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder()
                            .withParameter(DirectContextParameters.EVENT, cancellable)
                            .withParameter(DirectContextParameters.FURNITURE, furniture)
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                            .withParameter(DirectContextParameters.HAND, hand)
                            .withParameter(DirectContextParameters.POSITION, furniture.position())
                    );
                    furniture.config().execute(context, EventTrigger.RIGHT_CLICK);
                    if (cancellable.isCancelled()) {
                        return;
                    }

                    // 必须从网络包层面处理，否则无法获取交互的具体实体
                    if (serverPlayer.isSecondaryUseActive() && !itemInHand.isEmpty() && hitbox.config().canUseItemOn()) {
                        Optional<CustomItem<ItemStack>> optionalCustomItem = itemInHand.getCustomItem();
                        if (optionalCustomItem.isPresent() && !optionalCustomItem.get().behaviors().isEmpty()) {
                            for (ItemBehavior behavior : optionalCustomItem.get().behaviors()) {
                                if (behavior instanceof FurnitureItemBehavior) {
                                    behavior.useOnBlock(new UseOnContext(serverPlayer, InteractionHand.MAIN_HAND, new BlockHitResult(hitResult.hitLocation(), hitResult.direction(), BlockPos.fromVec3d(hitResult.hitLocation()), false)));
                                    return;
                                }
                            }
                        }
                        // now simulate vanilla item behavior
                        serverPlayer.setResendSound();
                        FastNMS.INSTANCE.simulateInteraction(
                                serverPlayer.serverPlayer(),
                                DirectionUtils.toNMSDirection(hitResult.direction()),
                                hitResult.hitLocation().x, hitResult.hitLocation().y, hitResult.hitLocation().z,
                                LocationUtils.toBlockPos(hitResult.blockPos())
                        );
                    } else {
                        if (!serverPlayer.isSecondaryUseActive()) {
                            for (Seat<HitBox> seat : hitbox.seats()) {
                                if (!seat.isOccupied()) {
                                    if (seat.spawnSeat(serverPlayer, furniture.position())) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                };
            } else if (actionType == 0) {
                int hand = buf.readVarInt();
                boolean usingSecondaryAction = buf.readBoolean();
                if (entityId != furniture.baseEntityId()) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(furniture.baseEntityId());
                    buf.writeVarInt(actionType);
                    buf.writeVarInt(hand);
                    buf.writeBoolean(usingSecondaryAction);
                }
                return;
            } else {
                return;
            }

            if (VersionHelper.isFolia()) {
                platformPlayer.getScheduler().run(BukkitCraftEngine.instance().javaPlugin(), t -> mainThreadTask.run(), () -> {});
            } else {
                BukkitCraftEngine.instance().scheduler().executeSync(mainThreadTask);
            }
        }
    }

    public static class CustomPayloadListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
            if (VersionHelper.isOrAbove1_20_2()) return;
            FriendlyByteBuf byteBuf = event.getBuffer();
            Key key = byteBuf.readKey();
            PayloadHelper.handleReceiver(new UnknownPayload(key, byteBuf.readBytes(byteBuf.readableBytes())), user);
        }
    }

    public class AddEntityListener implements ByteBufferPacketListener {
        private final EntityTypeHandler[] handlers;

        public AddEntityListener(int entityTypes) {
            this.handlers = new EntityTypeHandler[entityTypes];
            Arrays.fill(this.handlers, EntityTypeHandler.DoNothing.INSTANCE);
            this.handlers[MEntityTypes.BLOCK_DISPLAY$registryId] = simpleAddEntityHandler(BlockDisplayPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.TEXT_DISPLAY$registryId] = simpleAddEntityHandler(TextDisplayPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.ARMOR_STAND$registryId] = simpleAddEntityHandler(ArmorStandPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.ITEM$registryId] = simpleAddEntityHandler(CommonItemPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.ITEM_FRAME$registryId] = simpleAddEntityHandler(ItemFramePacketHandler.INSTANCE);
            this.handlers[MEntityTypes.GLOW_ITEM_FRAME$registryId] = simpleAddEntityHandler(ItemFramePacketHandler.INSTANCE);
            this.handlers[MEntityTypes.ENDERMAN$registryId] = simpleAddEntityHandler(EndermanPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.CHEST_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.COMMAND_BLOCK_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.FURNACE_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.HOPPER_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.SPAWNER_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.TNT_MINECART$registryId] = simpleAddEntityHandler(MinecartPacketHandler.INSTANCE);
            this.handlers[MEntityTypes.FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.EYE_OF_ENDER$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.FIREWORK_ROCKET$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.SMALL_FIREBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.EGG$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.ENDER_PEARL$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.EXPERIENCE_BOTTLE$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.SNOWBALL$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.POTION$registryId] = createOptionalCustomProjectileEntityHandler(true);
            this.handlers[MEntityTypes.TRIDENT$registryId] = createOptionalCustomProjectileEntityHandler(false);
            this.handlers[MEntityTypes.ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
            this.handlers[MEntityTypes.SPECTRAL_ARROW$registryId] = createOptionalCustomProjectileEntityHandler(false);
            if (VersionHelper.isOrAbove1_20_3()) {
                this.handlers[MEntityTypes.TNT$registryId] = simpleAddEntityHandler(PrimedTNTPacketHandler.INSTANCE);
            }
            if (VersionHelper.isOrAbove1_20_5()) {
                this.handlers[MEntityTypes.OMINOUS_ITEM_SPAWNER$registryId] = simpleAddEntityHandler(CommonItemPacketHandler.INSTANCE);
            }
            this.handlers[MEntityTypes.FALLING_BLOCK$registryId] = (user, event) -> {
                FriendlyByteBuf buf = event.getBuffer();
                int id = buf.readVarInt();
                UUID uuid = buf.readUUID();
                int type = buf.readVarInt();
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                Vec3d movement = VersionHelper.isOrAbove1_21_9() ? buf.readLpVec3() : null;
                byte xRot = buf.readByte();
                byte yRot = buf.readByte();
                byte yHeadRot = buf.readByte();
                int data = buf.readVarInt();
                // Falling blocks
                int remapped = remapBlockState(data, user.clientModEnabled());
                if (remapped != data) {
                    int xa = VersionHelper.isOrAbove1_21_9() ? -1 : buf.readShort();
                    int ya = VersionHelper.isOrAbove1_21_9() ? -1 : buf.readShort();
                    int za = VersionHelper.isOrAbove1_21_9() ? -1 : buf.readShort();
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(id);
                    buf.writeUUID(uuid);
                    buf.writeVarInt(type);
                    buf.writeDouble(x);
                    buf.writeDouble(y);
                    buf.writeDouble(z);
                    if (VersionHelper.isOrAbove1_21_9()) buf.writeLpVec3(movement);
                    buf.writeByte(xRot);
                    buf.writeByte(yRot);
                    buf.writeByte(yHeadRot);
                    buf.writeVarInt(remapped);
                    if (!VersionHelper.isOrAbove1_21_9()) buf.writeShort(xa);
                    if (!VersionHelper.isOrAbove1_21_9()) buf.writeShort(ya);
                    if (!VersionHelper.isOrAbove1_21_9()) buf.writeShort(za);
                }
            };
            this.handlers[MEntityTypes.ITEM_DISPLAY$registryId] = (user, event) -> {
                FriendlyByteBuf buf = event.getBuffer();
                int id = buf.readVarInt();
                BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(id);
                if (furniture != null) {
                    user.entityPacketHandlers().put(id, new FurniturePacketHandler(furniture.fakeEntityIds()));
                    user.sendPacket(furniture.spawnPacket((Player) user.platformPlayer()), false);
                    if (Config.hideBaseEntity() && !furniture.hasExternalModel()) {
                        event.setCancelled(true);
                    }
                } else {
                    user.entityPacketHandlers().put(id, ItemDisplayPacketHandler.INSTANCE);
                }
            };
            this.handlers[MEntityTypes.INTERACTION$registryId] = (user, event) -> {
                if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != MEntityTypes.INTERACTION) return;
                FriendlyByteBuf buf = event.getBuffer();
                int id = buf.readVarInt();
                // Cancel collider entity packet
                BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(id);
                if (furniture != null) {
                    event.setCancelled(true);
                    user.entityPacketHandlers().put(id, FurnitureCollisionPacketHandler.INSTANCE);
                }
            };
            this.handlers[MEntityTypes.OAK_BOAT$registryId] = (user, event) -> {
                if (BukkitFurnitureManager.NMS_COLLISION_ENTITY_TYPE != MEntityTypes.OAK_BOAT) return;
                FriendlyByteBuf buf = event.getBuffer();
                int id = buf.readVarInt();
                // Cancel collider entity packet
                BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(id);
                if (furniture != null) {
                    event.setCancelled(true);
                    user.entityPacketHandlers().put(id, FurnitureCollisionPacketHandler.INSTANCE);
                }
            };
        }

        private static EntityTypeHandler simpleAddEntityHandler(EntityPacketHandler handler) {
            return (user, event) -> {
                FriendlyByteBuf buf = event.getBuffer();
                user.entityPacketHandlers().put(buf.readVarInt(), handler);
            };
        }

        private static EntityTypeHandler createOptionalCustomProjectileEntityHandler(boolean fallback) {
            return (user, event) -> {
                FriendlyByteBuf buf = event.getBuffer();
                int id = buf.readVarInt();
                BukkitProjectileManager.instance().projectileByEntityId(id).ifPresentOrElse(customProjectile -> {
                    ProjectilePacketHandler handler = new ProjectilePacketHandler(customProjectile, id);
                    handler.convertAddCustomProjectilePacket(buf, event);
                    user.entityPacketHandlers().put(id, handler);
                }, () -> {
                    if (fallback) {
                        user.entityPacketHandlers().put(id, CommonItemPacketHandler.INSTANCE);
                    }
                });
            };
        }

        public interface EntityTypeHandler {

            void handle(NetWorkUser user, ByteBufPacketEvent event);

            class DoNothing implements EntityTypeHandler {
                public static final DoNothing INSTANCE = new DoNothing();

                @Override
                public void handle(NetWorkUser user, ByteBufPacketEvent event) {
                }
            }
        }

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            buf.readVarInt();
            buf.readUUID();
            int type = buf.readVarInt();
            this.handlers[type].handle(user, event);
        }
    }

    public static class SetEntityDataListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            EntityPacketHandler handler = user.entityPacketHandlers().get(id);
            if (handler != null) {
                handler.handleSetEntityData(serverPlayer, event);
                return;
            }
            if (Config.interceptEntityName()) {
                boolean isChanged = false;
                List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
                for (int i = 0; i < packedItems.size(); i++) {
                    Object packedItem = packedItems.get(i);
                    int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                    if (entityDataId != BaseEntityData.CustomName.id()) continue;
                    @SuppressWarnings("unchecked")
                    Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                    if (optionalTextComponent.isEmpty()) continue;
                    Object textComponent = optionalTextComponent.get();
                    String json = ComponentUtils.minecraftToJson(textComponent);
                    Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
                    if (tokens.isEmpty()) continue;
                    Component component = AdventureHelper.jsonToComponent(json);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
                    Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                    packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))));
                    isChanged = true;
                    break;
                }
                if (isChanged) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(id);
                    FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
                }
            }
        }
    }

    public static class MerchantOffersListener1_20 implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            BukkitItemManager manager = BukkitItemManager.instance();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            List<MerchantOffer<ItemStack>> merchantOffers = buf.readCollection(ArrayList::new, byteBuf -> {
                ItemStack cost1 = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                ItemStack result = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                ItemStack cost2 = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                boolean outOfStock = byteBuf.readBoolean();
                int uses = byteBuf.readInt();
                int maxUses = byteBuf.readInt();
                int xp = byteBuf.readInt();
                int specialPrice = byteBuf.readInt();
                float priceMultiplier = byteBuf.readFloat();
                int demand = byteBuf.readInt();
                return new MerchantOffer<>(manager.wrap(cost1), Optional.of(manager.wrap(cost2)), manager.wrap(result), outOfStock, uses, maxUses, xp, specialPrice, priceMultiplier, demand);
            });

            MutableBoolean changed = new MutableBoolean(false);
            for (MerchantOffer<ItemStack> offer : merchantOffers) {
                offer.applyClientboundData(item -> {
                    Optional<Item<ItemStack>> remapped = manager.s2c(item, serverPlayer);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
            }

            if (changed.booleanValue()) {
                int villagerLevel = buf.readVarInt();
                int villagerXp = buf.readVarInt();
                boolean showProgress = buf.readBoolean();
                boolean canRestock = buf.readBoolean();

                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeCollection(merchantOffers, (byteBuf, offer) -> {
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, offer.cost1().getItem());
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, offer.result().getItem());
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, offer.cost2().get().getItem());
                    byteBuf.writeBoolean(offer.outOfStock());
                    byteBuf.writeInt(offer.uses());
                    byteBuf.writeInt(offer.maxUses());
                    byteBuf.writeInt(offer.xp());
                    byteBuf.writeInt(offer.specialPrice());
                    byteBuf.writeFloat(offer.priceMultiplier());
                    byteBuf.writeInt(offer.demand());
                });

                buf.writeVarInt(villagerLevel);
                buf.writeVarInt(villagerXp);
                buf.writeBoolean(showProgress);
                buf.writeBoolean(canRestock);
            }
        }
    }

    public static class MerchantOffersListener1_20_5 implements ByteBufferPacketListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableItemOperations()) return;
            if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readContainerId();
            BukkitItemManager manager = BukkitItemManager.instance();
            Object friendlyBuf = FastNMS.INSTANCE.constructor$FriendlyByteBuf(buf.source());
            List<MerchantOffer<ItemStack>> merchantOffers = buf.readCollection(ArrayList::new, byteBuf -> {
                ItemStack cost1 = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.field$ItemCost$itemStack(FastNMS.INSTANCE.method$StreamDecoder$decode(NetworkReflections.instance$ItemCost$STREAM_CODEC, friendlyBuf)));
                ItemStack result = FastNMS.INSTANCE.method$FriendlyByteBuf$readItem(friendlyBuf);
                Optional<ItemStack> cost2 = ((Optional<Object>) FastNMS.INSTANCE.method$StreamDecoder$decode(NetworkReflections.instance$ItemCost$OPTIONAL_STREAM_CODEC, friendlyBuf))
                        .map(cost -> FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.field$ItemCost$itemStack(cost)));
                boolean outOfStock = byteBuf.readBoolean();
                int uses = byteBuf.readInt();
                int maxUses = byteBuf.readInt();
                int xp = byteBuf.readInt();
                int specialPrice = byteBuf.readInt();
                float priceMultiplier = byteBuf.readFloat();
                int demand = byteBuf.readInt();
                return new MerchantOffer<>(manager.wrap(cost1), cost2.map(manager::wrap), manager.wrap(result), outOfStock, uses, maxUses, xp, specialPrice, priceMultiplier, demand);
            });

            MutableBoolean changed = new MutableBoolean(false);
            for (MerchantOffer<ItemStack> offer : merchantOffers) {
                offer.applyClientboundData(item -> {
                    Optional<Item<ItemStack>> remapped = manager.s2c(item, serverPlayer);
                    if (remapped.isEmpty()) {
                        return item;
                    }
                    changed.set(true);
                    return remapped.get();
                });
            }

            if (changed.booleanValue()) {
                int villagerLevel = buf.readVarInt();
                int villagerXp = buf.readVarInt();
                boolean showProgress = buf.readBoolean();
                boolean canRestock = buf.readBoolean();

                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeContainerId(containerId);
                buf.writeCollection(merchantOffers, (byteBuf, offer) -> {
                    FastNMS.INSTANCE.method$StreamEncoder$encode(NetworkReflections.instance$ItemCost$STREAM_CODEC, friendlyBuf, itemStackToItemCost(offer.cost1().getLiteralObject(), offer.cost1().count()));
                    FastNMS.INSTANCE.method$FriendlyByteBuf$writeItem(friendlyBuf, offer.result().getItem());
                    FastNMS.INSTANCE.method$StreamEncoder$encode(NetworkReflections.instance$ItemCost$OPTIONAL_STREAM_CODEC, friendlyBuf, offer.cost2().map(it -> itemStackToItemCost(it.getLiteralObject(), it.count())));
                    byteBuf.writeBoolean(offer.outOfStock());
                    byteBuf.writeInt(offer.uses());
                    byteBuf.writeInt(offer.maxUses());
                    byteBuf.writeInt(offer.xp());
                    byteBuf.writeInt(offer.specialPrice());
                    byteBuf.writeFloat(offer.priceMultiplier());
                    byteBuf.writeInt(offer.demand());
                });

                buf.writeVarInt(villagerLevel);
                buf.writeVarInt(villagerXp);
                buf.writeBoolean(showProgress);
                buf.writeBoolean(canRestock);
            }
        }

        private Object itemStackToItemCost(Object itemStack, int count) {
            return FastNMS.INSTANCE.constructor$ItemCost(
                    FastNMS.INSTANCE.method$Item$builtInRegistryHolder(FastNMS.INSTANCE.method$ItemStack$getItem(itemStack)),
                    count,
                    FastNMS.INSTANCE.method$DataComponentExactPredicate$allOf(FastNMS.INSTANCE.method$ItemStack$getComponents(itemStack))
            );
        }
    }

    public static class BlockEntityDataListener implements ByteBufferPacketListener {

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptItem()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            BlockPos pos = buf.readBlockPos();
            int entityType = buf.readVarInt();
            boolean named = !VersionHelper.isOrAbove1_20_2();
            CompoundTag tag = (CompoundTag) buf.readNbt(named);
            // todo 刷怪笼里的物品？

            // 通用方块实体存储的物品
            if (tag != null && tag.containsKey("Items")) {
                BukkitItemManager itemManager = BukkitItemManager.instance();
                ListTag itemsTag = tag.getList("Items");
                List<Pair<Byte, ItemStack>> items = new ArrayList<>();
                for (Tag itemTag : itemsTag) {
                    if (itemTag instanceof CompoundTag itemCompoundTag) {
                        byte slot = itemCompoundTag.getByte("Slot");
                        Object nmsStack;
                        if (VersionHelper.isOrAbove1_20_5()) {
                            nmsStack = CoreReflections.instance$ItemStack$CODEC.parse(MRegistryOps.SPARROW_NBT, itemCompoundTag)
                                    .resultOrPartial((error) -> CraftEngine.instance().logger().severe("Tried to parse invalid item: '" + error + "'")).orElse(null);
                        } else {
                            Object nmsTag = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.NBT, itemTag);
                            nmsStack = FastNMS.INSTANCE.method$ItemStack$of(nmsTag);
                        }
                        ItemStack bukkitStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsStack);
                        Optional<ItemStack> optional = itemManager.s2c(bukkitStack, (BukkitServerPlayer) user);
                        if (optional.isPresent()) {
                            changed = true;
                            items.add(new Pair<>(slot, optional.get()));
                        } else {
                            items.add(Pair.of(slot, bukkitStack));
                        }
                    }
                }
                if (changed) {
                    ListTag newItemsTag = new ListTag();
                    for (Pair<Byte, ItemStack> pair : items) {
                        CompoundTag newItemCompoundTag;
                        if (VersionHelper.isOrAbove1_20_5()) {
                            newItemCompoundTag = (CompoundTag) CoreReflections.instance$ItemStack$CODEC.encodeStart(MRegistryOps.SPARROW_NBT, FastNMS.INSTANCE.field$CraftItemStack$handle(pair.right()))
                                    .resultOrPartial((error) -> CraftEngine.instance().logger().severe("Tried to encode invalid item: '" + error + "'")).orElse(null);
                        } else {
                            Object nmsTag = FastNMS.INSTANCE.method$itemStack$save(FastNMS.INSTANCE.field$CraftItemStack$handle(pair.right()), FastNMS.INSTANCE.constructor$CompoundTag());
                            newItemCompoundTag = (CompoundTag) MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, nmsTag);
                        }
                        if (newItemCompoundTag != null) {
                            newItemCompoundTag.putByte("Slot", pair.left());
                            newItemsTag.add(newItemCompoundTag);
                        }
                    }
                    tag.put("Items", newItemsTag);
                }
            }
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeBlockPos(pos);
                buf.writeVarInt(entityType);
                buf.writeNbt(tag, named);
            }
        }
    }

    public static class PlayerChatListener_1_20 implements ByteBufferPacketListener {

        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptPlayerChat()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            UUID sender = buf.readUUID();
            int index = buf.readVarInt();
            byte @Nullable [] messageSignature = buf.readNullable(b -> {
                byte[] bs = new byte[256];
                buf.readBytes(bs);
                return bs;
            });
            // SignedMessageBody.Packed start
            String content = buf.readUtf(256);
            Instant timeStamp = buf.readInstant();
            long salt = buf.readLong();
            // LastSeenMessages.Packed start
            ArrayList<Pair<Integer, byte[]>> lastSeen = buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), b -> {
                int i = b.readVarInt() - 1;
                if (i == -1) {
                    byte[] bs = new byte[256];
                    buf.readBytes(bs);
                    return Pair.of(-1, bs);
                } else {
                    return Pair.of(i, null);
                }
            });
            // LastSeenMessages.Packed end
            // SignedMessageBody.Packed end
            @Nullable String unsignedContent = buf.readNullable(FriendlyByteBuf::readUtf);
            if (unsignedContent != null) {
                Map<String, ComponentProvider> unsignedContentTokens = CraftEngine.instance().fontManager().matchTags(unsignedContent);
                if (!unsignedContentTokens.isEmpty()) {
                    Tag tag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(unsignedContent, JsonElement.class));
                    Component component = AdventureHelper.nbtToComponent(tag);
                    component = AdventureHelper.replaceText(component, unsignedContentTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    unsignedContent = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                    changed = true;
                }
            }
            // FilterMask start
            int type = buf.readVarInt();
            BitSet mask = type == 2 /* PARTIALLY_FILTERED */ ? buf.readBitSet() : null;
            // FilterMask end
            // ChatType.BoundNetwork start
            int chatType = buf.readVarInt();
            String name = buf.readUtf();
            Map<String, ComponentProvider> nameTokens = CraftEngine.instance().fontManager().matchTags(name);
            if (!nameTokens.isEmpty()) {
                Tag tag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(name, JsonElement.class));
                Component component = AdventureHelper.nbtToComponent(tag);
                component = AdventureHelper.replaceText(component, nameTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                name = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                changed = true;
            }
            @Nullable String targetName = buf.readNullable(FriendlyByteBuf::readUtf);
            if (targetName != null) {
                Map<String, ComponentProvider> targetNameTokens = CraftEngine.instance().fontManager().matchTags(targetName);
                if (!targetNameTokens.isEmpty()) {
                    Tag tag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(targetName, JsonElement.class));
                    Component component = AdventureHelper.nbtToComponent(tag);
                    component = AdventureHelper.replaceText(component, targetNameTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    targetName = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                    changed = true;
                }
            }
            // ChatType.BoundNetwork end
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(sender);
                buf.writeVarInt(index);
                buf.writeNullable(messageSignature, (b, bs) -> buf.writeBytes(bs));
                buf.writeUtf(content);
                buf.writeInstant(timeStamp);
                buf.writeLong(salt);
                buf.writeCollection(lastSeen, (b, pair) -> {
                    b.writeVarInt(pair.left() + 1);
                    if (pair.right() != null) {
                        b.writeBytes(pair.right());
                    }
                });
                buf.writeNullable(unsignedContent, FriendlyByteBuf::writeUtf);
                buf.writeVarInt(type);
                if (type == 2) buf.writeBitSet(mask);
                buf.writeVarInt(chatType);
                buf.writeUtf(name);
                buf.writeNullable(targetName, FriendlyByteBuf::writeUtf);
            }
        }
    }

    public static class PlayerChatListener_1_20_3 implements ByteBufferPacketListener {

        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptPlayerChat()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            int globalIndex = VersionHelper.isOrAbove1_21_5() ? buf.readVarInt() : -1;
            UUID sender = buf.readUUID();
            int index = buf.readVarInt();
            byte @Nullable [] messageSignature = buf.readNullable(b -> {
                byte[] bs = new byte[256];
                buf.readBytes(bs);
                return bs;
            });
            // SignedMessageBody.Packed start
            String content = buf.readUtf(256);
            Instant timeStamp = buf.readInstant();
            long salt = buf.readLong();
            // LastSeenMessages.Packed start
            ArrayList<Pair<Integer, byte[]>> lastSeen = buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), b -> {
                int i = b.readVarInt() - 1;
                if (i == -1) {
                    byte[] bs = new byte[256];
                    buf.readBytes(bs);
                    return Pair.of(-1, bs);
                } else {
                    return Pair.of(i, null);
                }
            });
            // LastSeenMessages.Packed end
            // SignedMessageBody.Packed end
            @Nullable Tag unsignedContent = buf.readNullable(b -> b.readNbt(false));
            if (unsignedContent != null) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(unsignedContent);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(unsignedContent);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    unsignedContent = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            // FilterMask start
            int type = buf.readVarInt();
            BitSet mask = type == 2 /* PARTIALLY_FILTERED */ ? buf.readBitSet() : null;
            // FilterMask end
            // ChatType.Bound start
            int chatType = buf.readVarInt();
            Tag name = buf.readNbt(false);
            if (name != null) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(name);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(name);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    name = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            @Nullable Tag targetName = buf.readNullable(b -> b.readNbt(false));
            if (targetName != null) {
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(targetName);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(targetName);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    targetName = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            // ChatType.Bound end
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                if (VersionHelper.isOrAbove1_21_5()) buf.writeVarInt(globalIndex);
                buf.writeUUID(sender);
                buf.writeVarInt(index);
                buf.writeNullable(messageSignature, (b, bs) -> buf.writeBytes(bs));
                buf.writeUtf(content);
                buf.writeInstant(timeStamp);
                buf.writeLong(salt);
                buf.writeCollection(lastSeen, (b, pair) -> {
                    b.writeVarInt(pair.left() + 1);
                    if (pair.right() != null) {
                        b.writeBytes(pair.right());
                    }
                });
                buf.writeNullable(unsignedContent, (b, tag) -> b.writeNbt(tag, false));
                buf.writeVarInt(type);
                if (type == 2) buf.writeBitSet(mask);
                buf.writeVarInt(chatType);
                buf.writeNbt(name, false);
                buf.writeNullable(targetName, (b, tag) -> b.writeNbt(tag, false));
            }
        }
    }
}
