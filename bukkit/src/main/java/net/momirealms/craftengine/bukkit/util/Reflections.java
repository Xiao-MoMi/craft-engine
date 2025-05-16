package net.momirealms.craftengine.bukkit.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.io.BufferedReader;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unused")
public class Reflections {

    public static void init() {
    }

    public static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$CraftChatMessage = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("util.CraftChatMessage")
            )
    );

    public static final Method method$CraftChatMessage$fromJSON = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftChatMessage,
                    new String[]{"fromJSON"},
                    String.class
            )
    );

    public static final Class<?> clazz$Component = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.IChatBaseComponent",
                    "network.chat.Component"
            )
    );

    public static final Method method$Component$getString = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Component, String.class, new String[]{"getString", "a"}
            )
    );

    public static final Class<?> clazz$RandomSource = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("util.RandomSource")
            )
    );


    public static final Class<?> clazz$ClientboundSetActionBarTextPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetActionBarTextPacket")
            )
    );

    public static final Field field$ClientboundSetActionBarTextPacket$text = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSetActionBarTextPacket, clazz$Component, 0
            )
    );

    public static final Constructor<?> constructor$ClientboundSetActionBarTextPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundSetActionBarTextPacket, clazz$Component
            )
    );

    public static final Class<?> clazz$ComponentContents = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.ComponentContents")
            )
    );

    public static final Method method$Component$getContents = requireNonNull(
            ReflectionUtils.getMethods(
                    clazz$Component, clazz$ComponentContents
            ).get(0)
    );

    public static final Class<?> clazz$ScoreContents = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.contents.ScoreContents")
            )
    );

    public static final Field field$ScoreContents$name = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ScoreContents, String.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundSystemChatPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSystemChatPacket")
            )
    );

    public static final Constructor<?> constructor$ClientboundSystemChatPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundSystemChatPacket, clazz$Component, boolean.class)
    );

    public static final Field field$ClientboundSystemChatPacket$overlay = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, boolean.class, 0
            )
    );

    public static final Class<?> clazz$LevelWriter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IWorldWriter",
                    "world.level.LevelWriter"
            )
    );

    public static final Class<?> clazz$LevelReader = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IWorldReader",
                    "world.level.LevelReader"
            )
    );

    public static final Class<?> clazz$DimensionType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.dimension.DimensionManager",
                    "world.level.dimension.DimensionType"
            )
    );

    public static final Method method$$LevelReader$dimensionType = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelReader, clazz$DimensionType
            )
    );

    public static final Field field$DimensionType$minY = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$DimensionType, int.class, 0
            )
    );

    public static final Field field$DimensionType$height = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$DimensionType, int.class, 1
            )
    );

    public static final Field field$ClientboundSystemChatPacket$component =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, clazz$Component, 0
            );

    public static final Field field$ClientboundSystemChatPacket$adventure$content =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, Component.class, 0
            );

    public static final Field field$ClientboundSystemChatPacket$text =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, String.class, 0
            );

    public static final Class<?> clazz$ClientboundBossEventPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBoss",
                    "network.protocol.game.ClientboundBossEventPacket"
            )
    );

    public static final Class<?> clazz$ClientboundBossEventPacket$Operation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBoss$Action",
                    "network.protocol.game.ClientboundBossEventPacket$Operation"
            )
    );

    public static final Constructor<?> constructor$ClientboundBossEventPacket = requireNonNull(
            ReflectionUtils.getDeclaredConstructor(
                    clazz$ClientboundBossEventPacket,
                    UUID.class, clazz$ClientboundBossEventPacket$Operation
            )
    );

    public static final Class<?> clazz$ClientboundBossEventPacket$AddOperation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBoss$a",
                    "network.protocol.game.ClientboundBossEventPacket$AddOperation"
            )
    );


    public static final Class<?> clazz$BossEvent$BossBarColor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.BossBattle$BarColor",
                    "world.BossEvent$BossBarColor"
            )
    );

    public static final Method method$BossEvent$BossBarColor$valueOf = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BossEvent$BossBarColor,
                    new String[]{"valueOf"},
                    String.class
            )
    );

    public static final Class<?> clazz$BossEvent$BossBarOverlay = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.BossBattle$BarStyle",
                    "world.BossEvent$BossBarOverlay"
            )
    );

    public static final Method method$BossEvent$BossBarOverlay$valueOf = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BossEvent$BossBarOverlay,
                    new String[]{"valueOf"},
                    String.class
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$name = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    0
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$progress = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    1
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$color = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    2
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$overlay = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    3
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$darkenScreen = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    4
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$playMusic = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    5
            )
    );

    public static final Field field$ClientboundBossEventPacket$AddOperation$createWorldFog = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBossEventPacket$AddOperation,
                    6
            )
    );

    public static final Class<?> clazz$ResourceLocation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "resources.MinecraftKey",
                    "resources.ResourceLocation"
            )
    );

    public static Object allocateAddOperationInstance() throws InstantiationException {
        return UNSAFE.allocateInstance(clazz$ClientboundBossEventPacket$AddOperation);
    }

    public static final Class<?> clazz$ClientboundBossEventPacket$UpdateNameOperation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBoss$e",
                    "network.protocol.game.ClientboundBossEventPacket$UpdateNameOperation"
            )
    );

    public static final Constructor<?> constructor$ClientboundBossEventPacket$UpdateNameOperation = requireNonNull(
            ReflectionUtils.getDeclaredConstructor(
                    clazz$ClientboundBossEventPacket$UpdateNameOperation,
                    clazz$Component
            )
    );

    public static final Class<?> clazz$SoundEvent = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "sounds.SoundEffect",
                    "sounds.SoundEvent"
            )
    );

    public static final Constructor<?> constructor$SoundEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
            ReflectionUtils.getConstructor(
                    clazz$SoundEvent, clazz$ResourceLocation, Optional.class
            ) :
            ReflectionUtils.getDeclaredConstructor(
                    clazz$SoundEvent, clazz$ResourceLocation, float.class, boolean.class
            )
    );

    // 1.21.2+
    public static final Field field$SoundEvent$fixedRange = ReflectionUtils.getInstanceDeclaredField(
            clazz$SoundEvent, Optional.class, 0
    );

    // 1.21.2-
    public static final Field field$SoundEvent$range = ReflectionUtils.getInstanceDeclaredField(
            clazz$SoundEvent, float.class, 0
    );

    public static final Field field$SoundEvent$newSystem = ReflectionUtils.getInstanceDeclaredField(
            clazz$SoundEvent, boolean.class, 0
    );

    public static final Method method$SoundEvent$createVariableRangeEvent = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$SoundEvent, clazz$SoundEvent, clazz$ResourceLocation
            )
    );

    public static final Class<?> clazz$CraftRegistry = ReflectionUtils.getClazz(
        BukkitReflectionUtils.assembleCBClass("CraftRegistry")
    );

    public static final Object instance$MinecraftRegistry;

    static {
        if (VersionHelper.isOrAbove1_20()) {
            try {
                Method method = requireNonNull(ReflectionUtils.getMethod(clazz$CraftRegistry, new String[]{"getMinecraftRegistry"}));
                instance$MinecraftRegistry = method.invoke(null);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            instance$MinecraftRegistry = null;
        }
    }

    public static final Class<?> clazz$Component$Serializer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.IChatBaseComponent$ChatSerializer",
                    "network.chat.Component$Serializer"
            )
    );

    public static final Class<?> clazz$HolderLookup$Provider = BukkitReflectionUtils.findReobfOrMojmapClass(
            VersionHelper.isOrAbove1_20_5() ? "core.HolderLookup$a" : "core.HolderLookup$b",
            "core.HolderLookup$Provider"
    );

    @Deprecated
    public static final Method method$Component$Serializer$fromJson0 = ReflectionUtils.getMethod(
            clazz$Component$Serializer,
            new String[] { "fromJson" },
            String.class, clazz$HolderLookup$Provider
    );

    @Deprecated
    public static final Method method$Component$Serializer$fromJson1 = ReflectionUtils.getMethod(
            clazz$Component$Serializer,
            new String[] { "fromJson" },
            JsonElement.class, clazz$HolderLookup$Provider
    );

    @Deprecated
    public static final Method method$Component$Serializer$toJson = ReflectionUtils.getMethod(
            clazz$Component$Serializer,
            new String[] { "toJson" },
            clazz$Component, clazz$HolderLookup$Provider
    );

    public static final Class<?> clazz$ClientboundBundlePacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBundlePacket")
            )
    );

//    public static final Constructor<?> constructor$ClientboundBundlePacket = requireNonNull(
//            ReflectionUtils.getConstructor(clazz$ClientboundBundlePacket, Iterable.class)
//    );

    public static final Class<?> clazz$Packet = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.Packet")
            )
    );

    public static final Class<?> clazz$ServerPlayer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.EntityPlayer",
                    "server.level.ServerPlayer"
            )
    );

    public static final Class<?> clazz$ServerGamePacketListenerImpl = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.network.PlayerConnection",
                    "server.network.ServerGamePacketListenerImpl"
            )
    );

    public static final Class<?> clazz$ServerCommonPacketListenerImpl = requireNonNull(
            clazz$ServerGamePacketListenerImpl.getSuperclass()
    );

    public static final Class<?> clazz$Connection = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.NetworkManager",
                    "network.Connection"
            )
    );

    public static final Field field$ServerCommonPacketListenerImpl$connection = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getDeclaredField(
                    clazz$ServerCommonPacketListenerImpl, clazz$Connection, 0
            ) :
            ReflectionUtils.getDeclaredField(
                    clazz$ServerGamePacketListenerImpl, clazz$Connection, 0
            )
    );

    public static final Class<?> clazz$PacketSendListener = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.PacketSendListener")
            )
    );

    public static final Class<?> clazz$CraftPlayer = requireNonNull(ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleCBClass("entity.CraftPlayer")
    ));

//    public static final Method method$CraftPlayer$getHandle = requireNonNull(
//            ReflectionUtils.getMethod(clazz$CraftPlayer, new String[] { "getHandle" })
//    );

    public static final Field field$ServerPlayer$connection = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$ServerPlayer, clazz$ServerGamePacketListenerImpl, 0)
    );

    @Deprecated
    public static final Method method$ServerGamePacketListenerImpl$sendPacket = requireNonNull(
            ReflectionUtils.getMethods(clazz$ServerGamePacketListenerImpl, void.class, clazz$Packet).get(0)
    );

    public static final Method method$Connection$sendPacketImmediate = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getDeclaredMethod(
                    clazz$Connection, void.class, new String[] {"sendPacket", "b"}, clazz$Packet, clazz$PacketSendListener, boolean.class
            ) :
            ReflectionUtils.getDeclaredMethod(
                    clazz$Connection, void.class, new String[] {"sendPacket"}, clazz$Packet, clazz$PacketSendListener, Boolean.class
            )
    );

    public static final Field field$Channel = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Connection, Channel.class, 0
            )
    );

    public static final Field field$BundlePacket$packets = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBundlePacket.getSuperclass(), Iterable.class, 0
            )
    );

    public static final Class<?> clazz$EntityType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityTypes",
                    "world.entity.EntityType"
            )
    );

    public static final Class<?> clazz$EntityType$EntityFactory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityTypes$b",
                    "world.entity.EntityType$EntityFactory"
            )
    );

    public static final Field field$EntityType$factory = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$EntityType, clazz$EntityType$EntityFactory, 0
            )
    );

    public static final Class<?> clazz$ClientboundAddEntityPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutSpawnEntity",
                    "network.protocol.game.ClientboundAddEntityPacket"
            )
    );

    public static final Class<?> clazz$VoxelShape = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.phys.shapes.VoxelShape")
            )
    );

    @Deprecated
    public static final Field field$ClientboundAddEntityPacket$data = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundAddEntityPacket, int.class, 4
            )
    );

    public static final Field field$ClientboundAddEntityPacket$type = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundAddEntityPacket, clazz$EntityType, 0
            )
    );

    public static final Class<?> clazz$ClientboundAddPlayerPacket =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutNamedEntitySpawn",
                    "network.protocol.game.ClientboundAddPlayerPacket"
            );

    public static final Class<?> clazz$ClientboundRemoveEntitiesPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityDestroy",
                    "network.protocol.game.ClientboundRemoveEntitiesPacket"
            )
    );

//    public static final Field field$ClientboundRemoveEntitiesPacket$entityIds = requireNonNull(
//            ReflectionUtils.getInstanceDeclaredField(
//                    clazz$ClientboundRemoveEntitiesPacket, 0
//            )
//    );

    @Deprecated
    public static final Field field$ClientboundAddEntityPacket$entityId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundAddEntityPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundAddPlayerPacket$entityId = clazz$ClientboundAddPlayerPacket != null ?
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundAddPlayerPacket, int.class, 0
            ) : null;

    public static final Class<?> clazz$Vec3 = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.Vec3D",
                    "world.phys.Vec3"
            )
    );

    @Deprecated
    public static final Field field$Vec3$x = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Vec3, double.class, 0
            )
    );

    @Deprecated
    public static final Field field$Vec3$y = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Vec3, double.class, 1
            )
    );

    @Deprecated
    public static final Field field$Vec3$z = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Vec3, double.class, 2
            )
    );

    public static final Constructor<?> constructor$Vec3 = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$Vec3, double.class, double.class, double.class
            )
    );

//    public static final Constructor<?> constructor$ClientboundAddEntityPacket = requireNonNull(
//            ReflectionUtils.getConstructor(clazz$ClientboundAddEntityPacket,
//                    int.class, UUID.class,
//                    double.class, double.class, double.class,
//                    float.class, float.class,
//                    clazz$EntityType,
//                    int.class, clazz$Vec3, double.class
//            )
//    );

    public static final Constructor<?> constructor$ClientboundRemoveEntitiesPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundRemoveEntitiesPacket, int[].class)
    );

    public static final Class<?> clazz$ClientboundSetPassengersPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutMount",
                    "network.protocol.game.ClientboundSetPassengersPacket"
            )
    );

    public static final Field field$ClientboundSetPassengersPacket$vehicle = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPassengersPacket, 0
            )
    );

    public static final Field field$ClientboundSetPassengersPacket$passengers = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPassengersPacket, 1
            )
    );

//    public static Object allocateClientboundSetPassengersPacketInstance() throws InstantiationException {
//            return UNSAFE.allocateInstance(clazz$ClientboundSetPassengersPacket);
//    }

    public static final Field field$Vec3$Zero = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Vec3, clazz$Vec3, 0
            )
    );

    public static final Object instance$Vec3$Zero;

    static {
        try {
            instance$Vec3$Zero = field$Vec3$Zero.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Class<?> clazz$ClientboundSetEntityDataPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityMetadata",
                    "network.protocol.game.ClientboundSetEntityDataPacket"
            )
    );

//    public static final Constructor<?> constructor$ClientboundSetEntityDataPacket = requireNonNull(
//            ReflectionUtils.getConstructor(clazz$ClientboundSetEntityDataPacket,
//                    int.class, List.class)
//    );

    public static final Class<?> clazz$EntityDataSerializers = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcherRegistry",
                    "network.syncher.EntityDataSerializers"
            )
    );

    public static final Class<?> clazz$EntityDataSerializer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcherSerializer",
                    "network.syncher.EntityDataSerializer"
            )
    );

    public static final Class<?> clazz$EntityDataAccessor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcherObject",
                    "network.syncher.EntityDataAccessor"
            )
    );

//    public static final Constructor<?> constructor$EntityDataAccessor = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$EntityDataAccessor, int.class, clazz$EntityDataSerializer
//            )
//    );

    public static final Class<?> clazz$SynchedEntityData = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcher",
                    "network.syncher.SynchedEntityData"
            )
    );

    public static final Method method$SynchedEntityData$get = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$SynchedEntityData, Object.class, clazz$EntityDataAccessor
            )
    );

    public static final Class<?> clazz$SynchedEntityData$DataValue = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcher$b",
                    "network.syncher.SynchedEntityData$DataValue"
            )
    );

//    public static final Method method$SynchedEntityData$DataValue$create = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$SynchedEntityData$DataValue, clazz$SynchedEntityData$DataValue, clazz$EntityDataAccessor, Object.class
//            )
//    );

    public static final Method method$Component$empty = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Component, clazz$Component
            )
    );

    public static final Object instance$Component$empty;

    static {
        try {
            instance$Component$empty = method$Component$empty.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Class<?> clazz$Quaternionf = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.joml.Quaternionf"
            )
    );

    public static final Constructor<?> constructor$Quaternionf = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$Quaternionf, float.class, float.class, float.class, float.class
            )
    );

    public static final Object instance$Quaternionf$None;

    static {
        try {
            instance$Quaternionf$None = constructor$Quaternionf.newInstance(0f, 0f, 0f, 1f);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Class<?> clazz$Vector3f = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.joml.Vector3f"
            )
    );

    public static final Constructor<?> constructor$Vector3f = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$Vector3f, float.class, float.class, float.class
            )
    );

    public static final Object instance$Vector3f$None;
    public static final Object instance$Vector3f$Normal;

    static {
        try {
            instance$Vector3f$None = constructor$Vector3f.newInstance(0f, 0f, 0f);
            instance$Vector3f$Normal = constructor$Vector3f.newInstance(1f, 1f, 1f);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

//    public static final Field field$ClientboundSetEntityDataPacket$id = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$ClientboundSetEntityDataPacket, int.class, 0
//            )
//    );

//    public static final Field field$ClientboundSetEntityDataPacket$packedItems = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$ClientboundSetEntityDataPacket, List.class, 0
//            )
//    );

//    public static final Field field$SynchedEntityData$DataValue$id = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$SynchedEntityData$DataValue, int.class, 0
//            )
//    );

//    public static final Field field$SynchedEntityData$DataValue$serializer = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$SynchedEntityData$DataValue, 1
//            )
//    );
//
//    public static final Field field$SynchedEntityData$DataValue$value = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$SynchedEntityData$DataValue, 2
//            )
//    );

    public static final Class<?> clazz$ClientboundUpdateAttributesPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutUpdateAttributes",
                    "network.protocol.game.ClientboundUpdateAttributesPacket"
            )
    );

    public static final Class<?> clazz$AttributeInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.AttributeModifiable",
                    "world.entity.ai.attributes.AttributeInstance"
            )
    );

    public static final Method method$AttributeInstance$getValue = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$AttributeInstance, double.class, new String[]{"getValue", "f"}
            )
    );

    public static final Class<?> clazz$AttributeModifier = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier")
            )
    );

    public static final Class<?> clazz$AttributeModifier$Operation = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier$Operation")
            )
    );

    public static final Method method$AttributeModifier$Operation$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$AttributeModifier$Operation, clazz$AttributeModifier$Operation.arrayType()
            )
    );

    public static final Object instance$AttributeModifier$Operation$ADD_VALUE;

    static {
        try {
            Object[] values = (Object[]) method$AttributeModifier$Operation$values.invoke(null);
            instance$AttributeModifier$Operation$ADD_VALUE = values[0];
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Constructor<?> constructor$AttributeModifier = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getConstructor(clazz$AttributeModifier, String.class, double.class, clazz$AttributeModifier$Operation):
            (
                !VersionHelper.isOrAbove1_21() ?
                ReflectionUtils.getConstructor(clazz$AttributeModifier, UUID.class, String.class, double.class, clazz$AttributeModifier$Operation) :
                (
                        ReflectionUtils.getConstructor(clazz$AttributeModifier, clazz$ResourceLocation, double.class, clazz$AttributeModifier$Operation)
                )
            )
    );

    public static final Constructor<?> constructor$ClientboundUpdateAttributesPacket0 = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundUpdateAttributesPacket, 0
            )
    );

    public static final Constructor<?> constructor$ClientboundUpdateAttributesPacket1 = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundUpdateAttributesPacket, 1
            )
    );

    public static final Field field$ClientboundUpdateAttributesPacket$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundUpdateAttributesPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundUpdateAttributesPacket$attributes = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundUpdateAttributesPacket, List.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutUpdateAttributes$AttributeSnapshot",
                    "network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot"
            )
    );

    public static final Class<?> clazz$Holder = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.Holder")
            )
    );

    // 1.20.6+
    public static final Method method$Holder$getRegisteredName =
            ReflectionUtils.getMethod(
                    clazz$Holder, String.class
            );

    public static final Class<?> clazz$Holder$Reference = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.Holder$c",
                    "core.Holder$Reference"
            )
    );

    public static final Field field$ClientboundUpdateAttributesPacket$AttributeSnapshot$attribute =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot, clazz$Holder, 0
            );

    public static final Field field$ClientboundUpdateAttributesPacket$AttributeSnapshot$base =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot, double.class, 0
            );

    public static final Field field$ClientboundUpdateAttributesPacket$AttributeSnapshot$modifiers =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot, Collection.class, 0
            );

    public static final Method method$Holder$value = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Holder, new String[]{"a", "value"}
            )
    );

    public static final Method method$Holder$direct = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Holder, clazz$Holder, Object.class
            )
    );

    public static final Class<?> clazz$Attribute = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.AttributeBase",
                    "world.entity.ai.attributes.Attribute"
            )
    );

    public static final Constructor<?> constructor$ClientboundUpdateAttributesPacket$AttributeSnapshot = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
                    ReflectionUtils.getConstructor(
                            clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot, clazz$Holder, double.class, Collection.class
                    ) :
                    ReflectionUtils.getConstructor(
                            clazz$ClientboundUpdateAttributesPacket$AttributeSnapshot, clazz$Attribute, double.class, Collection.class
                    )
    );

    public static final Field field$Attribute$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Attribute, String.class, 0
            )
    );

    public static final Field field$AttributeModifier$amount = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AttributeModifier, double.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundGameEventPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutGameStateChange",
                    "network.protocol.game.ClientboundGameEventPacket"
            )
    );

    public static final Class<?> clazz$ClientboundGameEventPacket$Type = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutGameStateChange$a",
                    "network.protocol.game.ClientboundGameEventPacket$Type"
            )
    );

    public static final Field field$ClientboundGameEventPacket$event = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundGameEventPacket, clazz$ClientboundGameEventPacket$Type, 0
            )
    );

    public static final Field field$ClientboundGameEventPacket$param = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundGameEventPacket, float.class, 0
            )
    );

    public static final Field field$ClientboundGameEventPacket$Type$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundGameEventPacket$Type, int.class, 0
            )
    );

    public static final Class<?> clazz$GameType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.EnumGamemode",
                    "world.level.GameType"
            )
    );

    public static final Method method$GameType$getId = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$GameType, new String[] { "getId", "a" }
            )
    );

    public static final Class<?> clazz$Biome = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.biome.BiomeBase",
                    "world.level.biome.Biome"
            )
    );

    public static final Class<?> clazz$CraftWorld = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("CraftWorld")
            )
    );

    public static final Class<?> clazz$ServerLevel = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.WorldServer",
                    "server.level.ServerLevel"
            )
    );

//    @Deprecated
//    public static final Field field$CraftWorld$ServerLevel = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$CraftWorld, clazz$ServerLevel, 0
//            )
//    );

    public static final Method method$ServerLevel$getNoiseBiome = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerLevel, clazz$Holder, int.class, int.class, int.class
            )
    );

    public static final Class<?> clazz$ResourceKey = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("resources.ResourceKey")
            )
    );

    public static final Field field$ResourceKey$registry = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ResourceKey, clazz$ResourceLocation, 0
            )
    );

    @Deprecated
    public static final Field field$ResourceKey$location = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ResourceKey, clazz$ResourceLocation, 1
            )
    );

    public static final Method method$ResourceKey$create = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ResourceKey, clazz$ResourceKey, clazz$ResourceKey, clazz$ResourceLocation
            )
    );

    public static final Class<?> clazz$MinecraftServer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.MinecraftServer")
            )
    );

    public static final Method method$MinecraftServer$getServer = requireNonNull(
            ReflectionUtils.getMethod(clazz$MinecraftServer, new String[] { "getServer" })
    );

    public static final Class<?> clazz$LayeredRegistryAccess = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.LayeredRegistryAccess")
            )
    );

    public static final Field field$MinecraftServer$registries = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$MinecraftServer, clazz$LayeredRegistryAccess, 0
            )
    );

    public static final Class<?> clazz$RegistryAccess$Frozen = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.IRegistryCustom$Dimension",
                    "core.RegistryAccess$Frozen"
            )
    );

    public static final Class<?> clazz$RegistryAccess = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.IRegistryCustom",
                    "core.RegistryAccess"
            )
    );

    public static final Field field$LayeredRegistryAccess$composite = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$LayeredRegistryAccess, clazz$RegistryAccess$Frozen, 0
            )
    );

    public static final Class<?> clazz$Registry = requireNonNull(
            requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.IRegistryWritable",
                    "core.WritableRegistry"
            )).getInterfaces()[0]
    );

    public static final Method method$RegistryAccess$registryOrThrow = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$RegistryAccess, clazz$Registry, clazz$ResourceKey
            )
    );

    public static final Method method$Registry$register = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Registry, Object.class, clazz$Registry, clazz$ResourceLocation, Object.class
            )
    );

    public static final Method method$Registry$registerForHolder = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Registry, clazz$Holder$Reference, clazz$Registry, clazz$ResourceLocation, Object.class
            )
    );

    public static final Method method$Holder$Reference$bindValue = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Holder$Reference, void.class, Object.class
            )
    );

    public static final Class<?> clazz$Registries = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.registries.Registries")
            )
    );

    public static final Class<?> clazz$DefaultedRegistry = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryBlocks",
                    "core.DefaultedRegistry"
            )
    );

    public static final Method method$Registry$getKey = requireNonNull(
            ReflectionUtils.getMethod(clazz$Registry, clazz$ResourceLocation, Object.class)
    );

    public static final Method method$Registry$get = requireNonNull(
            ReflectionUtils.getMethods(
                    clazz$Registry, Object.class, clazz$ResourceLocation
            ).stream().filter(m -> m.getReturnType() != Optional.class).findAny().orElse(null)
    );

    // use ResourceLocation
    public static final Method method$Registry$getHolder0;
    // use ResourceKey
    public static final Method method$Registry$getHolder1;

    static {
        List<Method> methods = ReflectionUtils.getMethods(
                clazz$Registry, Optional.class, clazz$ResourceLocation
        );
        Method theMethod1 = null;
        for (Method method : methods) {
            Type returnType = method.getGenericReturnType();
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
                if (returnType instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length == 1) {
                        if (actualTypeArguments[0] instanceof ParameterizedType) {
                            theMethod1 = method;
                        }
                    }
                }
            }
        }
        method$Registry$getHolder0 = theMethod1;
    }

    static {
        List<Method> methods = ReflectionUtils.getMethods(
                clazz$Registry, Optional.class, clazz$ResourceKey
        );
        Method theMethod1 = null;
        for (Method method : methods) {
            Type returnType = method.getGenericReturnType();
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceKey) {
                if (returnType instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length == 1) {
                        if (actualTypeArguments[0] instanceof ParameterizedType) {
                            theMethod1 = method;
                        }
                    }
                }
            }
        }
        method$Registry$getHolder1 = theMethod1;
    }

    public static final Class<?> clazz$ClientboundSetPlayerTeamPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutScoreboardTeam",
                    "network.protocol.game.ClientboundSetPlayerTeamPacket"
            )
    );

    public static final Field field$ClientboundSetPlayerTeamPacket$method = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPlayerTeamPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundSetPlayerTeamPacket$players = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPlayerTeamPacket, Collection.class, 0
            )
    );

    public static final Field field$ClientboundSetPlayerTeamPacket$parameters = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPlayerTeamPacket, Optional.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundSetPlayerTeamPacket$Parameters = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutScoreboardTeam$b",
                    "network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters"
            )
    );

    public static final Class<?> clazz$Team$Visibility = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.scores.ScoreboardTeamBase$EnumTeamPush",
                    "world.scores.Team$Visibility"
            )
    );

    public static final Field field$ClientboundSetPlayerTeamPacket$Parameters$nametagVisibility = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPlayerTeamPacket$Parameters,
                    VersionHelper.isOrAbove1_21_5() ? clazz$Team$Visibility : String.class,
                    0
            )
    );

    public static final Class<?> clazz$ServerConnectionListener = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.network.ServerConnection",
                    "server.network.ServerConnectionListener"
            )
    );

    public static final Field field$MinecraftServer$connection = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MinecraftServer, clazz$ServerConnectionListener, 0)
    );

    public static final Field field$ServerConnectionListener$channels;

    static {
        Field[] fields = clazz$ServerConnectionListener.getDeclaredFields();
        Field f = null;
        for (Field field : fields) {
            if (List.class.isAssignableFrom(field.getType())) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType paramType) {
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] == ChannelFuture.class) {
                        f = ReflectionUtils.setAccessible(field);
                        break;
                    }
                }
            }
        }
        field$ServerConnectionListener$channels = requireNonNull(f);
    }

    public static final Field field$ServerConnectionListener$connections;

    static {
        Field[] fields = clazz$ServerConnectionListener.getDeclaredFields();
        Field f = null;
        for (Field field : fields) {
            if (List.class.isAssignableFrom(field.getType())) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType paramType) {
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] == clazz$Connection) {
                        f = ReflectionUtils.setAccessible(field);
                        break;
                    }
                }
            }
        }
        field$ServerConnectionListener$connections = requireNonNull(f);
    }

    public static final Class<?> clazz$ClientboundBlockUpdatePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBlockChange",
                    "network.protocol.game.ClientboundBlockUpdatePacket"
            )
    );

    public static final Class<?> clazz$ClientboundSectionBlocksUpdatePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutMultiBlockChange",
                    "network.protocol.game.ClientboundSectionBlocksUpdatePacket"
            )
    );

    public static final Class<?> clazz$BlockPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.BlockPosition",
                    "core.BlockPos"
            )
    );

    public static final Class<?> clazz$SectionPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.SectionPosition",
                    "core.SectionPos"
            )
    );

    public static final Class<?> clazz$Vec3i = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.BaseBlockPosition",
                    "core.Vec3i"
            )
    );

//    public static final Field field$Vec3i$x = requireNonNull(
//            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 0)
//    );
//
//    public static final Field field$Vec3i$y = requireNonNull(
//            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 1)
//    );
//
//    public static final Field field$Vec3i$z = requireNonNull(
//            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 2)
//    );

    public static final Class<?> clazz$BlockState = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.IBlockData",
                    "world.level.block.state.BlockState"
            )
    );

    public static final Field field$ClientboundSectionBlocksUpdatePacket$positions = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSectionBlocksUpdatePacket, short[].class, 0
            )
    );

    public static final Field field$ClientboundSectionBlocksUpdatePacket$states = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSectionBlocksUpdatePacket, clazz$BlockState.arrayType(), 0
            )
    );

    public static final Field field$ClientboundSectionBlocksUpdatePacket$sectionPos = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSectionBlocksUpdatePacket, clazz$SectionPos, 0
            )
    );

    public static final Field field$ClientboundBlockUpdatePacket$blockstate = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBlockUpdatePacket, clazz$BlockState, 0
            )
    );

    public static final Field field$ClientboundBlockUpdatePacket$blockPos = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundBlockUpdatePacket, clazz$BlockPos, 0
            )
    );

    public static final Class<?> clazz$Block = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.Block")
            )
    );

    public static final Class<?> clazz$IdMapper = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryBlockID",
                    "core.IdMapper"
            )
    );

    public static final Class<?> clazz$IdMap = requireNonNull(
            clazz$IdMapper.getInterfaces()[0]
    );

    public static final Method method$IdMap$byId = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$IdMap, Object.class, int.class
            )
    );

    public static final Method method$IdMap$size = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMap, int.class)
    );

    public static final Method method$IdMapper$size = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, int.class)
    );

    @Deprecated
    public static final Method method$IdMapper$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, int.class, Object.class)
    );

    @Deprecated
    public static final Method method$IdMapper$byId = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, Object.class, int.class)
    );

    public static final Field field$BLOCK_STATE_REGISTRY = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Block, clazz$IdMapper, 0)
    );

    public static final Method method$Registry$asHolderIdMap = requireNonNull(
            ReflectionUtils.getMethod(clazz$Registry, clazz$IdMap)
    );

    public static final Class<?> clazz$LevelAccessor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.GeneratorAccess",
                    "world.level.LevelAccessor"
            )
    );

    public static final Object instance$BLOCK_STATE_REGISTRY;

    static {
        try {
            instance$BLOCK_STATE_REGISTRY = field$BLOCK_STATE_REGISTRY.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$Direction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.EnumDirection",
                    "core.Direction"
            )
    );

    public static final Method method$Direction$ordinal = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Direction, new String[]{"ordinal"}
            )
    );

    public static final Method method$Direction$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Direction, clazz$Direction.arrayType()
            )
    );

    public static final Object instance$Direction$DOWN;
    public static final Object instance$Direction$UP;
    public static final Object instance$Direction$NORTH;
    public static final Object instance$Direction$SOUTH;
    public static final Object instance$Direction$WEST;
    public static final Object instance$Direction$EAST;
    public static final Object[] instance$Directions;

    static {
        try {
            instance$Directions = (Object[]) method$Direction$values.invoke(null);
            instance$Direction$DOWN = instance$Directions[0];
            instance$Direction$UP = instance$Directions[1];
            instance$Direction$NORTH = instance$Directions[2];
            instance$Direction$SOUTH = instance$Directions[3];
            instance$Direction$WEST = instance$Directions[4];
            instance$Direction$EAST = instance$Directions[5];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

//    public static Object getOppositeDirection(Object direction) {
//        return oppositeDirections.get(direction);
//    }

    public static final Class<?> clazz$CraftBlock = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.CraftBlock")
            )
    );

    public static final Class<?> clazz$CraftEventFactory = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("event.CraftEventFactory")
            )
    );

    public static final Class<?> clazz$CraftBlockStates = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.CraftBlockStates")
            )
    );

    public static final Class<?> clazz$CraftBlockState = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.CraftBlockState")
            )
    );

//    public static final Method method$CraftBlock$at = requireNonNull(
//            ReflectionUtils.getStaticMethod(
//                    clazz$CraftBlock, clazz$CraftBlock, clazz$LevelAccessor, clazz$BlockPos
//            )
//    );

    public static final Method method$CraftBlockStates$getBlockState = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockStates, clazz$CraftBlockState, clazz$LevelAccessor, clazz$BlockPos
            )
    );

    public static final Method method$CraftBlockState$getHandle = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftBlockState, clazz$BlockState
            )
    );

    public static final Class<?> clazz$CraftBlockData = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.data.CraftBlockData")
            )
    );

    public static final Field field$CraftBlockData$data = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftBlockData, clazz$BlockState, 0
            )
    );

    @Deprecated
    public static final Method method$CraftBlockData$createData = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockData, clazz$CraftBlockData, new String[]{"createData"}, clazz$BlockState
            )
    );

    @Deprecated
    public static final Method method$CraftBlockData$fromData = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockData, clazz$CraftBlockData, new String[]{"fromData"}, clazz$BlockState
            )
    );

//    public static final Constructor<?> constructor$BlockPos = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$BlockPos, int.class, int.class, int.class
//            )
//    );

    public static final Method method$Vec3i$relative = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Vec3i, clazz$Vec3i, clazz$Direction
            )
    );

    public static final Method method$BlockPos$relative = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockPos, clazz$BlockPos, clazz$Direction
            )
    );

    public static final Constructor<?> constructor$ClientboundBlockUpdatePacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundBlockUpdatePacket, clazz$BlockPos, clazz$BlockState
            )
    );

    public static final Class<?> clazz$FriendlyByteBuf = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.PacketDataSerializer",
                    "network.FriendlyByteBuf"
            )
    );

    public static final Class<?> clazz$RegistryFriendlyByteBuf =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.RegistryFriendlyByteBuf")
            );

    @Deprecated
    public static final Constructor<?> constructor$RegistryFriendlyByteBuf = Optional.ofNullable(clazz$RegistryFriendlyByteBuf)
            .map(it -> ReflectionUtils.getConstructor(it, 0))
            .orElse(null);

    public static final Constructor<?> constructor$FriendlyByteBuf = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$FriendlyByteBuf, ByteBuf.class
            )
    );

    public static final Method method$FriendlyByteBuf$writeByte = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, int.class
            )
    );

    public static final Method method$FriendlyByteBuf$writeLongArray = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, long[].class
            )
    );

    public static final Class<?> clazz$PalettedContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteBlock",
                    "world.level.chunk.PalettedContainer"
            )
    );

    public static final Class<?> clazz$PalettedContainer$Data = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteBlock$c",
                    "world.level.chunk.PalettedContainer$Data"
            )
    );

    public static final Class<?> clazz$BitStorage = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "util.DataBits",
                    "util.BitStorage"
            )
    );

    public static final Class<?> clazz$Palette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPalette",
                    "world.level.chunk.Palette"
            )
    );

    public static final Field field$PalettedContainer$data = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$PalettedContainer, clazz$PalettedContainer$Data, 0
            )
    );

    public static final VarHandle varHandle$PalettedContainer$data = requireNonNull(
            ReflectionUtils.findVarHandle(field$PalettedContainer$data)
    );

    public static final Field field$PalettedContainer$Data$storage = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$PalettedContainer$Data, clazz$BitStorage, 0
            )
    );

    public static final Field field$PalettedContainer$Data$palette = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$PalettedContainer$Data, clazz$Palette, 0
            )
    );

    public static final Method method$BitStorage$getBits = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BitStorage, int.class
            )
    );

    public static final Method method$BitStorage$getRaw = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BitStorage, long[].class
            )
    );

    public static final Method method$Palette$write = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Palette, void.class, clazz$FriendlyByteBuf
            )
    );

    public static final Class<?> clazz$ClientboundLevelChunkWithLightPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLevelChunkWithLightPacket")
            )
    );

    public static final Class<?> clazz$ClientboundLevelChunkPacketData = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLevelChunkPacketData")
            )
    );

    public static final Class<?> clazz$ClientboundPlayerInfoUpdatePacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundPlayerInfoUpdatePacket")
            )
    );

    public static final Field field$ClientboundPlayerInfoUpdatePacket$entries = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundPlayerInfoUpdatePacket, List.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundPlayerInfoUpdatePacket$Action = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.ClientboundPlayerInfoUpdatePacket$a",
                    "network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action"
            )
    );

    public static final Method method$ClientboundPlayerInfoUpdatePacket$Action$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ClientboundPlayerInfoUpdatePacket$Action, clazz$ClientboundPlayerInfoUpdatePacket$Action.arrayType()
            )
    );

    public static final Object instance$ClientboundPlayerInfoUpdatePacket$Action$UPDATE_DISPLAY_NAME;

    static {
        try {
            Object[] values = (Object[]) method$ClientboundPlayerInfoUpdatePacket$Action$values.invoke(null);
            instance$ClientboundPlayerInfoUpdatePacket$Action$UPDATE_DISPLAY_NAME = values[5];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static final Field field$ClientboundLevelChunkWithLightPacket$chunkData = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelChunkWithLightPacket, clazz$ClientboundLevelChunkPacketData, 0
            )
    );

    public static final Field field$ClientboundLevelChunkWithLightPacket$x = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelChunkWithLightPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundLevelChunkWithLightPacket$z = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelChunkWithLightPacket, int.class, 1
            )
    );

    public static final Field field$ClientboundLevelChunkPacketData$buffer = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelChunkPacketData, byte[].class, 0
            )
    );

    public static final Field field$BlockPhysicsEvent$changed = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    BlockPhysicsEvent.class, BlockData.class, 0
            )
    );

    public static final Class<?> clazz$CraftChunk = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("CraftChunk")
            )
    );

    @Deprecated
    public static final Field field$CraftChunk$worldServer = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftChunk, clazz$ServerLevel, 0
            )
    );

    public static final Class<?> clazz$ChunkAccess = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.IChunkAccess",
                    "world.level.chunk.ChunkAccess"
            )
    );

    public static final Class<?> clazz$LevelChunk = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.Chunk",
                    "world.level.chunk.LevelChunk"
            )
    );

    public static final Class<?> clazz$LevelChunkSection = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.ChunkSection",
                    "world.level.chunk.LevelChunkSection"
            )
    );

    public static final Class<?> clazz$ServerChunkCache = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.ChunkProviderServer",
                    "server.level.ServerChunkCache"
            )
    );

    @Deprecated
//    public static final Field field$ServerLevel$chunkSource = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$ServerLevel, clazz$ServerChunkCache, 0
//            )
//    );

//    public static final Method method$ServerChunkCache$blockChanged = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$ServerChunkCache, void.class, clazz$BlockPos
//            )
//    );

//    public static final Method method$ServerChunkCache$getChunkAtIfLoadedMainThread = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$ServerChunkCache, clazz$LevelChunk, int.class, int.class
//            )
//    );

    public static final Field field$ChunkAccess$sections = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkAccess, clazz$LevelChunkSection.arrayType(), 0
            )
    );

    public static final Class<?> clazz$BlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.entity.TileEntity",
                    "world.level.block.entity.BlockEntity"
            )
    );

    public static final Class<?> clazz$AbstractFurnaceBlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.entity.TileEntityFurnace",
                    "world.level.block.entity.AbstractFurnaceBlockEntity"
            )
    );

    public static final Class<?> clazz$CampfireBlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.entity.TileEntityCampfire",
                    "world.level.block.entity.CampfireBlockEntity"
            )
    );

//    public static final Field field$ChunkAccess$blockEntities;
//
//    static {
//        Field targetField = null;
//        for (Field field : clazz$ChunkAccess.getDeclaredFields()) {
//            if (Map.class.isAssignableFrom(field.getType())) {
//                Type genericType = field.getGenericType();
//                if (genericType instanceof ParameterizedType parameterizedType) {
//                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//                    if (actualTypeArguments.length == 2 &&
//                            actualTypeArguments[0].equals(clazz$BlockPos) &&
//                            actualTypeArguments[1].equals(clazz$BlockEntity)) {
//                        field.setAccessible(true);
//                        targetField = field;
//                    }
//                }
//            }
//        }
//        field$ChunkAccess$blockEntities = targetField;
//    }

    public static final Method method$LevelChunkSection$setBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class, clazz$BlockState, boolean.class
            )
    );

    @Deprecated
    public static final Method method$LevelChunkSection$getBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class
            )
    );

    public static final Class<?> clazz$StatePredicate = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$f",
                    "world.level.block.state.BlockBehaviour$StatePredicate"
            )
    );

    public static final Class<?> clazz$BlockBehaviour$Properties = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$Info",
                    "world.level.block.state.BlockBehaviour$Properties"
            )
    );

    public static final Class<?> clazz$BlockBehaviour = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase",
                    "world.level.block.state.BlockBehaviour"
            )
    );

    public static final Method method$BlockBehaviour$Properties$of = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$BlockBehaviour$Properties, clazz$BlockBehaviour$Properties
            )
    );

    public static final Field field$BlockBehaviour$Properties$id = ReflectionUtils.getDeclaredField(
            clazz$BlockBehaviour$Properties, clazz$ResourceKey, 0
    );

    public static final Constructor<?> constructor$Block  = requireNonNull(
            ReflectionUtils.getConstructor(clazz$Block, clazz$BlockBehaviour$Properties)
    );

    public static final Class<?> clazz$MobEffect = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffectList"),  // 这里paper会自动获取到NM.world.effect.MobEffect
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffect") // 如果插件是mojmap就会走这个
            )
    );

    public static final Class<?> clazz$MobEffectInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.effect.MobEffect",
                    "world.effect.MobEffectInstance"
            )
    );

    public static final Class<?> clazz$ParticleType = requireNonNull(
            Optional.of(Objects.requireNonNull(
                    BukkitReflectionUtils.findReobfOrMojmapClass("core.particles.Particle", "core.particles.ParticleType")
            )).map(it -> {
                if (it.getSuperclass() != Object.class) {
                    return it.getSuperclass();
                }
                return it;
            }).orElseThrow()
    );

    public static final Class<?> clazz$SoundType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.SoundEffectType",
                    "world.level.block.SoundType"
            )
    );

    public static final Constructor<?> constructor$SoundType = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$SoundType, float.class, float.class, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent
            )
    );

    public static final Class<?> clazz$ItemLike = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IMaterial",
                    "world.level.ItemLike"
            )
    );

    public static final Class<?> clazz$Item = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.Item")
            )
    );

    public static final Class<?> clazz$FluidState = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.Fluid",
                    "world.level.material.FluidState"
            )
    );

    public static final Class<?> clazz$Fluid = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FluidType"),  // 这里paper会自动获取到NM.world.level.material.Fluid
                    BukkitReflectionUtils.assembleMCClass("world.level.material.Fluid") // 如果插件是mojmap就会走这个
            )
    );

    public static final Class<?> clazz$RecipeType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.Recipes",
                    "world.item.crafting.RecipeType"
            )
    );

    public static final Class<?> clazz$WorldGenLevel = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.GeneratorAccessSeed",
                    "world.level.WorldGenLevel"
            )
    );

    public static final Class<?> clazz$ChunkGenerator = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.ChunkGenerator")
            )
    );

    // 1.20.1-1.20.2
    public static final Class<?> clazz$AbstractTreeGrower =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.grower.WorldGenTreeProvider",
                    "world.level.block.grower.AbstractTreeGrower"
            );

    public static final Class<?> clazz$ConfiguredFeature = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.WorldGenFeatureConfigured",
                    "world.level.levelgen.feature.ConfiguredFeature"
            )
    );

    public static final Class<?> clazz$PlacedFeature = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.levelgen.placement.PlacedFeature")
            )
    );

    // 1.21+
    public static final Class<?> clazz$JukeboxSong =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.JukeboxSong")
            );

    public static final Object instance$BuiltInRegistries$BLOCK;
    public static final Object instance$BuiltInRegistries$ITEM;
    public static final Object instance$BuiltInRegistries$ATTRIBUTE;
    public static final Object instance$BuiltInRegistries$BIOME;
    public static final Object instance$BuiltInRegistries$MOB_EFFECT;
    public static final Object instance$BuiltInRegistries$SOUND_EVENT;
    public static final Object instance$BuiltInRegistries$PARTICLE_TYPE;
    public static final Object instance$BuiltInRegistries$ENTITY_TYPE;
    public static final Object instance$BuiltInRegistries$FLUID;
    public static final Object instance$BuiltInRegistries$RECIPE_TYPE;
    public static final Object instance$BuiltInRegistries$PLACED_FEATURE;
    public static final Object instance$InternalRegistries$DIMENSION_TYPE;
    @Nullable // 1.21+
    public static final Object instance$InternalRegistries$JUKEBOX_SONG;

    public static final Object instance$Registries$BLOCK;
    public static final Object instance$Registries$ITEM;
    public static final Object instance$Registries$ATTRIBUTE;
    public static final Object instance$Registries$BIOME;
    public static final Object instance$Registries$MOB_EFFECT;
    public static final Object instance$Registries$SOUND_EVENT;
    public static final Object instance$Registries$PARTICLE_TYPE;
    public static final Object instance$Registries$ENTITY_TYPE;
    public static final Object instance$Registries$FLUID;
    public static final Object instance$Registries$RECIPE_TYPE;
    public static final Object instance$Registries$DIMENSION_TYPE;
    public static final Object instance$Registries$CONFIGURED_FEATURE;
    public static final Object instance$Registries$PLACED_FEATURE;
    @Nullable // 1.21+
    public static final Object instance$Registries$JUKEBOX_SONG;

    public static final Object instance$registryAccess;

    static {
        Field[] fields = clazz$Registries.getDeclaredFields();
        try {
            Object registries$Block = null;
            Object registries$Attribute  = null;
            Object registries$Biome  = null;
            Object registries$MobEffect  = null;
            Object registries$SoundEvent  = null;
            Object registries$DimensionType  = null;
            Object registries$ParticleType  = null;
            Object registries$EntityType  = null;
            Object registries$Item  = null;
            Object registries$Fluid  = null;
            Object registries$RecipeType  = null;
            Object registries$ConfiguredFeature  = null;
            Object registries$PlacedFeature  = null;
            Object registries$JukeboxSong  = null;
            for (Field field : fields) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof ParameterizedType paramType) {
                    if (paramType.getRawType() == clazz$ResourceKey) {
                        Type[] actualTypeArguments = paramType.getActualTypeArguments();
                        if (actualTypeArguments.length == 1 && actualTypeArguments[0] instanceof ParameterizedType registryType) {
                            Type type = registryType.getActualTypeArguments()[0];
                            if (type instanceof  ParameterizedType parameterizedType) {
                                Type rawType = parameterizedType.getRawType();
                                if (rawType == clazz$ParticleType) {
                                    registries$ParticleType = field.get(null);
                                } else if (rawType == clazz$EntityType) {
                                    registries$EntityType = field.get(null);
                                } else if (rawType == clazz$RecipeType) {
                                    registries$RecipeType = field.get(null);
                                } else if (rawType == clazz$ConfiguredFeature) {
                                    registries$ConfiguredFeature = field.get(null);
                                }
                            } else {
                                if (type == clazz$Block) {
                                    registries$Block = field.get(null);
                                } else if (type == clazz$Attribute) {
                                    registries$Attribute = field.get(null);
                                } else if (type == clazz$Biome) {
                                    registries$Biome = field.get(null);
                                } else if (type == clazz$MobEffect) {
                                    registries$MobEffect = field.get(null);
                                } else if (type == clazz$SoundEvent) {
                                    registries$SoundEvent = field.get(null);
                                } else if (type == clazz$DimensionType) {
                                    registries$DimensionType = field.get(null);
                                } else if (type == clazz$Item) {
                                    registries$Item = field.get(null);
                                } else if (type == clazz$Fluid) {
                                    registries$Fluid = field.get(null);
                                } else if (VersionHelper.isOrAbove1_21() && type == clazz$JukeboxSong) {
                                    registries$JukeboxSong = field.get(null);
                                } else if (type == clazz$PlacedFeature) {
                                    registries$PlacedFeature = field.get(null);
                                }
                            }
                        }
                    }
                }
            }
            instance$Registries$BLOCK = requireNonNull(registries$Block);
            instance$Registries$ITEM = requireNonNull(registries$Item);
            instance$Registries$ATTRIBUTE = requireNonNull(registries$Attribute);
            instance$Registries$BIOME = requireNonNull(registries$Biome);
            instance$Registries$MOB_EFFECT = requireNonNull(registries$MobEffect);
            instance$Registries$SOUND_EVENT = requireNonNull(registries$SoundEvent);
            instance$Registries$DIMENSION_TYPE = requireNonNull(registries$DimensionType);
            instance$Registries$PARTICLE_TYPE = requireNonNull(registries$ParticleType);
            instance$Registries$ENTITY_TYPE = requireNonNull(registries$EntityType);
            instance$Registries$FLUID = requireNonNull(registries$Fluid);
            instance$Registries$RECIPE_TYPE = requireNonNull(registries$RecipeType);
            instance$Registries$CONFIGURED_FEATURE = requireNonNull(registries$ConfiguredFeature);
            instance$Registries$PLACED_FEATURE = requireNonNull(registries$PlacedFeature);
            instance$Registries$JUKEBOX_SONG = registries$JukeboxSong;
            Object server = method$MinecraftServer$getServer.invoke(null);
            Object registries = field$MinecraftServer$registries.get(server);
            instance$registryAccess = field$LayeredRegistryAccess$composite.get(registries);
            instance$BuiltInRegistries$BLOCK = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$Block);
            instance$BuiltInRegistries$ITEM = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$Item);
            instance$BuiltInRegistries$ATTRIBUTE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$Attribute);
            instance$BuiltInRegistries$BIOME = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$Biome);
            instance$BuiltInRegistries$MOB_EFFECT = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$MobEffect);
            instance$BuiltInRegistries$SOUND_EVENT = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$SoundEvent);
            instance$InternalRegistries$DIMENSION_TYPE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$DimensionType);
            instance$BuiltInRegistries$PARTICLE_TYPE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$ParticleType);
            instance$BuiltInRegistries$ENTITY_TYPE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$EntityType);
            instance$BuiltInRegistries$FLUID = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$Fluid);
            instance$BuiltInRegistries$RECIPE_TYPE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$RecipeType);
            instance$BuiltInRegistries$PLACED_FEATURE = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$PlacedFeature);
            if (registries$JukeboxSong == null) instance$InternalRegistries$JUKEBOX_SONG = null;
            else instance$InternalRegistries$JUKEBOX_SONG = method$RegistryAccess$registryOrThrow.invoke(instance$registryAccess, registries$JukeboxSong);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static final Method method$ResourceLocation$fromNamespaceAndPath = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ResourceLocation, clazz$ResourceLocation, String.class, String.class
            )
    );

    public static final Object instance$Block$BLOCK_STATE_REGISTRY;

    static {
        try {
            Field field = ReflectionUtils.getDeclaredField(clazz$Block, clazz$IdMapper, 0);
            assert field != null;
            instance$Block$BLOCK_STATE_REGISTRY = field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$IdMapper$add = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$IdMapper, void.class, Object.class
            )
    );

    public static final Class<?> clazz$StateDefinition = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockStateList",
                    "world.level.block.state.StateDefinition"
            )
    );

    public static final Field field$Block$StateDefinition = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Block, clazz$StateDefinition, 0
            )
    );

    public static final Field field$StateDefinition$states = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$StateDefinition, ImmutableList.class, 0
            )
    );

    public static final Class<?> clazz$MappedRegistry = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryMaterials",
                    "core.MappedRegistry"
            )
    );

    public static final Field field$MappedRegistry$frozen = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$MappedRegistry, boolean.class, 0
            )
    );

    public static final Method method$MappedRegistry$freeze = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$MappedRegistry, clazz$Registry
            )
    );

    public static final Field field$MappedRegistry$byValue = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$MappedRegistry, Map.class, 2
            )
    );

    public static final Field field$MappedRegistry$unregisteredIntrusiveHolders = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$MappedRegistry, Map.class, 5
            )
    );

    public static final Class<?> clazz$MapColor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.MaterialMapColor",
                    "world.level.material.MapColor"
            )
    );

    public static final Method method$MapColor$byId = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$MapColor, clazz$MapColor, int.class
            )
    );

    public static final Class<?> clazz$PushReaction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.EnumPistonReaction",
                    "world.level.material.PushReaction"
            )
    );

    public static final Method method$PushReaction$values = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$PushReaction, new String[] { "values" }
            )
    );

    public static final Class<?> clazz$NoteBlockInstrument = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockPropertyInstrument",
                    "world.level.block.state.properties.NoteBlockInstrument"
            )
    );

    public static final Method method$NoteBlockInstrument$values = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$NoteBlockInstrument, new String[] { "values" }
            )
    );

    public static final Class<?> clazz$BlockStateBase = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$BlockData",
                    "world.level.block.state.BlockBehaviour$BlockStateBase"
            )
    );

    public static final Class<?> clazz$BlockStateBase$Cache = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$BlockData$Cache",
                    "world.level.block.state.BlockBehaviour$BlockStateBase$Cache"
            )
    );

    public static final Field field$BlockStateBase$cache = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$BlockStateBase$Cache, 0
            )
    );

    // 1.20-1.21.1
    public static final Field field$BlockStateBase$Cache$lightBlock =
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockStateBase$Cache, int.class, 0
            );

    public static final Method method$BlockStateBase$initCache = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, void.class, new String[] { "initCache", "a" }
            )
    );

    public static final Field field$BlockStateBase$isRedstoneConductor = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$StatePredicate, 0
            )
    );

    public static final Field field$BlockStateBase$isSuffocating = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$StatePredicate, 1
            )
    );

    public static final Field field$BlockStateBase$isViewBlocking = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$StatePredicate, 2
            )
    );

    public static final Field field$BlockStateBase$fluidState = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$FluidState, 0
            )
    );

    public static final Field field$BlockStateBase$pushReaction = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$PushReaction, 0
            )
    );

    public static final Field field$BlockStateBase$mapColor = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$MapColor, 0
            )
    );

    public static final Field field$BlockStateBase$instrument = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, clazz$NoteBlockInstrument, 0
            )
    );

    public static final Field field$BlockStateBase$hardness = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, float.class, 0
            )
    );

    public static final Field field$BlockStateBase$burnable = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, boolean.class, 2
            )
    );

    public static final Field field$BlockStateBase$isRandomlyTicking = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, boolean.class, 9
            )
    );

    public static final Field field$BlockStateBase$requiresCorrectToolForDrops = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, boolean.class, 5
            )
    );

    public static final Field field$BlockStateBase$canOcclude = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, boolean.class, 6
            )
    );

    public static final Field field$BlockStateBase$replaceable = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, boolean.class, 8
            )
    );

    public static final Field field$BlockStateBase$lightEmission = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockStateBase, int.class, 0
            )
    );

    // 1.21.2+
    public static final Field field$BlockStateBase$lightBlock =
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockStateBase, int.class, 1
            );

    public static final Class<?> clazz$AABB = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.AxisAlignedBB",
                    "world.phys.AABB"
            )
    );

    public static final Field field$AABB$minX = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 0
            )
    );

    public static final Field field$AABB$minY = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 1
            )
    );

    public static final Field field$AABB$minZ = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 2
            )
    );

    public static final Field field$AABB$maxX = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 3
            )
    );

    public static final Field field$AABB$maxY = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 4
            )
    );

    public static final Field field$AABB$maxZ = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$AABB, double.class, 5
            )
    );

    public static final Method method$Block$box = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Block, clazz$VoxelShape, double.class, double.class, double.class, double.class, double.class, double.class
            )
    );

    @Deprecated
    public static final Constructor<?> constructor$AABB = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$AABB, double.class, double.class, double.class, double.class, double.class, double.class
            )
    );

    public static final Class<?> clazz$BlockGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IBlockAccess",
                    "world.level.BlockGetter"
            )
    );

    public static final Class<?> clazz$StateHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.IBlockDataHolder",
                    "world.level.block.state.StateHolder"
            )
    );

    public static final Field field$StateHolder$owner = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$StateHolder, Object.class, 0
            )
    );

    public static final Class<?> clazz$CollisionContext = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.shapes.VoxelShapeCollision",
                    "world.phys.shapes.CollisionContext"
            )
    );

    public static final Method method$BlockBehaviour$getShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getShape", "a"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext
            )
    );

    public static final Method method$BlockBehaviour$tick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, void.class, new String[]{"tick", "a"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource
            )
    );

    public static final Method method$BlockBehaviour$randomTick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, void.class, new String[]{"randomTick", "b"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource
            )
    );

//    public static final Method method$BlockGetter$getBlockState = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$BlockGetter, clazz$BlockState, clazz$BlockPos
//            )
//    );

    public static final Method method$LevelAccessor$scheduleTick = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelAccessor, void.class, clazz$BlockPos, clazz$Block, int.class
            )
    );

    public static final Method method$CraftBlock$setTypeAndData = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlock, boolean.class, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState, clazz$BlockState, boolean.class
            )
    );

    public static final Class<?> clazz$MappedRegistry$TagSet =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "RegistryMaterials$a",
                    "core.MappedRegistry$TagSet"
            );

    public static final Field field$MappedRegistry$allTags = Optional.ofNullable(clazz$MappedRegistry$TagSet)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$MappedRegistry, it, 0))
            .orElse(null);

    public static final Method method$MappedRegistry$TagSet$unbound = Optional.ofNullable(clazz$MappedRegistry$TagSet)
            .map(it -> ReflectionUtils.getStaticMethod(clazz$MappedRegistry$TagSet, clazz$MappedRegistry$TagSet))
            .orElse(null);

    public static final Method method$TagSet$forEach = Optional.ofNullable(clazz$MappedRegistry$TagSet)
            .map(it -> ReflectionUtils.getDeclaredMethod(clazz$MappedRegistry$TagSet, void.class, BiConsumer.class))
            .orElse(null);

    public static final Method method$Holder$Reference$bingTags = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Holder$Reference, void.class, Collection.class
            )
    );

    public static final Class<?> clazz$ClientboundLevelParticlesPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutWorldParticles",
                    "network.protocol.game.ClientboundLevelParticlesPacket"
            )
    );

    public static final Constructor<?> constructor$ClientboundLevelParticlesPacket = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
                    ReflectionUtils.getDeclaredConstructor(clazz$ClientboundLevelParticlesPacket, clazz$RegistryFriendlyByteBuf) :
                    ReflectionUtils.getConstructor(clazz$ClientboundLevelParticlesPacket, clazz$FriendlyByteBuf)
    );

    public static final Class<?> clazz$ParticleOptions = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.particles.ParticleParam",
                    "core.particles.ParticleOptions"
            )
    );

    public static final Class<?> clazz$BlockParticleOption = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.particles.ParticleParamBlock",
                    "core.particles.BlockParticleOption"
            )
    );

    @Deprecated
    public static final Field field$ClientboundLevelParticlesPacket$particle = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelParticlesPacket, clazz$ParticleOptions, 0
            )
    );

    @Deprecated
    public static final Field field$BlockParticleOption$blockState = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockParticleOption, clazz$BlockState, 0
            )
    );

    public static final Class<?> clazz$CraftMagicNumbers = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("util.CraftMagicNumbers")
            )
    );

    public static final Field field$CraftMagicNumbers$BLOCK_MATERIAL = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftMagicNumbers, "BLOCK_MATERIAL"
            )
    );

    public static final Field field$BlockBehaviour$properties = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockBehaviour, clazz$BlockBehaviour$Properties, 0
            )
    );

    public static final Field field$BlockBehaviour$explosionResistance = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockBehaviour, float.class, 0
            )
    );

    public static final Field field$BlockBehaviour$soundType = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockBehaviour, clazz$SoundType, 0
            )
    );

    public static final Field field$SoundType$breakSound = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SoundType, clazz$SoundEvent, 0
            )
    );

    public static final Field field$SoundType$stepSound = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SoundType, clazz$SoundEvent, 1
            )
    );

    public static final Field field$SoundType$placeSound = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SoundType, clazz$SoundEvent, 2
            )
    );

    public static final Field field$SoundType$hitSound = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SoundType, clazz$SoundEvent, 3
            )
    );

    public static final Field field$SoundType$fallSound = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SoundType, clazz$SoundEvent, 4
            )
    );

    @Deprecated
    public static final Field field$SoundEvent$location = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$SoundEvent, clazz$ResourceLocation, 0
            )
    );

    public static final Field field$BlockBehaviour$Properties$hasCollision = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$BlockBehaviour$Properties, boolean.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundLightUpdatePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutLightUpdate",
                    "network.protocol.game.ClientboundLightUpdatePacket"
            )
    );

    public static final Class<?> clazz$ChunkPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.ChunkCoordIntPair",
                    "world.level.ChunkPos"
            )
    );

    @Deprecated
    public static final Constructor<?> constructor$ChunkPos = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ChunkPos, int.class, int.class
            )
    );

    public static final Class<?> clazz$LevelLightEngine = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.lighting.LevelLightEngine")
            )
    );

    @Deprecated
    public static final Constructor<?> constructor$ClientboundLightUpdatePacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundLightUpdatePacket, clazz$ChunkPos, clazz$LevelLightEngine, BitSet.class, BitSet.class
            )
    );

    public static final Class<?> clazz$ChunkHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerChunk",
                    "server.level.ChunkHolder"
            )
    );

    public static final Class<?> clazz$ChunkMap = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerChunkMap",
                    "server.level.ChunkMap"
            )
    );

    public static final Class<?> clazz$ChunkHolder$PlayerProvider = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerChunk$d",
                    "server.level.ChunkHolder$PlayerProvider"
            )
    );

    public static final Field field$ChunkHolder$playerProvider = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkHolder, clazz$ChunkHolder$PlayerProvider, 0
            )
    );

    public static final Method method$ChunkHolder$PlayerProvider$getPlayers = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ChunkHolder$PlayerProvider, List.class, clazz$ChunkPos, boolean.class
            )
    );

    // 1.20 ~ 1.21.4 moonrise
    @Deprecated
    public static final Method method$ChunkHolder$getPlayers =
            ReflectionUtils.getMethod(
                    clazz$ChunkHolder, List.class, boolean.class
            );

    public static final Field field$ChunkHolder$lightEngine = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkHolder, clazz$LevelLightEngine, 0
            )
    );

    public static final Field field$ChunkHolder$blockChangedLightSectionFilter = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkHolder, BitSet.class, 0
            )
    );

    public static final Field field$ChunkHolder$skyChangedLightSectionFilter = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkHolder, BitSet.class, 1
            )
    );

    public static final Method method$ChunkHolder$broadcast = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$ChunkHolder, void.class, List.class, clazz$Packet
            )
    );

    @Deprecated
    public static final Method method$ServerChunkCache$getVisibleChunkIfPresent = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerChunkCache, clazz$ChunkHolder, long.class
            )
    );

    public static final Class<?> clazz$LightLayer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.EnumSkyBlock",
                    "world.level.LightLayer"
            )
    );

    public static final Method method$LightLayer$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$LightLayer, clazz$LightLayer.arrayType()
            )
    );

    public static final Object instance$LightLayer$BLOCK;

    static {
        try {
            instance$LightLayer$BLOCK = ((Object[]) method$LightLayer$values.invoke(null))[1];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$ChunkHolder$sectionLightChanged = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
            ReflectionUtils.getMethod(clazz$ChunkHolder, boolean.class, clazz$LightLayer, int.class) :
                    ReflectionUtils.getMethod(clazz$ChunkHolder, void.class, clazz$LightLayer, int.class)
    );

    public static final Class<?> clazz$ServerboundPlayerActionPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInBlockDig",
                    "network.protocol.game.ServerboundPlayerActionPacket"
            )
    );

    public static final Class<?> clazz$ServerboundPlayerActionPacket$Action = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInBlockDig$EnumPlayerDigType",
                    "network.protocol.game.ServerboundPlayerActionPacket$Action"
            )
    );

    @Deprecated
    public static final Field field$ServerboundPlayerActionPacket$pos = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundPlayerActionPacket, clazz$BlockPos, 0
            )
    );

    @Deprecated
    public static final Field field$ServerboundPlayerActionPacket$action = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundPlayerActionPacket, clazz$ServerboundPlayerActionPacket$Action, 0
            )
    );

    public static final Method method$ServerboundPlayerActionPacket$Action$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ServerboundPlayerActionPacket$Action, clazz$ServerboundPlayerActionPacket$Action.arrayType()
            )
    );

    public static final Object instance$ServerboundPlayerActionPacket$Action$START_DESTROY_BLOCK;
    public static final Object instance$ServerboundPlayerActionPacket$Action$ABORT_DESTROY_BLOCK;
    public static final Object instance$ServerboundPlayerActionPacket$Action$STOP_DESTROY_BLOCK;

    static {
        try {
            Object[] values = (Object[]) method$ServerboundPlayerActionPacket$Action$values.invoke(null);
            instance$ServerboundPlayerActionPacket$Action$START_DESTROY_BLOCK = values[0];
            instance$ServerboundPlayerActionPacket$Action$ABORT_DESTROY_BLOCK = values[1];
            instance$ServerboundPlayerActionPacket$Action$STOP_DESTROY_BLOCK = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Object instance$Holder$Attribute$block_break_speed;
    public static final Object instance$Holder$Attribute$block_interaction_range;
    public static final Object instance$Holder$Attribute$scale;

    static {
        try {
            if (VersionHelper.isOrAbove1_20_5()) {
                Object block_break_speed = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", VersionHelper.isOrAbove1_21_2() ? "block_break_speed" : "player.block_break_speed");
                @SuppressWarnings("unchecked")
                Optional<Object> breakSpeedHolder = (Optional<Object>) method$Registry$getHolder0.invoke(instance$BuiltInRegistries$ATTRIBUTE, block_break_speed);
                instance$Holder$Attribute$block_break_speed = breakSpeedHolder.orElse(null);

                Object block_interaction_range = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", VersionHelper.isOrAbove1_21_2() ? "block_interaction_range" : "player.block_interaction_range");
                @SuppressWarnings("unchecked")
                Optional<Object> blockInteractionRangeHolder = (Optional<Object>) method$Registry$getHolder0.invoke(instance$BuiltInRegistries$ATTRIBUTE, block_interaction_range);
                instance$Holder$Attribute$block_interaction_range = blockInteractionRangeHolder.orElse(null);

                Object scale = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", VersionHelper.isOrAbove1_21_2() ? "scale" : "generic.scale");
                @SuppressWarnings("unchecked")
                Optional<Object> scaleHolder = (Optional<Object>) method$Registry$getHolder0.invoke(instance$BuiltInRegistries$ATTRIBUTE, scale);
                instance$Holder$Attribute$scale = scaleHolder.orElse(null);
            } else {
                instance$Holder$Attribute$block_break_speed = null;
                instance$Holder$Attribute$block_interaction_range = null;
                instance$Holder$Attribute$scale = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$ServerPlayer$getAttribute = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$AttributeInstance, clazz$Holder) :
            ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$AttributeInstance, clazz$Attribute)
    );

    public static final Class<?> clazz$ServerPlayerGameMode = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerInteractManager",
                    "server.level.ServerPlayerGameMode"
            )
    );

    public static final Field field$ServerPlayer$gameMode = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayer, clazz$ServerPlayerGameMode, 0
            )
    );

    public static final Field field$ServerPlayerGameMode$destroyProgressStart = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayerGameMode, int.class, 0
            )
    );

    public static final Field field$ServerPlayerGameMode$gameTicks = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayerGameMode, int.class, 1
            )
    );

    public static final Field field$ServerPlayerGameMode$delayedTickStart = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayerGameMode, int.class, 2
            )
    );

    public static final Field field$ServerPlayerGameMode$isDestroyingBlock = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayerGameMode, boolean.class, 0
            )
    );

    public static final Field field$ServerPlayerGameMode$hasDelayedDestroy = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerPlayerGameMode, boolean.class, 1
            )
    );

    public static final Method method$ServerPlayerGameMode$destroyBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerPlayerGameMode, boolean.class, clazz$BlockPos
            )
    );

    public static final Class<?> clazz$Player = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.EntityHuman",
                    "world.entity.player.Player"
            )
    );

    public static final Class<?> clazz$Entity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.Entity")
            )
    );

    public static final Field field$Entity$ENTITY_COUNTER = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Entity, AtomicInteger.class, 0
            )
    );

    public static final AtomicInteger instance$Entity$ENTITY_COUNTER;

    static {
        try {
            instance$Entity$ENTITY_COUNTER = (AtomicInteger) requireNonNull(field$Entity$ENTITY_COUNTER.get(null));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$Level = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.World",
                    "world.level.Level"
            )
    );

    public static final Method method$Entity$level = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Entity, clazz$Level
            )
    );

//    public static final Method method$BlockStateBase$getDestroyProgress = requireNonNull(
//            ReflectionUtils.getDeclaredMethod(
//                    clazz$BlockStateBase, float.class, clazz$Player, clazz$BlockGetter, clazz$BlockPos
//            )
//    );

    public static final Class<?> clazz$ClientboundBlockDestructionPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutBlockBreakAnimation",
                    "network.protocol.game.ClientboundBlockDestructionPacket"
            )
    );

//    public static final Constructor<?> constructor$ClientboundBlockDestructionPacket = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$ClientboundBlockDestructionPacket, int.class, clazz$BlockPos, int.class
//            )
//    );

    public static final Class<?> clazz$ServerboundSwingPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInArmAnimation",
                    "network.protocol.game.ServerboundSwingPacket"
            )
    );

    public static final Class<?> clazz$InteractionHand = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.EnumHand",
                    "world.InteractionHand"
            )
    );

    @Deprecated
    public static final Field field$ServerboundSwingPacket$hand = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundSwingPacket, clazz$InteractionHand, 0
            )
    );

    public static final Method method$InteractionHand$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$InteractionHand, clazz$InteractionHand.arrayType()
            )
    );

    public static final Object instance$InteractionHand$MAIN_HAND;
    public static final Object instance$InteractionHand$OFF_HAND;

    static {
        try {
            Object[] values = (Object[]) method$InteractionHand$values.invoke(null);
            instance$InteractionHand$MAIN_HAND = values[0];
            instance$InteractionHand$OFF_HAND = values[1];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$EquipmentSlot = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EnumItemSlot",
                    "world.entity.EquipmentSlot"
            )
    );

    public static final Method method$EquipmentSlot$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$EquipmentSlot, clazz$EquipmentSlot.arrayType()
            )
    );

    public static final Object instance$EquipmentSlot$MAINHAND;
    public static final Object instance$EquipmentSlot$OFFHAND;
    public static final Object instance$EquipmentSlot$FEET;
    public static final Object instance$EquipmentSlot$LEGS;
    public static final Object instance$EquipmentSlot$CHEST;
    public static final Object instance$EquipmentSlot$HEAD;
//    public static final Object instance$EquipmentSlot$BODY;

    static {
        try {
            Object[] values = (Object[]) method$EquipmentSlot$values.invoke(null);
            instance$EquipmentSlot$MAINHAND = values[0];
            instance$EquipmentSlot$OFFHAND = values[1];
            instance$EquipmentSlot$FEET = values[2];
            instance$EquipmentSlot$LEGS = values[3];
            instance$EquipmentSlot$CHEST = values[4];
            instance$EquipmentSlot$HEAD = values[5];
//            instance$EquipmentSlot$BODY = values[6];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$ClientboundSetEquipmentPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityEquipment",
                    "network.protocol.game.ClientboundSetEquipmentPacket"
            )
    );

    public static final Constructor<?> constructor$ClientboundSetEquipmentPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundSetEquipmentPacket, int.class, List.class
            )
    );

    public static final Class<?> clazz$ClientboundEntityEventPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityStatus",
                    "network.protocol.game.ClientboundEntityEventPacket"
            )
    );

    public static final Field field$ClientboundEntityEventPacket$entityId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundEntityEventPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundEntityEventPacket$eventId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundEntityEventPacket, byte.class, 0
            )
    );

    public static final Constructor<?> constructor$ClientboundEntityEventPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundEntityEventPacket, clazz$Entity, byte.class
            )
    );

    public static final Method method$Block$defaultBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Block, clazz$BlockState
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseEntity",
                    "network.protocol.game.ServerboundInteractPacket"
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$Action = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction",
                    "network.protocol.game.ServerboundInteractPacket$Action"
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$InteractionAction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseEntity$d",
                    "network.protocol.game.ServerboundInteractPacket$InteractionAction"
            )
    );

    public static final Field field$ServerboundInteractPacket$InteractionAction$hand = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundInteractPacket$InteractionAction, clazz$InteractionHand, 0
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$InteractionAtLocationAction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseEntity$e",
                    "network.protocol.game.ServerboundInteractPacket$InteractionAtLocationAction"
            )
    );

    public static final Field field$ServerboundInteractPacket$InteractionAtLocationAction$hand = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundInteractPacket$InteractionAtLocationAction, clazz$InteractionHand, 0
            )
    );

    public static final Field field$ServerboundInteractPacket$InteractionAtLocationAction$location = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundInteractPacket$InteractionAtLocationAction, clazz$Vec3, 0
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$ActionType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseEntity$b",
                    "network.protocol.game.ServerboundInteractPacket$ActionType"
            )
    );

    public static final Method method$ServerboundInteractPacket$ActionType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ServerboundInteractPacket$ActionType, clazz$ServerboundInteractPacket$ActionType.arrayType()
            )
    );

    public static final Object instance$ServerboundInteractPacket$ActionType$INTERACT;
    public static final Object instance$ServerboundInteractPacket$ActionType$ATTACK;
    public static final Object instance$ServerboundInteractPacket$ActionType$INTERACT_AT;

    static {
        try {
            Object[] values = (Object[]) method$ServerboundInteractPacket$ActionType$values.invoke(null);
            instance$ServerboundInteractPacket$ActionType$INTERACT = values[0];
            instance$ServerboundInteractPacket$ActionType$ATTACK = values[1];
            instance$ServerboundInteractPacket$ActionType$INTERACT_AT = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

//    public static final Field field$ServerboundInteractPacket$entityId = requireNonNull(
//            ReflectionUtils.getInstanceDeclaredField(
//                    clazz$ServerboundInteractPacket, int.class, 0
//            )
//    );

    public static final Field field$ServerboundInteractPacket$usingSecondaryAction = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ServerboundInteractPacket, boolean.class, 0
            )
    );

    public static final Field field$ServerboundInteractPacket$action = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ServerboundInteractPacket, clazz$ServerboundInteractPacket$Action, 0
            )
    );

    public static final Method method$ServerboundInteractPacket$Action$getType = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerboundInteractPacket$Action, clazz$ServerboundInteractPacket$ActionType
            )
    );

    public static final Class<?> clazz$ClientboundUpdateMobEffectPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityEffect",
                    "network.protocol.game.ClientboundUpdateMobEffectPacket"
            )
    );

    public static final Class<?> clazz$ClientboundRemoveMobEffectPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutRemoveEntityEffect",
                    "network.protocol.game.ClientboundRemoveMobEffectPacket"
            )
    );

    public static final Object instance$MobEffecr$mining_fatigue;
    public static final Object instance$MobEffecr$haste;
    public static final Object instance$MobEffecr$invisibility;

    // for 1.20.1-1.20.4
    static {
        try {
            Object mining_fatigue = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "mining_fatigue");
            instance$MobEffecr$mining_fatigue = method$Registry$get.invoke(instance$BuiltInRegistries$MOB_EFFECT, mining_fatigue);
            Object haste = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "haste");
            instance$MobEffecr$haste = method$Registry$get.invoke(instance$BuiltInRegistries$MOB_EFFECT, haste);
            Object invisibility = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "invisibility");
            instance$MobEffecr$invisibility = method$Registry$get.invoke(instance$BuiltInRegistries$MOB_EFFECT, invisibility);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object allocateClientboundUpdateMobEffectPacketInstance() throws InstantiationException {
        return UNSAFE.allocateInstance(clazz$ClientboundUpdateMobEffectPacket);
    }

    public static final Constructor<?> constructor$ClientboundRemoveMobEffectPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundRemoveMobEffectPacket, 0
            )
    );

    public static final Constructor<?> constructor$ClientboundUpdateMobEffectPacket = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getConstructor(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, clazz$MobEffectInstance
            ) :
            ReflectionUtils.getConstructor(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, clazz$MobEffectInstance, boolean.class
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$entityId = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$effect = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, clazz$MobEffect, 0
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, clazz$Holder, 0
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$amplifier = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 0
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 1
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$duration = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 1
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 2
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$flags = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 1
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 0
            )
    );

    public static final Method method$ServerPlayer$getEffect = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, clazz$MobEffectInstance, clazz$MobEffect
            ) :
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, clazz$MobEffectInstance, clazz$Holder
            )
    );

    public static final Object instance$SoundEvent$EMPTY;
    public static final Object instance$SoundEvent$TRIDENT_RIPTIDE_1;
    public static final Object instance$SoundEvent$TRIDENT_RIPTIDE_2;
    public static final Object instance$SoundEvent$TRIDENT_RIPTIDE_3;
    public static final Object instance$SoundEvent$TRIDENT_THROW;

    static {
        try {
            Object intentionallyEmpty = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "intentionally_empty");
            instance$SoundEvent$EMPTY = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, intentionallyEmpty);
            Object tridentRiptide1 = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.trident_riptide_1");
            instance$SoundEvent$TRIDENT_RIPTIDE_1 = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, tridentRiptide1);
            Object tridentRiptide2 = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.trident_riptide_2");
            instance$SoundEvent$TRIDENT_RIPTIDE_2 = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, tridentRiptide2);
            Object tridentRiptide3 = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.trident.riptide_3");
            instance$SoundEvent$TRIDENT_RIPTIDE_3 = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, tridentRiptide3);
            Object tridentThrow = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item.trident.throw");
            instance$SoundEvent$TRIDENT_THROW = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, tridentThrow);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$Entity$getOnPos = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Entity, clazz$BlockPos, float.class
            )
    );

    // 1.21.4+
    public static final Class<?> clazz$ServerboundPickItemFromBlockPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundPickItemFromBlockPacket")
            );

    public static final Field field$ServerboundPickItemFromBlockPacket$pos = Optional.ofNullable(clazz$ServerboundPickItemFromBlockPacket)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$BlockPos, 0))
            .orElse(null);

    public static final Class<?> clazz$ServerboundSetCreativeModeSlotPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInSetCreativeSlot",
                    "network.protocol.game.ServerboundSetCreativeModeSlotPacket"
            )
    );

    public static final Field field$ServerboundSetCreativeModeSlotPacket$slotNum = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundSetCreativeModeSlotPacket, short.class, 0
            ) :
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundSetCreativeModeSlotPacket, int.class, 0
            )
    );

    public static final Class<?> clazz$ItemStack = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.ItemStack")
            )
    );

    public static final Object instance$ItemStack$EMPTY;

    static {
        try {
            instance$ItemStack$EMPTY = requireNonNull(
                    ReflectionUtils.getDeclaredField(
                            clazz$ItemStack, clazz$ItemStack, 0
                    )
            ).get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Field field$ServerboundSetCreativeModeSlotPacket$itemStack = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundSetCreativeModeSlotPacket, clazz$ItemStack, 0
            )
    );

    public static final Class<?> clazz$CraftItemStack = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftItemStack")
            )
    );

    @Deprecated
    public static final Method method$CraftItemStack$asCraftMirror = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftItemStack, clazz$CraftItemStack, new String[]{"asCraftMirror"}, clazz$ItemStack
            )
    );

//    public static final Method method$CraftItemStack$asNMSCopy = requireNonNull(
//            ReflectionUtils.getStaticMethod(
//                    clazz$CraftItemStack, clazz$ItemStack, new String[]{"asNMSCopy"}, ItemStack.class
//            )
//    );

    public static final Field field$Holder$Reference$tags = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Holder$Reference, Set.class, 0
            )
    );

    public static final Class<?> clazz$TagKey = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("tags.TagKey")
            )
    );

    public static final Field field$TagKey$location = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$TagKey, clazz$ResourceLocation, 0
            )
    );

    public static final Method method$TagKey$create = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$TagKey, clazz$TagKey, clazz$ResourceKey, clazz$ResourceLocation
            )
    );

    public static final Class<?> clazz$ItemEnchantments =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass( "world.item.enchantment.ItemEnchantments")
            );

    public static final Field field$ItemEnchantments$enchantments = Optional.ofNullable(clazz$ItemEnchantments)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, 0))
            .orElse(null);

    public static final Field field$Direction$data3d = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Direction, int.class, 0
            )
    );

    public static final Field field$Holder$Reference$value = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Holder$Reference, Object.class, 0
            )
    );

    // 1.21.3+
    public static final Class<?> clazz$ScheduledTickAccess =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.ScheduledTickAccess")
            );

    public static final Method method$RandomSource$nextFloat = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$RandomSource, float.class
            )
    );

    public static final Method method$BlockBehaviour$updateShape = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$LevelReader, clazz$ScheduledTickAccess, clazz$BlockPos, clazz$Direction, clazz$BlockPos, clazz$BlockState, clazz$RandomSource
            ) :
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Direction, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockPos
            )
    );

    public static final Class<?> clazz$Fallable = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.Fallable")
            )
    );

    public static final Class<?> clazz$FallingBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFalling",
                    "world.level.block.FallingBlock"
            )
    );

    public static final Method method$FallingBlock$isFree = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$FallingBlock, boolean.class, clazz$BlockState
            )
    );

    public static final Class<?> clazz$FallingBlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.item.EntityFallingBlock",
                    "world.entity.item.FallingBlockEntity"
            )
    );

    public static final Method method$FallingBlockEntity$fall = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$FallingBlockEntity, clazz$FallingBlockEntity, clazz$Level, clazz$BlockPos, clazz$BlockState
            )
    );

    public static final Method method$FallingBlockEntity$setHurtsEntities = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FallingBlockEntity, void.class, float.class, int.class
            )
    );

    public static final Field field$ServerLevel$uuid = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerLevel, UUID.class, 0
            )
    );

    public static final Field field$FallingBlockEntity$blockState = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$FallingBlockEntity, clazz$BlockState, 0
            )
    );

    public static final Field field$FallingBlockEntity$cancelDrop = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$FallingBlockEntity, boolean.class, 1
            )
    );

    @Deprecated
    public static final Method method$Level$getCraftWorld = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Level, clazz$CraftWorld
            )
    );

    public static final Field field$Entity$xo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Entity, double.class, 0
            )
    );

    public static final Field field$Entity$yo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Entity, double.class, 1
            )
    );

    public static final Field field$Entity$zo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Entity, double.class, 2
            )
    );

    public static final Object instance$Blocks$AIR;
    public static final Object instance$Blocks$AIR$defaultState;
    public static final Object instance$Blocks$STONE;
    public static final Object instance$Blocks$STONE$defaultState;
    public static final Object instance$Blocks$FIRE;
    public static final Object instance$Blocks$SOUL_FIRE;
    public static final Object instance$Blocks$ICE;
    public static final Object instance$Blocks$SHORT_GRASS;
    public static final Object instance$Blocks$SHORT_GRASS$defaultState;

    static {
        try {
            Object air = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "air");
            instance$Blocks$AIR = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, air);
            instance$Blocks$AIR$defaultState = method$Block$defaultBlockState.invoke(instance$Blocks$AIR);
            Object fire = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "fire");
            instance$Blocks$FIRE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, fire);
            Object soulFire = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "soul_fire");
            instance$Blocks$SOUL_FIRE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, soulFire);
            Object stone = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "stone");
            instance$Blocks$STONE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, stone);
            instance$Blocks$STONE$defaultState = method$Block$defaultBlockState.invoke(instance$Blocks$STONE);
            Object ice = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "ice");
            instance$Blocks$ICE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, ice);
            Object shortGrass = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", VersionHelper.isOrAbove1_20_3() ? "short_grass" : "grass");
            instance$Blocks$SHORT_GRASS = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, shortGrass);
            instance$Blocks$SHORT_GRASS$defaultState = method$Block$defaultBlockState.invoke(instance$Blocks$SHORT_GRASS);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$BlockStateBase$hasTag = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, boolean.class, clazz$TagKey
            )
    );

    @Deprecated
    public static final Method method$Level$removeBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Level, boolean.class, clazz$BlockPos, boolean.class
            )
    );

    public static final Class<?> clazz$MutableBlockPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.BlockPosition$MutableBlockPosition",
                    "core.BlockPos$MutableBlockPos"
            )
    );

    public static final Constructor<?> constructor$MutableBlockPos = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$MutableBlockPos
            )
    );

    public static final Method method$MutableBlockPos$setWithOffset = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$MutableBlockPos, clazz$MutableBlockPos, clazz$Vec3i, clazz$Direction
            )
    );

    public static final Method method$BlockPos$mutable = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockPos, clazz$MutableBlockPos
            )
    );

    public static final Class<?> clazz$LeavesBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockLeaves",
                    "world.level.block.LeavesBlock"
            )
    );

    public static final Class<?> clazz$IntegerProperty = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockStateInteger",
                    "world.level.block.state.properties.IntegerProperty"
            )
    );

    public static final Class<?> clazz$Property = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.IBlockState",
                    "world.level.block.state.properties.Property"
            )
    );

    public static final Field field$LeavesBlock$DISTANCE = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$LeavesBlock, clazz$IntegerProperty, 0
            )
    );

    public static final Method method$StateHolder$hasProperty = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$StateHolder, boolean.class, clazz$Property
            )
    );

    public static final Method method$StateHolder$getValue = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$StateHolder, Object.class, new String[] {"getValue", "c"}, clazz$Property
            )
    );

    public static final Method method$Block$updateFromNeighbourShapes = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Block, clazz$BlockState, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos
            )
    );

    public static final Method method$BlockStateBase$updateNeighbourShapes = requireNonNull(
            ReflectionUtils.getMethod(
                                                                                           // flags   // depth
                    clazz$BlockStateBase, void.class, clazz$LevelAccessor, clazz$BlockPos, int.class, int.class
            )
    );

    public static final Method method$messageToByteEncoder$encode = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    MessageToByteEncoder.class, void.class, ChannelHandlerContext.class, Object.class, ByteBuf.class
            )
    );

    public static final Method method$byteToMessageDecoder$decode = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    ByteToMessageDecoder.class, void.class, ChannelHandlerContext.class, ByteBuf.class, List.class
            )
    );

    public static final Class<?> clazz$ClientboundRespawnPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutRespawn",
                    "network.protocol.game.ClientboundRespawnPacket"
            )
    );

    public static final Class<?> clazz$ClientboundLoginPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutLogin",
                    "network.protocol.game.ClientboundLoginPacket"
            )
    );

    // 1.20
    public static final Field field$ClientboundRespawnPacket$dimension =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundRespawnPacket, clazz$ResourceKey, 1
            );

    // 1.20
    public static final Field field$ClientboundLoginPacket$dimension =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLoginPacket, clazz$ResourceKey, 1
            );

    // 1.20.2+
    public static final Class<?> clazz$CommonPlayerSpawnInfo =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.CommonPlayerSpawnInfo")
            );

    // 1.20.2+
    public static final Field field$ClientboundRespawnPacket$commonPlayerSpawnInfo = Optional.ofNullable(clazz$CommonPlayerSpawnInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ClientboundRespawnPacket, it, 0))
            .orElse(null);

    // 1.20.2+
    public static final Field field$CommonPlayerSpawnInfo$dimension = Optional.ofNullable(clazz$CommonPlayerSpawnInfo)
            .map(it -> {
                if (VersionHelper.isOrAbove1_20_5()) {
                    return ReflectionUtils.getDeclaredField(it, clazz$ResourceKey, 0);
                } else {
                    return ReflectionUtils.getDeclaredField(it, clazz$ResourceKey, 1);
                }
            })
            .orElse(null);

    // 1.20.2+
    public static final Field field$ClientboundLoginPacket$commonPlayerSpawnInfo = Optional.ofNullable(clazz$CommonPlayerSpawnInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ClientboundLoginPacket, it, 0))
            .orElse(null);

    // 1.20-1.20.4
    public static final Method method$Packet$write =
            ReflectionUtils.getMethod(
                    clazz$Packet, void.class, clazz$FriendlyByteBuf
            );

    // 1.20.5+
    public static final Method method$ClientboundLevelParticlesPacket$write = Optional.ofNullable(clazz$RegistryFriendlyByteBuf)
            .map(it -> ReflectionUtils.getDeclaredMethod(clazz$ClientboundLevelParticlesPacket, void.class, it))
            .orElse(null);

    public static final Object instance$EntityType$TEXT_DISPLAY;
    public static final Object instance$EntityType$ITEM_DISPLAY;
    public static final Object instance$EntityType$BLOCK_DISPLAY;
    public static final Object instance$EntityType$ARMOR_STAND;
    public static final Object instance$EntityType$FALLING_BLOCK;
    public static final Object instance$EntityType$INTERACTION;
    public static final Object instance$EntityType$SHULKER;
    public static final Object instance$EntityType$OAK_BOAT;
    public static final Object instance$EntityType$TRIDENT;
    public static final Object instance$EntityType$SNOWBALL;

    static {
        try {
            Object textDisplay = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "text_display");
            instance$EntityType$TEXT_DISPLAY = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, textDisplay);
            Object itemDisplay = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "item_display");
            instance$EntityType$ITEM_DISPLAY = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, itemDisplay);
            Object blockDisplay = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "block_display");
            instance$EntityType$BLOCK_DISPLAY = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, blockDisplay);
            Object fallingBlock = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "falling_block");
            instance$EntityType$FALLING_BLOCK = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, fallingBlock);
            Object interaction = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "interaction");
            instance$EntityType$INTERACTION = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, interaction);
            Object shulker = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "shulker");
            instance$EntityType$SHULKER = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, shulker);
            Object armorStand = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "armor_stand");
            instance$EntityType$ARMOR_STAND = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, armorStand);
            Object oakBoat = VersionHelper.isOrAbove1_21_2() ? FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "oak_boat") : FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "boat");
            instance$EntityType$OAK_BOAT = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, oakBoat);
            Object trident = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "trident");
            instance$EntityType$TRIDENT = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, trident);
            Object snowball = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "snowball");
            instance$EntityType$SNOWBALL = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, snowball);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Object instance$RecipeType$CRAFTING;
    public static final Object instance$RecipeType$SMELTING;
    public static final Object instance$RecipeType$BLASTING;
    public static final Object instance$RecipeType$SMOKING;
    public static final Object instance$RecipeType$CAMPFIRE_COOKING;
    public static final Object instance$RecipeType$STONECUTTING;
    public static final Object instance$RecipeType$SMITHING;

    static {
        try {
            instance$RecipeType$CRAFTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "crafting"));
            instance$RecipeType$SMELTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "smelting"));
            instance$RecipeType$BLASTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "blasting"));
            instance$RecipeType$SMOKING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "smoking"));
            instance$RecipeType$CAMPFIRE_COOKING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "campfire_cooking"));
            instance$RecipeType$STONECUTTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "stonecutting"));
            instance$RecipeType$SMITHING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "smithing"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$BlockState$getShape = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, clazz$VoxelShape, new String[]{"getShape", "a"}, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext
            )
    );

    public static final Method method$VoxelShape$isEmpty = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$VoxelShape, boolean.class
            )
    );

    public static final Method method$VoxelShape$bounds = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$VoxelShape, clazz$AABB
            )
    );

    @Deprecated
    public static final Method method$LevelWriter$setBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelWriter, boolean.class, clazz$BlockPos, clazz$BlockState, int.class
            )
    );

    public static final Method method$CollisionContext$of = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CollisionContext, clazz$CollisionContext, clazz$Entity
            )
    );

    public static final Method method$CollisionContext$empty = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CollisionContext, clazz$CollisionContext
            )
    );

    public static final Method method$ServerLevel$checkEntityCollision = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerLevel, boolean.class, clazz$BlockState, clazz$Entity, clazz$CollisionContext, clazz$BlockPos, boolean.class
            )
    );

    public static final Method method$BlockStateBase$canSurvive = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, boolean.class, clazz$LevelReader, clazz$BlockPos
            )
    );

    @Deprecated
    public static final Method method$BlockStateBase$onPlace = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class
            )
    );

    public static final Method method$ItemStack$isTag = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ItemStack, boolean.class, clazz$TagKey
            )
    );

    public static final Class<?> clazz$FireBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFire",
                    "world.level.block.FireBlock"
            )
    );

    public static final Method method$FireBlock$setFlammable = requireNonNull(
            Optional.ofNullable(ReflectionUtils.getMethod(
                    clazz$FireBlock, void.class, clazz$Block, int.class, int.class
            )).orElse(ReflectionUtils.getDeclaredMethod(
                    clazz$FireBlock, void.class, clazz$Block, int.class, int.class)
            )
    );

    public static final Field field$LevelChunkSection$states = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$LevelChunkSection, clazz$PalettedContainer, 0
            )
    );

    public static final Constructor<?> constructor$ItemStack = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ItemStack, clazz$ItemLike
            )
    );

    public static final Object instance$Items$AIR;
    public static final Object instance$Items$WATER_BUCKET;
    public static final Object instance$ItemStack$Air;

    static {
        try {
            Object air = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "air");
            instance$Items$AIR = method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ITEM, air);
            instance$ItemStack$Air = constructor$ItemStack.newInstance(instance$Items$AIR);
            Object waterBucket = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "water_bucket");
            instance$Items$WATER_BUCKET = method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ITEM, waterBucket);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$Registry$SimpleRegistry = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.bukkit.Registry$SimpleRegistry"
            )
    );

    public static final Field field$Registry$SimpleRegistry$map = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Registry$SimpleRegistry, Map.class, 0
            )
    );

    public static final Class<?> clazz$Display$ItemDisplay = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.Display$ItemDisplay")
            )
    );

    // 1.21.2+
    public static final Class<?> clazz$ClientboundEntityPositionSyncPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundEntityPositionSyncPacket")
            );

    public static final Field field$ClientboundEntityPositionSyncPacket$id = Optional.ofNullable(clazz$ClientboundEntityPositionSyncPacket)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, int.class, 0))
            .orElse(null);

    public static final Class<?> clazz$ClientboundMoveEntityPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntity",
                    "network.protocol.game.ClientboundMoveEntityPacket"
            )
    );

    public static final Field field$ClientboundMoveEntityPacket$entityId = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundMoveEntityPacket, int.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundMoveEntityPacket$Pos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntity$PacketPlayOutRelEntityMove",
                    "network.protocol.game.ClientboundMoveEntityPacket$Pos"
            )
    );

    public static final Class<?> clazz$ClientboundMoveEntityPacket$Rot = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook",
                    "network.protocol.game.ClientboundMoveEntityPacket$Rot"
            )
    );

    public static final Constructor<?> constructor$ClientboundMoveEntityPacket$Rot = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundMoveEntityPacket$Rot, int.class, byte.class, byte.class, boolean.class)
    );

    public static final Class<?> clazz$ServerboundUseItemOnPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUseItem",
                    "network.protocol.game.ServerboundUseItemOnPacket"
            )
    );

//    @SuppressWarnings("deprecation")
//    public static final Method method$World$spawnEntity = requireNonNull(
//            VersionHelper.isVersionNewerThan1_20_2() ?
//                    ReflectionUtils.getMethod(World.class, Entity.class, Location.class, EntityType.class, CreatureSpawnEvent.SpawnReason.class, Consumer.class) :
//                    ReflectionUtils.getMethod(World.class, Entity.class, Location.class, EntityType.class, CreatureSpawnEvent.SpawnReason.class, org.bukkit.util.Consumer.class)
//    );

    // 1.21.4+
    public static final Class<?> clazz$ServerboundPickItemFromEntityPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundPickItemFromEntityPacket")
            );

    public static final Field field$ServerboundPickItemFromEntityPacket$id = Optional.ofNullable(clazz$ServerboundPickItemFromEntityPacket)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, int.class, 0))
            .orElse(null);

    public static final Class<?> clazz$ClientboundSoundPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutNamedSoundEffect",
                    "network.protocol.game.ClientboundSoundPacket"
            )
    );

    public static final Class<?> clazz$SoundSource = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "sounds.SoundCategory",
                    "sounds.SoundSource"
            )
    );

    public static final Constructor<?> constructor$ClientboundSoundPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundSoundPacket, clazz$Holder, clazz$SoundSource, double.class, double.class, double.class, float.class, float.class, long.class
            )
    );

    public static final Field field$ClientboundSoundPacket$sound = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, clazz$Holder, 0
            )
    );

    public static final Field field$ClientboundSoundPacket$source = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, clazz$SoundSource, 0
            )
    );

    public static final Field field$ClientboundSoundPacket$x = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundSoundPacket$y = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, int.class, 1
            )
    );

    public static final Field field$ClientboundSoundPacket$z = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, int.class, 2
            )
    );

    public static final Field field$ClientboundSoundPacket$volume = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, float.class, 0
            )
    );

    public static final Field field$ClientboundSoundPacket$pitch = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, float.class, 1
            )
    );

    public static final Field field$ClientboundSoundPacket$seed = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSoundPacket, long.class, 0
            )
    );

    public static final Method method$CraftEventFactory$callBlockPlaceEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, BlockPlaceEvent.class, clazz$ServerLevel, clazz$Player, clazz$InteractionHand, BlockState.class, clazz$BlockPos)
                    : ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, BlockPlaceEvent.class, clazz$ServerLevel, clazz$Player, clazz$InteractionHand, BlockState.class, int.class, int.class, int.class)
    );

    public static final Class<?> clazz$Abilities = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.PlayerAbilities",
                    "world.entity.player.Abilities"
            )
    );

    public static final Field field$Abilities$invulnerable = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Abilities, boolean.class, 0
            )
    );

    public static final Field field$Abilities$flying = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Abilities, boolean.class, 1
            )
    );

    public static final Field field$Abilities$mayfly = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Abilities, boolean.class, 2
            )
    );

    public static final Field field$Abilities$instabuild = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Abilities, boolean.class, 3
            )
    );

    public static final Field field$Abilities$mayBuild = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Abilities, boolean.class, 4
            )
    );

    public static final Field field$Player$abilities = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Player, clazz$Abilities, 0
            )
    );

    public static final Class<?> clazz$CraftEntity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("entity.CraftEntity")
            )
    );

    public static final Field field$CraftEntity$entity = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftEntity, clazz$Entity, 0
            )
    );

    public static final Object instance$Fluids$WATER;
    public static final Object instance$Fluids$FLOWING_WATER;
    public static final Object instance$Fluids$LAVA;
    public static final Object instance$Fluids$FLOWING_LAVA;
    public static final Object instance$Fluids$EMPTY;

    static {
        try {
            Object waterId = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "water");
            instance$Fluids$WATER = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, waterId);
            Object flowingWaterId = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "flowing_water");
            instance$Fluids$FLOWING_WATER = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, flowingWaterId);
            Object lavaId = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "lava");
            instance$Fluids$LAVA = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, lavaId);
            Object flowingLavaId = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "flowing_lava");
            instance$Fluids$FLOWING_LAVA = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, flowingLavaId);
            Object emptyId = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", "empty");
            instance$Fluids$EMPTY = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, emptyId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$FlowingFluid = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.FluidTypeFlowing",
                    "world.level.material.FlowingFluid"
            )
    );

    public static final Method method$FlowingFluid$getSource = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FlowingFluid, clazz$FluidState, boolean.class
            )
    );

    public static final Method method$Level$getFluidState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Level, clazz$FluidState, clazz$BlockPos
            )
    );

    public static final Method method$FluidState$isSource = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FluidState, boolean.class, new String[]{"isSource", "b"}
            )
    );

    public static final Method method$FluidState$createLegacyBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FluidState, clazz$BlockState
            )
    );

    public static final Class<?> clazz$FileToIdConverter = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("resources.FileToIdConverter")
            )
    );

    public static final Method method$FileToIdConverter$json = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$FileToIdConverter, clazz$FileToIdConverter, String.class
            )
    );

    public static final Class<?> clazz$ResourceManager = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.IResourceManager"),  // 这里paper会自动获取到NM.server.packs.resources.ResourceManager
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.ResourceManager") // 如果插件是mojmap就会走这个
            )
    );

    public static final Method method$FileToIdConverter$listMatchingResources = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FileToIdConverter, Map.class, new String[]{"listMatchingResources", "a"}, clazz$ResourceManager
            )
    );

    public static final Class<?> clazz$Resource = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.resources.IResource",
                    "server.packs.resources.Resource"
            )
    );

    public static final Method method$Resource$openAsReader = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Resource, BufferedReader.class
            )
    );

    public static final Class<?> clazz$MultiPackResourceManager = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.resources.ResourceManager",
                    "server.packs.resources.MultiPackResourceManager"
            )
    );

    public static final Class<?> clazz$PackType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.EnumResourcePackType",
                    "server.packs.PackType"
            )
    );

    public static final Method method$PackType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$PackType, clazz$PackType.arrayType()
            )
    );

    public static final Object instance$PackType$SERVER_DATA;

    static {
        try {
            Object[] values = (Object[]) method$PackType$values.invoke(null);
            instance$PackType$SERVER_DATA = values[1];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$PackRepository = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.repository.ResourcePackRepository",
                    "server.packs.repository.PackRepository"
            )
    );

    public static final Method method$MinecraftServer$getPackRepository = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$MinecraftServer, clazz$PackRepository
            )
    );

    public static final Field field$PackRepository$selected = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$PackRepository, List.class, 0
            )
    );

    public static final Class<?> clazz$Pack = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.repository.ResourcePackLoader",
                    "server.packs.repository.Pack"
            )
    );

    public static final Method method$PackRepository$getPack = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$PackRepository, clazz$Pack, String.class
            )
    );

    public static final Method method$Pack$getId = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Pack, String.class
            )
    );

    public static final Method method$MinecraftServer$reloadResources = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$MinecraftServer, CompletableFuture.class, Collection.class
            )
    );

    public static final Class<?> clazz$PackResources = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.IResourcePack",
                    "server.packs.PackResources"
            )
    );

    public static final Method method$Pack$open = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Pack, clazz$PackResources
            )
    );

    public static final Constructor<?> constructor$MultiPackResourceManager = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$MultiPackResourceManager, clazz$PackType, List.class
            )
    );

    public static final Class<?> clazz$InventoryView = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.bukkit.inventory.InventoryView"
            )
    );

    public static final Method method$InventoryView$getPlayer = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$InventoryView, HumanEntity.class
            )
    );

    public static final Class<?> clazz$RecipeManager = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingManager",
                    "world.item.crafting.RecipeManager"
            )
    );

    public static final Class<?> clazz$RecipeManager$CachedCheck = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingManager$a",
                    "world.item.crafting.RecipeManager$CachedCheck"
            )
    );

    public static final Method method$RecipeManager$finalizeRecipeLoading =
            ReflectionUtils.getMethod(
                    clazz$RecipeManager, new String[]{"finalizeRecipeLoading"}
            );

    public static final Method method$MinecraftServer$getRecipeManager = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$MinecraftServer, clazz$RecipeManager
            )
    );

    public static final Class<?> clazz$RecipeMap =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeMap")
            );

    public static final Field field$RecipeManager$recipes = Optional.ofNullable(clazz$RecipeMap)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$RecipeManager, it, 0))
            .orElse(null);

    public static final Method method$RecipeMap$removeRecipe = Optional.ofNullable(clazz$RecipeMap)
            .map(it -> ReflectionUtils.getMethod(it, boolean.class, clazz$ResourceKey))
            .orElse(null);

    public static final Class<?> clazz$CraftRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftRecipe")
            )
    );

    public static final Method method$CraftRecipe$addToCraftingManager = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftRecipe, new String[]{"addToCraftingManager"}
            )
    );

    public static final Method method$CraftRecipe$toMinecraft = Optional.of(clazz$CraftRecipe)
            .map(it -> ReflectionUtils.getStaticMethod(it, clazz$ResourceKey, NamespacedKey.class))
            .orElse(null);

    public static final Class<?> clazz$CraftShapedRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftShapedRecipe")
            )
    );

    public static final Method method$CraftShapedRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftShapedRecipe, clazz$CraftShapedRecipe, ShapedRecipe.class
            )
    );

    public static final Class<?> clazz$CraftShapelessRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftShapelessRecipe")
            )
    );

    public static final Method method$CraftShapelessRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftShapelessRecipe, clazz$CraftShapelessRecipe, ShapelessRecipe.class
            )
    );

    public static final Class<?> clazz$CraftSmithingTransformRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftSmithingTransformRecipe")
            )
    );

    public static final Method method$CraftSmithingTransformRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftSmithingTransformRecipe, clazz$CraftSmithingTransformRecipe, SmithingTransformRecipe.class
            )
    );

    public static final Class<?> clazz$FeatureFlagSet = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.flag.FeatureFlagSet")
            )
    );

    public static final Field field$RecipeManager$featureflagset =
            ReflectionUtils.getDeclaredField(
                    clazz$RecipeManager, clazz$FeatureFlagSet, 0
            );

    public static final Class<?> clazz$CraftInventoryPlayer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryPlayer")
            )
    );

    public static final Class<?> clazz$CraftInventory = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftInventory")
            )
    );

    public static final Class<?> clazz$Inventory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.PlayerInventory",
                    "world.entity.player.Inventory"
            )
    );

    public static final Method method$CraftInventoryPlayer$getInventory = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftInventoryPlayer, clazz$Inventory, new String[]{ "getInventory" }
            )
    );

    public static final Class<?> clazz$NonNullList = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.NonNullList")
            )
    );

    public static final Field field$Inventory$items = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Inventory, clazz$NonNullList, 0
            )
    );

    public static final Method method$NonNullList$set = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$NonNullList, Object.class, int.class, Object.class
            )
    );

    public static final Class<?> clazz$Ingredient = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeItemStack",
                    "world.item.crafting.Ingredient"
            )
    );

    // 1.20.1-1.21.1
    public static final Field field$Ingredient$itemStacks1_20_1 =
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient, clazz$ItemStack.arrayType(), 0
            );

    // 1.21.2-1.21.3
    public static final Field field$Ingredient$itemStacks1_21_2 =
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient, List.class, 1
            );

    // 1.21.4 paper
    public static final Field field$Ingredient$itemStacks1_21_4 =
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient, Set.class, 0
            );

    // Since 1.21.2, exact has been removed
    public static final Field field$Ingredient$exact =
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient, boolean.class, 0
            );

    public static final Class<?> clazz$ShapedRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.ShapedRecipes",
                    "world.item.crafting.ShapedRecipe"
            )
    );

    // 1.20.3+
    public static final Class<?> clazz$ShapedRecipePattern =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapedRecipePattern")
            );

    // 1.20.1-1.20.2
    public static final Field field$1_20_1$ShapedRecipe$recipeItems=
            ReflectionUtils.getDeclaredField(
                    clazz$ShapedRecipe, clazz$NonNullList, 0
            );

    // 1.20.3+
    public static final Field field$1_20_3$ShapedRecipe$pattern=
            ReflectionUtils.getDeclaredField(
                    clazz$ShapedRecipe, clazz$ShapedRecipePattern, 0
            );

    // 1.20.3-1.21.1
    public static final Field field$ShapedRecipePattern$ingredients1_20_3 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$NonNullList, 0))
            .orElse(null);

    // 1.21.2+
    public static final Field field$ShapedRecipePattern$ingredients1_21_2 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, List.class, 0))
            .orElse(null);

    // 1.20.1-1.21.1
    public static final Field field$Ingredient$values = ReflectionUtils.getInstanceDeclaredField(
            clazz$Ingredient, 0
    );

    // 1.20.2+
    public static final Class<?> clazz$RecipeHolder = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeHolder")
    );

    // 1.20.2-1.21.1 resource location
    // 1.21.2+ resource key
    public static final Constructor<?> constructor$RecipeHolder = Optional.ofNullable(clazz$RecipeHolder)
            .map(it -> ReflectionUtils.getConstructor(it, 0))
            .orElse(null);

    // 1.20.2+
    public static final Field field$RecipeHolder$recipe = Optional.ofNullable(clazz$RecipeHolder)
            .map(it -> ReflectionUtils.getDeclaredField(it, 1))
            .orElse(null);

    @Deprecated
    public static final Field field$RecipeHolder$id = Optional.ofNullable(clazz$RecipeHolder)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0))
            .orElse(null);

    public static final Class<?> clazz$ShapelessRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.ShapelessRecipes",
                    "world.item.crafting.ShapelessRecipe"
            )
    );

    public static final Class<?> clazz$PlacementInfo =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.PlacementInfo")
            );

    // 1.21.2+
    public static final Field field$ShapelessRecipe$placementInfo = Optional.ofNullable(clazz$PlacementInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, it, 0))
            .orElse(null);

    public static final Field field$ShapedRecipe$placementInfo = Optional.ofNullable(clazz$PlacementInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, it, 0))
            .orElse(null);

    public static final Field field$ShapelessRecipe$ingredients =
            Optional.ofNullable(ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, List.class, 0))
            .orElse(ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, clazz$NonNullList, 0));

    // require ResourceLocation for 1.20.1-1.21.1
    // require ResourceKey for 1.21.2+
    public static final Method method$RecipeManager$byKey;

    static {
        Method method$RecipeManager$byKey0 = null;
        if (VersionHelper.isOrAbove1_21_2()) {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceKey) {
                    if (method.getReturnType() == Optional.class && method.getGenericReturnType() instanceof ParameterizedType type) {
                        Type[] actualTypeArguments = type.getActualTypeArguments();
                        if (actualTypeArguments.length == 1) {
                            method$RecipeManager$byKey0 = method;
                        }
                    }
                }
            }
        } else if (VersionHelper.isOrAbove1_20_2()) {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
                    if (method.getReturnType() == Optional.class && method.getGenericReturnType() instanceof ParameterizedType type) {
                        Type[] actualTypeArguments = type.getActualTypeArguments();
                        if (actualTypeArguments.length == 1) {
                            method$RecipeManager$byKey0 = method;
                        }
                    }
                }
            }
        } else {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
                    if (method.getReturnType() == Optional.class) {
                        method$RecipeManager$byKey0 = method;
                    }
                }
            }
        }
        method$RecipeManager$byKey = requireNonNull(method$RecipeManager$byKey0);
    }

    public static final Class<?> clazz$CraftServer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("CraftServer")
            )
    );

    public static final Class<?> clazz$DedicatedPlayerList = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedPlayerList")
            )
    );

    public static final Field field$CraftServer$playerList = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftServer, clazz$DedicatedPlayerList, 0
            )
    );

    public static final Method method$DedicatedPlayerList$reloadRecipes = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$DedicatedPlayerList, new String[] {"reloadRecipeData", "reloadRecipes"}
            )
    );

    public static final Class<?> clazz$ResultContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.InventoryCraftResult",
                    "world.inventory.ResultContainer"
            )
    );

    public static final Class<?> clazz$Container = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.IInventory",
                    "world.Container"
            )
    );

    public static final Class<?> clazz$Recipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.IRecipe",
                    "world.item.crafting.Recipe"
            )
    );

    public static final Field field$ResultContainer$recipeUsed = Optional.ofNullable(ReflectionUtils.getDeclaredField(clazz$ResultContainer, clazz$Recipe, 0))
            .orElse(ReflectionUtils.getDeclaredField(clazz$ResultContainer, clazz$RecipeHolder, 0));

    public static final Class<?> clazz$CraftInventoryCrafting = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryCrafting")
            )
    );

    public static final Field field$CraftInventoryCrafting$resultInventory = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftInventoryCrafting, clazz$Container, 0
            )
    );

    public static final Class<?> clazz$LivingEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityLiving",
                    "world.entity.LivingEntity"
            )
    );

    public static final Class<?> clazz$CraftResultInventory = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftResultInventory")
            )
    );

    public static final Field field$CraftResultInventory$resultInventory = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftResultInventory, clazz$Container, 0
            )
    );

    // 1.20.5+
    public static final Method method$ItemStack$hurtAndBreak =
            ReflectionUtils.getMethod(
                    clazz$ItemStack, void.class, int.class, clazz$LivingEntity, clazz$EquipmentSlot
            );

    // for 1.20.1-1.21.1
    public static final Class<?> clazz$AbstractCookingRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeCooking",
                    "world.item.crafting.AbstractCookingRecipe"
            )
    );

    // for 1.20.1-1.21.1
    public static final Field field$AbstractCookingRecipe$input =
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractCookingRecipe, clazz$Ingredient, 0
            );

    // for 1.21.2+
    public static final Class<?> clazz$SingleItemRecipe =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeSingleItem",
                    "world.item.crafting.SingleItemRecipe"
            );

    // for 1.21.2+
    public static final Field field$SingleItemRecipe$input =
            Optional.ofNullable(clazz$SingleItemRecipe)
                    .map(it -> ReflectionUtils.getDeclaredField(it, clazz$Ingredient, 0))
                    .orElse(null);

    public static final Class<?> clazz$CraftFurnaceRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftFurnaceRecipe")
            )
    );

    public static final Method method$CraftFurnaceRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftFurnaceRecipe, clazz$CraftFurnaceRecipe, FurnaceRecipe.class
            )
    );

    public static final Class<?> clazz$CraftBlastingRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftBlastingRecipe")
            )
    );

    public static final Method method$CraftBlastingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlastingRecipe, clazz$CraftBlastingRecipe, BlastingRecipe.class
            )
    );

    public static final Class<?> clazz$CraftSmokingRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftSmokingRecipe")
            )
    );

    public static final Method method$CraftSmokingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftSmokingRecipe, clazz$CraftSmokingRecipe, SmokingRecipe.class
            )
    );

    public static final Class<?> clazz$CraftCampfireRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftCampfireRecipe")
            )
    );

    public static final Method method$CraftCampfireRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftCampfireRecipe, clazz$CraftCampfireRecipe, CampfireRecipe.class
            )
    );

    public static final Class<?> clazz$CraftStonecuttingRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftStonecuttingRecipe")
            )
    );

    public static final Method method$CraftStonecuttingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftStonecuttingRecipe, clazz$CraftStonecuttingRecipe, StonecuttingRecipe.class
            )
    );

//    public static final Field field$AbstractFurnaceBlockEntity$recipeType = requireNonNull(
//            ReflectionUtils.getDeclaredField(
//                    clazz$AbstractFurnaceBlockEntity, clazz$RecipeType, 0
//            )
//    );

    public static final Field field$AbstractFurnaceBlockEntity$quickCheck = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractFurnaceBlockEntity, clazz$RecipeManager$CachedCheck, 0
            )
    );

    // 1.20.1-1.21.1
    public static final Field field$CampfireBlockEntity$quickCheck =
            ReflectionUtils.getDeclaredField(
                    clazz$CampfireBlockEntity, clazz$RecipeManager$CachedCheck, 0
            );

    // 1.21+
    public static final Class<?> clazz$RecipeInput = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeInput")
    );

    public static final Class<?> clazz$SingleRecipeInput = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("world.item.crafting.SingleRecipeInput")
    );

    public static final Constructor<?> constructor$SingleRecipeInput = Optional.ofNullable(clazz$SingleRecipeInput)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$ItemStack))
            .orElse(null);

//    // 1.20.1-1.20.6
//    public static final Method method$RecipeManager$getRecipeFor0 =
//            ReflectionUtils.getMethod(
//                    clazz$RecipeManager, Optional.class, clazz$RecipeType, clazz$Container, clazz$Level, clazz$ResourceLocation
//            );
//
//    // 1.21.1
//    public static final Method method$RecipeManager$getRecipeFor2 =
//            ReflectionUtils.getMethod(
//                    clazz$RecipeManager, Optional.class, clazz$RecipeType, clazz$RecipeInput, clazz$Level, clazz$ResourceLocation
//            );
//
//    // 1.21.2+
//    public static final Method method$RecipeManager$getRecipeFor1 =
//            ReflectionUtils.getMethod(
//                    clazz$RecipeManager, Optional.class, clazz$RecipeType, clazz$RecipeInput, clazz$Level, clazz$ResourceKey
//            );

    // 1.21+
    public static final Field field$SingleRecipeInput$item = Optional.ofNullable(clazz$SingleRecipeInput)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$ItemStack, 0))
            .orElse(null);

    public static final Field field$AbstractFurnaceBlockEntity$items = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractFurnaceBlockEntity, clazz$NonNullList, 0
            )
    );

    public static final Class<?> clazz$CraftBlockEntityState = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.CraftBlockEntityState")
            )
    );

    public static final Field field$CraftBlockEntityState$tileEntity = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftBlockEntityState, 0
            )
    );

    public static final Class<?> clazz$SimpleContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.InventorySubcontainer",
                    "world.SimpleContainer"
            )
    );

    public static final Field field$SimpleContainer$items = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SimpleContainer, clazz$NonNullList, 0
            )
    );

    public static final Method method$LevelReader$getMaxLocalRawBrightness = requireNonNull(
            ReflectionUtils.getMethod(
                    Reflections.clazz$LevelReader, int.class, Reflections.clazz$BlockPos
            )
    );

    public static final Method method$ConfiguredFeature$place = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ConfiguredFeature, boolean.class, clazz$WorldGenLevel, clazz$ChunkGenerator, clazz$RandomSource, clazz$BlockPos
            )
    );

    public static final Method method$ServerChunkCache$getGenerator = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerChunkCache, clazz$ChunkGenerator
            )
    );

    public static final Method method$ServerLevel$sendBlockUpdated = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerLevel, void.class, clazz$BlockPos, clazz$BlockState, clazz$BlockState, int.class
            )
    );

    public static final Class<?> clazz$BonemealableBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.IBlockFragilePlantElement",
                    "world.level.block.BonemealableBlock"
            )
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getInstanceMethod(
                            clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState
                    ) :
                    ReflectionUtils.getInstanceMethod(
                            clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState, boolean.class
                    )
    );

    public static final Method method$BonemealableBlock$isBonemealSuccess = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BonemealableBlock, boolean.class, clazz$Level, clazz$RandomSource, clazz$BlockPos, clazz$BlockState
            )
    );

    public static final Method method$BonemealableBlock$performBonemeal = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BonemealableBlock, void.class, clazz$ServerLevel, clazz$RandomSource, clazz$BlockPos, clazz$BlockState
            )
    );

    public static final Class<?> clazz$ClientboundLevelEventPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutWorldEvent",
                    "network.protocol.game.ClientboundLevelEventPacket"
            )
    );

//    public static final Constructor<?> constructor$ClientboundLevelEventPacket = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$ClientboundLevelEventPacket, int.class, clazz$BlockPos, int.class, boolean.class
//            )
//    );

    public static final Field field$ClientboundLevelEventPacket$eventId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelEventPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundLevelEventPacket$data = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelEventPacket, int.class, 1
            )
    );

    public static final Field field$ClientboundLevelEventPacket$global = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelEventPacket, boolean.class, 0
            )
    );

    public static final Method method$ServerLevel$levelEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$ServerLevel, void.class, clazz$Entity, int.class, clazz$BlockPos, int.class)
                    : ReflectionUtils.getMethod(clazz$ServerLevel, void.class, clazz$Player, int.class, clazz$BlockPos, int.class)
    );

    public static final Method method$PalettedContainer$getAndSet = Objects.requireNonNull(
            ReflectionUtils.getMethod(
                    Reflections.clazz$PalettedContainer,
                    Object.class,
                    new String[] {"a", "getAndSet"},
                    int.class, int.class, int.class, Object.class
            )
    );

    public static final Method method$ServerGamePacketListenerImpl$tryPickItem =
            VersionHelper.isOrAbove1_21_5() ?
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerGamePacketListenerImpl, void.class, clazz$ItemStack, clazz$BlockPos, clazz$Entity, boolean.class
            ) :
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerGamePacketListenerImpl, void.class, clazz$ItemStack
            );

    public static final Class<?> clazz$ClientboundOpenScreenPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutOpenWindow",
                    "network.protocol.game.ClientboundOpenScreenPacket"
            )
    );

    public static final Class<?> clazz$MenuType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Containers",
                    "world.inventory.MenuType"
            )
    );

    public static final Constructor<?> constructor$ClientboundOpenScreenPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundOpenScreenPacket, int.class, clazz$MenuType, clazz$Component
            )
    );

    public static final Class<?> clazz$AbstractContainerMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Container",
                    "world.inventory.AbstractContainerMenu"
            )
    );

    public static final Field field$AbstractContainerMenu$title = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractContainerMenu, clazz$Component, 0
            )
    );

    public static final Field field$Player$containerMenu = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Player, clazz$AbstractContainerMenu, 0
            )
    );

    public static final Field field$AbstractContainerMenu$containerId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractContainerMenu, int.class, 1
            )
    );

    public static final Field field$AbstractContainerMenu$menuType = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractContainerMenu, clazz$MenuType, 0
            )
    );

    public static final Method method$CraftInventory$getInventory = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftInventory, clazz$Container, new String[]{ "getInventory" }
            )
    );

    public static final Method method$AbstractContainerMenu$broadcastChanges = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$AbstractContainerMenu, void.class, new String[]{ "broadcastChanges", "d" }
            )
    );

    public static final Method method$AbstractContainerMenu$broadcastFullState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$AbstractContainerMenu, void.class, new String[]{ "broadcastFullState", "e" }
            )
    );

    public static final Class<?> clazz$CraftContainer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftContainer")
            )
    );

    public static final Constructor<?> constructor$CraftContainer = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$CraftContainer, Inventory.class, clazz$Player, int.class
            )
    );

    public static final Field field$AbstractContainerMenu$checkReachable = requireNonNull(
            ReflectionUtils.getDeclaredFieldBackwards(
                    clazz$AbstractContainerMenu, boolean.class, 0
            )
    );

    public static final Method method$CraftContainer$getNotchInventoryType = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftContainer, clazz$MenuType, Inventory.class
            )
    );

    public static final Method method$ServerPlayer$nextContainerCounter = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, int.class, new String[] {"nextContainerCounter"}
            )
    );

    public static final Method method$ServerPlayer$initMenu = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, void.class, clazz$AbstractContainerMenu
            )
    );

    public static final Class<?> clazz$ClientboundResourcePackPushPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayOutResourcePackSend", "network.protocol.common.ClientboundResourcePackPacket", "network.protocol.common.ClientboundResourcePackPushPacket"),
                    List.of("network.protocol.common.ClientboundResourcePackPacket", "network.protocol.common.ClientboundResourcePackPushPacket")
            )
    );

    public static final Constructor<?> constructor$ClientboundResourcePackPushPacket = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getConstructor(
                    clazz$ClientboundResourcePackPushPacket, UUID.class, String.class, String.class, boolean.class, Optional.class
            ) :
            VersionHelper.isOrAbove1_20_3() ?
            ReflectionUtils.getConstructor(
                    clazz$ClientboundResourcePackPushPacket, UUID.class, String.class, String.class, boolean.class, clazz$Component
            ) :
            ReflectionUtils.getConstructor(
                    clazz$ClientboundResourcePackPushPacket, String.class, String.class, boolean.class, clazz$Component
            )
    );

    public static final Class<?> clazz$DedicatedServerProperties = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServerProperties")
            )
    );

    public static final Class<?> clazz$DedicatedServerSettings = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServerSettings")
            )
    );

    public static final Class<?> clazz$DedicatedServer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServer")
            )
    );

    public static final Field field$DedicatedServerSettings$properties = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$DedicatedServerSettings, clazz$DedicatedServerProperties, 0
            )
    );

    public static final Field field$DedicatedServer$settings = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$DedicatedServer, clazz$DedicatedServerSettings, 0
            )
    );

    public static final Class<?> clazz$MinecraftServer$ServerResourcePackInfo = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.MinecraftServer$ServerResourcePackInfo")
            )
    );

    public static final Field field$DedicatedServerProperties$serverResourcePackInfo = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$DedicatedServerProperties, Optional.class, 0
            )
    );

    public static final Constructor<?> constructor$ServerResourcePackInfo = requireNonNull(
            ReflectionUtils.getConstructor(clazz$MinecraftServer$ServerResourcePackInfo, 0)
    );

    public static final Class<?> clazz$ClientboundResourcePackPopPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.common.ClientboundResourcePackPopPacket")
            );

    public static final Constructor<?> constructor$ClientboundResourcePackPopPacket = Optional.ofNullable(clazz$ClientboundResourcePackPopPacket)
            .map(it -> ReflectionUtils.getConstructor(it, Optional.class))
            .orElse(null);

    public static final Constructor<?> constructor$JukeboxSong = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$Holder, clazz$Component, float.class, int.class))
            .orElse(null);

    public static final Field field$JukeboxSong$soundEvent = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$Holder, 0))
            .orElse(null);

    public static final Field field$JukeboxSong$description = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$Component, 0))
            .orElse(null);

    public static final Field field$JukeboxSong$lengthInSeconds = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getDeclaredField(it, float.class, 0))
            .orElse(null);

    public static final Field field$JukeboxSong$comparatorOutput = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getDeclaredField(it, int.class, 0))
            .orElse(null);

    public static final Method method$FluidState$getType = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FluidState, clazz$Fluid
            )
    );

    public static final Class<?> clazz$CraftComplexRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftComplexRecipe")
            )
    );

    public static final Class<?> clazz$CustomRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.IRecipeComplex",
                    "world.item.crafting.CustomRecipe"
            )
    );

    public static final Class<?> clazz$RepairItemRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeRepair",
                    "world.item.crafting.RepairItemRecipe"
            )
    );

    public static final Class<?> clazz$ArmorDyeRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeArmorDye",
                    "world.item.crafting.ArmorDyeRecipe"
            )
    );

    public static final Field field$CraftComplexRecipe$recipe = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftComplexRecipe, clazz$CustomRecipe, 0
            )
    );

    public static final Class<?> clazz$CraftInventoryAnvil = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryAnvil")
            )
    );

    public static final Class<?> clazz$AnvilMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.ContainerAnvil",
                    "world.inventory.AnvilMenu"
            )
    );

    // 1.21+
    public static final Class<?> clazz$CraftInventoryView =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryView")
            );

    // 1.21+
    public static final Field field$CraftInventoryView$container = Optional.ofNullable(clazz$CraftInventoryView)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0)).orElse(null);

    // 1.20-1.20.6
    public static final Field field$CraftInventoryAnvil$menu =
            ReflectionUtils.getDeclaredField(
                    clazz$CraftInventoryAnvil, clazz$AnvilMenu, 0
            );

    public static final Class<?> clazz$SmithingTransformRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.SmithingTransformRecipe")
            )
    );

    // 1.21.5+
    public static final Class<?> clazz$TransmuteResult =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.TransmuteResult")
            );

    public static final Constructor<?> constructor$TransmuteResult = Optional.ofNullable(clazz$TransmuteResult)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$Item))
            .orElse(null);

    public static final Constructor<?> constructor$SmithingTransformRecipe = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getConstructor(clazz$SmithingTransformRecipe, Optional.class, clazz$Ingredient, Optional.class, clazz$TransmuteResult)
                    : VersionHelper.isOrAbove1_21_2()
                        ? ReflectionUtils.getConstructor(clazz$SmithingTransformRecipe, Optional.class, Optional.class, Optional.class, clazz$ItemStack)
                        : VersionHelper.isOrAbove1_20_2()
                            ? ReflectionUtils.getConstructor(clazz$SmithingTransformRecipe, clazz$Ingredient, clazz$Ingredient, clazz$Ingredient, clazz$ItemStack)
                            : ReflectionUtils.getConstructor(clazz$SmithingTransformRecipe, clazz$ResourceLocation, clazz$Ingredient, clazz$Ingredient, clazz$Ingredient, clazz$ItemStack)
    );

    public static final Method method$RecipeManager$addRecipe = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getMethod(clazz$RecipeManager, void.class, clazz$RecipeHolder) :
                    ReflectionUtils.getMethod(clazz$RecipeManager, void.class, clazz$Recipe)
    );

    public static final Method method$CraftRecipe$toIngredient = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftRecipe, clazz$Ingredient, RecipeChoice.class, boolean.class
            )
    );

//    // 1.20.5+
//    public static final Method method$ItemStack$transmuteCopy = ReflectionUtils.getMethod(
//            clazz$ItemStack, clazz$ItemStack, clazz$ItemLike, int.class
//    );

    // 1.20.5+
    public static final Class<?> clazz$DataComponentPatch = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.DataComponentPatch")
    );

//    // 1.20.5+
//    public static final Method method$ItemStack$getComponentsPatch = Optional.ofNullable(clazz$DataComponentPatch)
//            .map(it -> ReflectionUtils.getMethod(clazz$ItemStack, it))
//            .orElse(null);
//
//    // 1.20.5+  WRONG!!!
//    public static final Method method$ItemStack$applyComponents = Optional.ofNullable(clazz$DataComponentPatch)
//            .map(it -> ReflectionUtils.getMethod(clazz$ItemStack, void.class, it))
//            .orElse(null);

    public static final Method method$ItemStack$getItem = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ItemStack, clazz$Item
            )
    );

    public static final Class<?> clazz$BlockHitResult = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.MovingObjectPositionBlock",
                    "world.phys.BlockHitResult"
            )
    );

    public static final Class<?> clazz$ClipContext$Fluid = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.RayTrace$FluidCollisionOption",
                    "world.level.ClipContext$Fluid"
            )
    );

    public static final Method method$ClipContext$Fluid$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ClipContext$Fluid, clazz$ClipContext$Fluid.arrayType()
            )
    );

    public static final Object instance$ClipContext$Fluid$NONE;
    public static final Object instance$ClipContext$Fluid$SOURCE_ONLY;
    public static final Object instance$ClipContext$Fluid$ANY;

    static {
        try {
            Object[] values = (Object[]) method$ClipContext$Fluid$values.invoke(null);
            instance$ClipContext$Fluid$NONE = values[0];
            instance$ClipContext$Fluid$SOURCE_ONLY = values[1];
            instance$ClipContext$Fluid$ANY = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$Item$getPlayerPOVHitResult = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Item, clazz$BlockHitResult, clazz$Level, clazz$Player, clazz$ClipContext$Fluid
            )
    );

    public static final Method method$BlockHitResult$withPosition = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockHitResult, clazz$BlockHitResult, clazz$BlockPos
            )
    );

    public static final Field field$BlockHitResul$blockPos = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockHitResult, clazz$BlockPos, 0
            )
    );

    public static final Field field$BlockHitResul$direction = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockHitResult, clazz$Direction, 0
            )
    );

    public static final Field field$BlockHitResul$miss = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockHitResult, boolean.class, 0
            )
    );

    public static final Field field$BlockHitResul$inside = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockHitResult, boolean.class, 1
            )
    );

    public static final Class<?> clazz$HitResult = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.MovingObjectPosition",
                    "world.phys.HitResult"
            )
    );

    public static final Field field$HitResult$location = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$HitResult, clazz$Vec3, 0
            )
    );

    public static final Class<?> clazz$MessageSignature = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.MessageSignature")
            )
    );

    public static final Class<?> clazz$LastSeenMessages$Update = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.LastSeenMessages$b",
                    "network.chat.LastSeenMessages$Update"
            )
    );

    public static final Class<?> clazz$ServerboundChatPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInChat",
                    "network.protocol.game.ServerboundChatPacket"
            )
    );

    public static final Constructor<?> constructor$ServerboundChatPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ServerboundChatPacket, String.class, Instant.class, long.class, clazz$MessageSignature, clazz$LastSeenMessages$Update
            )
    );

    public static final Field field$ServerboundChatPacket$message = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundChatPacket, String.class, 0
            )
    );

    public static final Field field$ServerboundChatPacket$timeStamp = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundChatPacket, Instant.class, 0
            )
    );

    public static final Field field$ServerboundChatPacket$salt = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundChatPacket, long.class, 0
            )
    );

    public static final Field field$ServerboundChatPacket$signature = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundChatPacket, clazz$MessageSignature, 0
            )
    );

    public static final Field field$ServerboundChatPacket$lastSeenMessages = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundChatPacket, clazz$LastSeenMessages$Update, 0
            )
    );

    public static final Class<?> clazz$ServerboundRenameItemPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInItemName",
                    "network.protocol.game.ServerboundRenameItemPacket"
            )
    );

    public static final Field field$ServerboundRenameItemPacket$name = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundRenameItemPacket, String.class, 0
            )
    );

    public static final Class<?> clazz$ServerboundSignUpdatePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInUpdateSign",
                    "network.protocol.game.ServerboundSignUpdatePacket"
            )
    );

    public static final Field field$ServerboundSignUpdatePacket$lines = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundSignUpdatePacket, String[].class, 0
            )
    );

    public static final Class<?> clazz$AdventureComponent = requireNonNull(
            ReflectionUtils.getClazz(
                    "net{}kyori{}adventure{}text{}Component".replace("{}", ".")
            )
    );

    @SuppressWarnings("UnstableApiUsage")
    public static final Field field$AsyncChatDecorateEvent$originalMessage = requireNonNull(
            ReflectionUtils.getDeclaredField(AsyncChatDecorateEvent.class, clazz$AdventureComponent, 0)
    );

    public static final Class<?> clazz$ComponentSerializer = requireNonNull(
            ReflectionUtils.getClazz(
                    "net{}kyori{}adventure{}text{}serializer{}ComponentSerializer".replace("{}", ".")
            )
    );

    public static final Class<?> clazz$GsonComponentSerializer = requireNonNull(
            ReflectionUtils.getClazz(
                    "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer".replace("{}", ".")
            )
    );

    public static final Class<?> clazz$GsonComponentSerializer$Builder = requireNonNull(
            ReflectionUtils.getClazz(
                    "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer$Builder".replace("{}", ".")
            )
    );

    public static final Method method$GsonComponentSerializer$builder = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$GsonComponentSerializer, clazz$GsonComponentSerializer$Builder
            )
    );

    public static final Method method$GsonComponentSerializer$Builder$build = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$GsonComponentSerializer$Builder, clazz$GsonComponentSerializer
            )
    );

    public static final Method method$ComponentSerializer$serialize = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ComponentSerializer, Object.class, new String[] {"serialize"}, clazz$AdventureComponent
            )
    );

    public static final Method method$ComponentSerializer$deserialize = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ComponentSerializer, Object.class, new String[] {"deserialize"}, Object.class
            )
    );

    @SuppressWarnings("UnstableApiUsage")
    public static final Method method$AsyncChatDecorateEvent$result = requireNonNull(
            ReflectionUtils.getMethod(
                    AsyncChatDecorateEvent.class, void.class, clazz$AdventureComponent
            )
    );

    public static final Class<?> clazz$ServerboundEditBookPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayInBEdit",
                    "network.protocol.game.ServerboundEditBookPacket"
            )
    );

    public static final Field field$ServerboundEditBookPacket$slot = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundEditBookPacket, int.class, 0
            )
    );

    public static final Field field$ServerboundEditBookPacket$pages = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundEditBookPacket, List.class, 0
            )
    );

    public static final Field field$ServerboundEditBookPacket$title = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundEditBookPacket, Optional.class, 0
            )
    );

    public static final Constructor<?> constructor$ServerboundEditBookPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ServerboundEditBookPacket, int.class, List.class, Optional.class
            )
    );

    public static final Class<?> clazz$SimpleWaterloggedBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.IBlockWaterlogged",
                    "world.level.block.SimpleWaterloggedBlock"
            )
    );

    public static final Method method$SimpleWaterloggedBlock$canPlaceLiquid = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$LivingEntity, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
                    : VersionHelper.isOrAbove1_20_2()
                        ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$Player, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
                        : ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
    );

    public static final Method method$SimpleWaterloggedBlock$placeLiquid = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$SimpleWaterloggedBlock, boolean.class, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState, clazz$FluidState
            )
    );

    public static final Method method$SimpleWaterloggedBlock$pickupBlock = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LivingEntity, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                    : VersionHelper.isOrAbove1_20_2()
                        ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$Player, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                        : ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
    );

    public static final Method method$Fluid$getTickDelay = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Fluid, int.class, clazz$LevelReader
            )
    );

    public static final Method method$Fluid$defaultFluidState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Fluid, clazz$FluidState, 0
            )
    );

    public static final Object instance$Fluid$EMPTY$defaultState;

    static {
        try {
            instance$Fluid$EMPTY$defaultState = method$Fluid$defaultFluidState.invoke(instance$Fluids$EMPTY);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$SingleValuePalette = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.SingleValuePalette")
            )
    );

    public static final Field field$SingleValuePalette$value = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SingleValuePalette, Object.class, 0
            )
    );

    public static final Class<?> clazz$HashMapPalette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteHash",
                    "world.level.chunk.HashMapPalette"
            )
    );

    public static final Class<?> clazz$CrudeIncrementalIntIdentityHashBiMap = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "util.RegistryID",
                    "util.CrudeIncrementalIntIdentityHashBiMap"
            )
    );

    public static final Field field$CrudeIncrementalIntIdentityHashBiMap$keys = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CrudeIncrementalIntIdentityHashBiMap, Object.class.arrayType(), 0
            )
    );

    public static final Field field$HashMapPalette$values = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$HashMapPalette, clazz$CrudeIncrementalIntIdentityHashBiMap, 0
            )
    );

    public static final Class<?> clazz$LinearPalette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteLinear",
                    "world.level.chunk.LinearPalette"
            )
    );

    public static final Field field$LinearPalette$values = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$LinearPalette, Object.class.arrayType(), 0
            )
    );

    public static final Object instance$Entity$DATA_SILENT;

    static {
        int i = 0;
        Field targetField = null;
        for (Field field : clazz$Entity.getDeclaredFields()) {
            Type fieldType = field.getGenericType();
            if (field.getType() == clazz$EntityDataAccessor && fieldType instanceof ParameterizedType paramType) {
                if (paramType.getActualTypeArguments()[0] == Boolean.class) {
                    i++;
                    if (i == 2) {
                        targetField = field;
                        break;
                    }
                }
            }
        }
        try {
            instance$Entity$DATA_SILENT = ReflectionUtils.setAccessible(requireNonNull(targetField)).get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Field field$Entity$entityData = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Entity, clazz$SynchedEntityData, 0
            )
    );

    public static final Class<?> clazz$SupportType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockSupport",
                    "world.level.block.SupportType"
            )
    );

    public static final Method method$SupportType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$SupportType, clazz$SupportType.arrayType()
            )
    );

    public static final Object instance$SupportType$FULL;
    public static final Object instance$SupportType$CENTER;
    public static final Object instance$SupportType$RIGID;

    static {
        try {
            Object[] values = (Object[]) method$SupportType$values.invoke(null);
            instance$SupportType$FULL = values[0];
            instance$SupportType$CENTER = values[1];
            instance$SupportType$RIGID = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$BlockStateBase$isFaceSturdy = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, boolean.class, clazz$BlockGetter, clazz$BlockPos, clazz$Direction, clazz$SupportType
            )
    );

    public static final Method method$CraftEventFactory$handleBlockFormEvent = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftEventFactory, boolean.class, new String[] { "handleBlockFormEvent" }, clazz$Level, clazz$BlockPos, clazz$BlockState, int.class
            )
    );

    public static final Constructor<?> constructor$ClientboundLevelChunkWithLightPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundLevelChunkWithLightPacket, clazz$LevelChunk, clazz$LevelLightEngine, BitSet.class, BitSet.class
            )
    );

    public static final Class<?> clazz$BlockInWorld = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.pattern.ShapeDetectorBlock",
                    "world.level.block.state.pattern.BlockInWorld"
            )
    );

    public static final Field field$BlockInWorld$state = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BlockInWorld, clazz$BlockState, 0
            )
    );

//    public static final Constructor<?> constructor$BlockInWorld = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$BlockInWorld, 0
//            )
//    );

//    // 1.20.5+
//    public static final Method method$ItemStack$canBreakBlockInAdventureMode =
//            ReflectionUtils.getMethod(
//                    clazz$ItemStack, new String[]{"canBreakBlockInAdventureMode"}, clazz$BlockInWorld
//            );

//    // 1.20 ~ 1.20.4
//    // instance$BuiltInRegistries$BLOCK
//    public static final Method method$ItemStack$canDestroy =
//            ReflectionUtils.getMethod(
//                    clazz$ItemStack,new String[]{"b"}, clazz$Registry, clazz$BlockInWorld
//            );

    public static final Method method$BlockStateBase$getBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, clazz$Block
            )
    );

    public static final Method method$BlockBehaviour$getDescriptionId = requireNonNull(
            VersionHelper.isOrAbove1_21_2()
                    ? ReflectionUtils.getMethod(clazz$BlockBehaviour, String.class)
                    : ReflectionUtils.getMethod(clazz$Block, String.class)
    );

    public static final Class<?> clazz$ServerboundCustomPayloadPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayInCustomPayload", "network.protocol.common.ServerboundCustomPayloadPacket"),
                    List.of("network.protocol.game.ServerboundCustomPayloadPacket", "network.protocol.common.ServerboundCustomPayloadPacket")
            )
    );

    // 1.20.2+
    public static final Class<?> clazz$CustomPacketPayload =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.common.custom.CustomPacketPayload")
            );

    // 1.20.5+
    public static final Class<?> clazz$CustomPacketPayload$Type =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.common.custom.CustomPacketPayload$b",
                    "network.protocol.common.custom.CustomPacketPayload$Type"
            );

    // 1.20.2+
    public static final Field field$ServerboundCustomPayloadPacket$payload = Optional.ofNullable(clazz$CustomPacketPayload)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ServerboundCustomPayloadPacket, it, 0))
            .orElse(null);

    // 1.20.2+
    public static final Class<?> clazz$DiscardedPayload =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.common.custom.DiscardedPayload")
            );

    // 1.20.5+
    public static final Method method$CustomPacketPayload$type = Optional.ofNullable(clazz$CustomPacketPayload$Type)
            .map(it -> ReflectionUtils.getMethod(clazz$CustomPacketPayload, it))
            .orElse(null);

    // 1.20.5+
    public static final Method method$CustomPacketPayload$Type$id = Optional.ofNullable(clazz$CustomPacketPayload$Type)
            .map(it -> ReflectionUtils.getMethod(it, clazz$ResourceLocation))
            .orElse(null);

    // 1.20.5~1.21.4#221
    public static final Method method$DiscardedPayload$data = Optional.ofNullable(clazz$DiscardedPayload)
            .map(it -> ReflectionUtils.getMethod(it, ByteBuf.class))
            .orElse(null);

    // 1.21.4#222+
    public static final Method method$DiscardedPayload$dataByteArray = Optional.ofNullable(method$DiscardedPayload$data)
            .map(m -> (Method) null)
            .orElseGet(() -> Optional.ofNullable(clazz$DiscardedPayload)
                    .map(clazz -> ReflectionUtils.getMethod(clazz, byte[].class))
                    .orElse(null)
            );

    public static final Class<?> clazz$ClientboundDisconnectPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayOutKickDisconnect", "network.protocol.common.ClientboundDisconnectPacket"),
                    List.of("network.protocol.game.ClientboundDisconnectPacket", "network.protocol.common.ClientboundDisconnectPacket")
            )
    );

    public static final Constructor<?> constructor$ClientboundDisconnectPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundDisconnectPacket, clazz$Component
            )
    );

    public static final Method method$CraftEventFactory$handleBlockGrowEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5() ?
            ReflectionUtils.getStaticMethod(
                    clazz$CraftEventFactory, boolean.class, clazz$Level, clazz$BlockPos, clazz$BlockState, int.class
            ) :
            ReflectionUtils.getStaticMethod(
                    clazz$CraftEventFactory, boolean.class, clazz$Level, clazz$BlockPos, clazz$BlockState
            )
    );

    public static final Class<?> clazz$BlockAndTintGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IBlockLightAccess",
                    "world.level.BlockAndTintGetter"
            )
    );

    public static final Method method$BlockAndTintGetter$getRawBrightness = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockAndTintGetter, int.class, clazz$BlockPos, int.class
            )
    );

    public static final Field field$Entity$boundingBox = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Entity, clazz$AABB, 0
            )
    );

    public static final Class<?> clazz$CraftShulker = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("entity.CraftShulker")
            )
    );

    public static final Class<?> clazz$Shulker = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.monster.EntityShulker",
                    "world.entity.monster.Shulker"
            )
    );

    public static final Method method$CraftShulker$getHandle = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftShulker, clazz$Shulker, 0
            )
    );

    public static final Class<?> clazz$Pose = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityPose",
                    "world.entity.Pose"
            )
    );

    public static final Method method$Pose$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Pose, clazz$Pose.arrayType()
            )
    );

    public static final Object instance$Pose$STANDING;
    public static final Object instance$Pose$FALL_FLYING;
    public static final Object instance$Pose$SLEEPING;
    public static final Object instance$Pose$SWIMMING;
    public static final Object instance$Pose$SPIN_ATTACK;
    public static final Object instance$Pose$CROUCHING;
    public static final Object instance$Pose$LONG_JUMPING;
    public static final Object instance$Pose$DYING;
    public static final Object instance$Pose$CROAKING;
    public static final Object instance$Pose$USING_TONGUE;
    public static final Object instance$Pose$SITTING;
    public static final Object instance$Pose$ROARING;
    public static final Object instance$Pose$SNIFFING;
    public static final Object instance$Pose$EMERGING;
    public static final Object instance$Pose$DIGGING;
    public static final Object instance$Pose$SLIDING;
    public static final Object instance$Pose$SHOOTING;
    public static final Object instance$Pose$INHALING;
    public static final Object[] instance$Poses;

    static {
        try {
            instance$Poses = (Object[]) method$Pose$values.invoke(null);
            instance$Pose$STANDING = instance$Poses[0];
            instance$Pose$FALL_FLYING = instance$Poses[1];
            instance$Pose$SLEEPING = instance$Poses[2];
            instance$Pose$SWIMMING = instance$Poses[3];
            instance$Pose$SPIN_ATTACK = instance$Poses[4];
            instance$Pose$CROUCHING = instance$Poses[5];
            instance$Pose$LONG_JUMPING = instance$Poses[6];
            instance$Pose$DYING = instance$Poses[7];
            instance$Pose$CROAKING = instance$Poses[8];
            instance$Pose$USING_TONGUE = instance$Poses[9];
            instance$Pose$SITTING = instance$Poses[10];
            instance$Pose$ROARING = instance$Poses[11];
            instance$Pose$SNIFFING = instance$Poses[12];
            instance$Pose$EMERGING = instance$Poses[13];
            instance$Pose$DIGGING = instance$Poses[14];
            if (VersionHelper.isOrAbove1_20_3()) {
                instance$Pose$SLIDING = instance$Poses[15];
                instance$Pose$SHOOTING = instance$Poses[16];
                instance$Pose$INHALING = instance$Poses[17];
            } else {
                instance$Pose$SLIDING = null;
                instance$Pose$SHOOTING = null;
                instance$Pose$INHALING = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$Attributes = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.GenericAttributes",
                    "world.entity.ai.attributes.Attributes"
            )
    );

    // 1.20.5+
    public static final Constructor<?> constructor$AttributeInstance =
            ReflectionUtils.getConstructor(
                    clazz$AttributeInstance, clazz$Holder, Consumer.class
            );

    public static final Method method$AttributeInstance$setBaseValue = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$AttributeInstance, void.class, double.class
            )
    );

//    public static final Constructor<?> constructor$ClientboundSetPassengersPacket = requireNonNull(
//            ReflectionUtils.getDeclaredConstructor(
//                    clazz$ClientboundSetPassengersPacket, clazz$FriendlyByteBuf
//            )
//    );

//    public static final Method method$FriendlyByteBuf$writeVarInt = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, new String[]{"writeVarInt", "d", "c"}, int.class
//            )
//    );
//
//    public static final Method method$FriendlyByteBuf$writeVarIntArray = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, int[].class
//            )
//    );

    public static final Method method$Entity$canBeCollidedWith = requireNonNull(
            VersionHelper.isOrAbove1_20_5()
                    ? ReflectionUtils.getMethod(clazz$Entity, boolean.class, new String[]{"canBeCollidedWith"})
                    : VersionHelper.isOrAbove1_20_3()
                        ? ReflectionUtils.getMethod(clazz$Entity, boolean.class, new String[]{"bz"})
                        : VersionHelper.isOrAbove1_20_2()
                            ? ReflectionUtils.getMethod(clazz$Entity, boolean.class, new String[]{"bx"})
                            : VersionHelper.isOrAbove1_20()
                                ? ReflectionUtils.getMethod(clazz$Entity, boolean.class, new String[]{"bu"})
                                : ReflectionUtils.getMethod(clazz$Entity, boolean.class, new String[]{"canBeCollidedWith", "bu", "bx", "bz"})
    );

    @Deprecated
    public static final Method method$CraftEntity$getHandle = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$CraftEntity, clazz$Entity, 0
            )
    );

    @Deprecated
    public static final Method method$Entity$getId = requireNonNull(
            VersionHelper.isOrAbove1_20_5()
                    ? ReflectionUtils.getMethod(clazz$Entity, int.class, new String[]{"getId"})
                    : VersionHelper.isOrAbove1_20_3()
                        ? ReflectionUtils.getMethod(clazz$Entity, int.class, new String[]{"aj"})
                        : VersionHelper.isOrAbove1_20_2()
                            ? ReflectionUtils.getMethod(clazz$Entity, int.class, new String[]{"ah"})
                            : VersionHelper.isOrAbove1_20()
                                ? ReflectionUtils.getMethod(clazz$Entity, int.class, new String[]{"af"})
                                : ReflectionUtils.getMethod(clazz$Entity, int.class, new String[]{"getId", "aj", "ah", "af"})
    );

    public static final Class<?> clazz$ClientboundMoveEntityPacket$PosRot = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook",
                    "network.protocol.game.ClientboundMoveEntityPacket$PosRot"
            )
    );

    public static final Constructor<?> constructor$ClientboundMoveEntityPacket$PosRot = requireNonNull(
            ReflectionUtils.getTheOnlyConstructor(clazz$ClientboundMoveEntityPacket$PosRot)
    );

    public static final Class<?> clazz$ClientboundRotateHeadPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityHeadRotation",
                    "network.protocol.game.ClientboundRotateHeadPacket"
            )
    );

    public static final Field field$ClientboundRotateHeadPacket$entityId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundRotateHeadPacket, int.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundSetEntityMotionPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutEntityVelocity",
                    "network.protocol.game.ClientboundSetEntityMotionPacket"
            )
    );

    public static final Field field$ClientboundSetEntityMotionPacket$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSetEntityMotionPacket, int.class, 0
            )
    );

    public static final Class<?> clazz$Rotation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockRotation",
                    "world.level.block.Rotation"
            )
    );

    public static final Method method$Rotation$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Rotation, clazz$Rotation.arrayType()
            )
    );

    public static final Object instance$Rotation$NONE;
    public static final Object instance$Rotation$CLOCKWISE_90;
    public static final Object instance$Rotation$CLOCKWISE_180;
    public static final Object instance$Rotation$COUNTERCLOCKWISE_90;

    static {
        try {
            Object[] values = (Object[]) method$Rotation$values.invoke(null);
            instance$Rotation$NONE = values[0];
            instance$Rotation$CLOCKWISE_90 = values[1];
            instance$Rotation$CLOCKWISE_180 = values[2];
            instance$Rotation$COUNTERCLOCKWISE_90 = values[3];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$Rotation$ordinal = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Rotation, int.class, new String[]{"ordinal"}
            )
    );

    public static final Class<?> clazz$Mirror = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockMirror",
                    "world.level.block.Mirror"
            )
    );

    public static final Method method$Mirror$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Mirror, clazz$Mirror.arrayType()
            )
    );

    public static final Object instance$Mirror$NONE;
    public static final Object instance$Mirror$LEFT_RIGHT;
    public static final Object instance$Mirror$FRONT_BACK;

    static {
        try {
            Object[] values = (Object[]) method$Mirror$values.invoke(null);
            instance$Mirror$NONE = values[0];
            instance$Mirror$LEFT_RIGHT = values[1];
            instance$Mirror$FRONT_BACK = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$Mirror$ordinal = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Mirror, int.class, new String[]{"ordinal"}
            )
    );

    public static final Method method$BlockBehaviour$rotate = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Rotation
            )
    );

    public static final Method method$BlockBehaviour$mirror = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Mirror
            )
    );

    public static final Method method$BlockStateBase$rotate = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, clazz$BlockState, clazz$Rotation
            )
    );

    public static final Method method$BlockStateBase$mirror = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, clazz$BlockState, clazz$Mirror
            )
    );

    public static final Constructor<?> constructor$ClientboundMoveEntityPacket$Pos = requireNonNull(
            ReflectionUtils.getDeclaredConstructor(
                    clazz$ClientboundMoveEntityPacket$Pos, int.class, short.class, short.class, short.class, boolean.class
            )
    );

    public static final Method method$Entity$getType = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Entity, clazz$EntityType
            )
    );

//    public static final Constructor<?> constructor$SynchedEntityData$DataValue = requireNonNull(
//            ReflectionUtils.getConstructor(
//                    clazz$SynchedEntityData$DataValue, int.class, clazz$EntityDataSerializer, Object.class
//            )
//    );

    public static final Class<?> clazz$EntityLookup = requireNonNull(
            ReflectionUtils.getClazz(
                    "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup",
                    "io.papermc.paper.chunk.system.entity.EntityLookup"
            )
    );

    public static final Method method$Level$moonrise$getEntityLookup = requireNonNull(
            ReflectionUtils.getMethod(
                    VersionHelper.isOrAbove1_21() ? clazz$Level : clazz$ServerLevel,
                    clazz$EntityLookup
            )
    );

    public static final Method method$EntityLookup$get = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$EntityLookup, clazz$Entity, int.class
            )
    );

    // 1.21+
    public static final Class<?> clazz$PacketReport =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("data.info.PacketReport")
            );

    // 1.21+
    public static final Constructor<?> constructor$PacketReport = Optional.ofNullable(clazz$PacketReport)
            .map(it -> ReflectionUtils.getConstructor(it, 0))
            .orElse(null);

    // 1.21+
    public static final Method method$PacketReport$serializePackets = Optional.ofNullable(clazz$PacketReport)
            .map(it -> ReflectionUtils.getDeclaredMethod(it, JsonElement.class))
            .orElse(null);

    public static final Object instance$GsonComponentSerializer;

    static {
        try {
            Object builder = Reflections.method$GsonComponentSerializer$builder.invoke(null);
            instance$GsonComponentSerializer = Reflections.method$GsonComponentSerializer$Builder$build.invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // 1.20.2+
    public static final Class<?> clazz$ServerboundClientInformationPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.common.ServerboundClientInformationPacket")
            );

    // 1.20.2+
    public static final Constructor<?> constructor$ServerboundClientInformationPacket = Optional.ofNullable(clazz$ServerboundClientInformationPacket)
            .map(it -> ReflectionUtils.getConstructor(it, 1))
            .orElse(null);

    // 1.20.2+
    public static final Field field$ServerboundClientInformationPacket$information = Optional.ofNullable(clazz$ServerboundClientInformationPacket)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0))
            .orElse(null);

    // 1.20.2+
    // 从 1.21.2+ 才有 particleStatus
    public static final Class<?> clazz$ClientInformation =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ClientInformation")
            );

    // 1.20.2+
    public static final Constructor<?> constructor$ClientInformation = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getConstructor(it, 1))
            .orElse(null);

    // 1.20.2+ String
    public static final Field field$ClientInformation$language = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0))
            .orElse(null);

    // 1.20.2+ int
    public static final Field field$ClientInformation$viewDistance = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 1))
            .orElse(null);

    // 1.20.2+ ChatVisiblity
    public static final Field field$ClientInformation$chatVisibility = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 2))
            .orElse(null);

    // 1.20.2+ boolean
    public static final Field field$ClientInformation$chatColors = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 3))
            .orElse(null);

    // 1.20.2+ int
    public static final Field field$ClientInformation$modelCustomisation = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 4))
            .orElse(null);

    // 1.20.2+ HumanoidArm
    public static final Field field$ClientInformation$mainHand = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 5))
            .orElse(null);

    // 1.20.2+ boolean
    public static final Field field$ClientInformation$textFilteringEnabled = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 6))
            .orElse(null);

    // 1.20.2+ boolean
    public static final Field field$ClientInformation$allowsListing = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 7))
            .orElse(null);

    // 1.21.2+
    public static final Class<?> clazz$ParticleStatus =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ParticleStatus")
            );

    // 1.21.2+
    public static final Method method$ParticleStatus$values = Optional.ofNullable(clazz$ParticleStatus)
            .map(it -> ReflectionUtils.getStaticMethod(it, it.arrayType()))
            .orElse(null);

    // 1.21.2+
    public static final Object instance$ParticleStatus$ALL;
    public static final Object instance$ParticleStatus$DECREASED;
    public static final Object instance$ParticleStatus$MINIMAL;

    // 1.21.2+
    static {
        try {
            if (VersionHelper.isOrAbove1_21_2()) {
                Object[] values = (Object[]) method$ParticleStatus$values.invoke(null);
                instance$ParticleStatus$ALL = values[0];
                instance$ParticleStatus$DECREASED = values[1];
                instance$ParticleStatus$MINIMAL = values[2];
            } else {
                instance$ParticleStatus$ALL = null;
                instance$ParticleStatus$DECREASED = null;
                instance$ParticleStatus$MINIMAL = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 1.21.2+ ParticleStatus
    public static final Field field$ClientInformation$particleStatus = Optional.ofNullable(clazz$ClientInformation)
            .map(it -> ReflectionUtils.getDeclaredField(it, 8))
            .orElse(null);

    public static final Method method$Registry$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$Registry, int.class, Object.class)
    );

    public static final int instance$EntityType$BLOCK_DISPLAY$registryId;
    public static final int instance$EntityType$ITEM_DISPLAY$registryId;
    public static final int instance$EntityType$TEXT_DISPLAY$registryId;
    public static final int instance$EntityType$FALLING_BLOCK$registryId;
    public static final int instance$EntityType$TRIDENT$registryId;
    public static final int instance$EntityType$ARMOR_STAND$registryId;

    static {
        try {
            instance$EntityType$BLOCK_DISPLAY$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$BLOCK_DISPLAY);
            instance$EntityType$ITEM_DISPLAY$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$ITEM_DISPLAY);
            instance$EntityType$TEXT_DISPLAY$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$TEXT_DISPLAY);
            instance$EntityType$FALLING_BLOCK$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$FALLING_BLOCK);
            instance$EntityType$TRIDENT$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$TRIDENT);
            instance$EntityType$ARMOR_STAND$registryId = (int) Reflections.method$Registry$getId.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, instance$EntityType$ARMOR_STAND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$ClientboundSetTitleTextPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetTitleTextPacket")
            )
    );

    public static final Class<?> clazz$ClientboundSetSubtitleTextPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetSubtitleTextPacket")
            )
    );

    public static final Class<?> clazz$ClientboundTabListPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutPlayerListHeaderFooter",
                    "network.protocol.game.ClientboundTabListPacket"
            )
    );

    public static final Class<?> clazz$ClientboundSetObjectivePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutScoreboardObjective",
                    "network.protocol.game.ClientboundSetObjectivePacket"
            )
    );

    public static final Class<?> clazz$SignChangeEvent = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.bukkit.event.block.SignChangeEvent"
            )
    );

    public static final Method method$SignChangeEvent$line = requireNonNull(
            ReflectionUtils.getMethod(clazz$SignChangeEvent, void.class, int.class, clazz$AdventureComponent)
    );

    public static final Class<?> clazz$BookMeta = requireNonNull(
            ReflectionUtils.getClazz(
                    "org.bukkit.inventory.meta.BookMeta"
            )
    );

    public static final Method method$BookMeta$page = requireNonNull(
            ReflectionUtils.getMethod(clazz$BookMeta, void.class, int.class, clazz$AdventureComponent)
    );

    public static final Method method$GsonComponentSerializer$serializer = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$GsonComponentSerializer, Gson.class
            )
    );

    public static final Gson instance$GsonComponentSerializer$Gson;

    static {
        try {
            instance$GsonComponentSerializer$Gson = (Gson) Reflections.method$GsonComponentSerializer$serializer.invoke(instance$GsonComponentSerializer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$ClientboundSetScorePacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.game.PacketPlayOutScoreboardScore",
                    "network.protocol.game.ClientboundSetScorePacket"
            )
    );

    public static final Method method$CraftPlayer$setSimplifyContainerDesyncCheck =
            ReflectionUtils.getMethod(
                    clazz$CraftPlayer, new String[]{"setSimplifyContainerDesyncCheck"}, boolean.class
            );

    public static final Class<?> clazz$ServerboundHelloPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.login.PacketLoginInStart",
                    "network.protocol.login.ServerboundHelloPacket"
            )
    );

    public static final Field field$ServerboundHelloPacket$name = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundHelloPacket, String.class, 0
            )
    );

    public static final Field field$ServerboundHelloPacket$uuid = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundHelloPacket, UUID.class, 0
            ) :
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundHelloPacket, Optional.class, 0
            )
    );

    public static final Field field$ClientboundResourcePackPushPacket$id =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundResourcePackPushPacket, UUID.class, 0
            );

    public static final Field field$ClientboundResourcePackPushPacket$prompt = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundResourcePackPushPacket,
                    VersionHelper.isOrAbove1_20_5() ? Optional.class : clazz$Component,
                    0
            )
    );

    public static final Class<?> clazz$ServerboundResourcePackPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayInResourcePackStatus", "network.protocol.common.ServerboundResourcePackPacket"),
                    List.of("network.protocol.game.ServerboundResourcePackPacket", "network.protocol.common.ServerboundResourcePackPacket")
            )
    );

    public static final Class<?> clazz$ServerboundResourcePackPacket$Action = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayInResourcePackStatus$EnumResourcePackStatus", "network.protocol.common.ServerboundResourcePackPacket$a"),
                    List.of("network.protocol.game.ServerboundResourcePackPacket$Action", "network.protocol.common.ServerboundResourcePackPacket$Action")
            )
    );

    public static final Method method$ServerboundResourcePackPacket$Action$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$ServerboundResourcePackPacket$Action, clazz$ServerboundResourcePackPacket$Action.arrayType()
            )
    );

    public static final Object instance$ServerboundResourcePackPacket$Action$SUCCESSFULLY_LOADED;
    public static final Object instance$ServerboundResourcePackPacket$Action$DECLINED;
    public static final Object instance$ServerboundResourcePackPacket$Action$FAILED_DOWNLOAD;
    public static final Object instance$ServerboundResourcePackPacket$Action$ACCEPTED;
    public static final Object instance$ServerboundResourcePackPacket$Action$DOWNLOADED;
    public static final Object instance$ServerboundResourcePackPacket$Action$INVALID_URL;
    public static final Object instance$ServerboundResourcePackPacket$Action$FAILED_RELOAD;
    public static final Object instance$ServerboundResourcePackPacket$Action$DISCARDED;

    static {
        try {
            Object[] values = (Object[]) method$ServerboundResourcePackPacket$Action$values.invoke(null);
            instance$ServerboundResourcePackPacket$Action$SUCCESSFULLY_LOADED = values[0];
            instance$ServerboundResourcePackPacket$Action$DECLINED = values[1];
            instance$ServerboundResourcePackPacket$Action$FAILED_DOWNLOAD = values[2];
            instance$ServerboundResourcePackPacket$Action$ACCEPTED = values[3];
            if (VersionHelper.isOrAbove1_20_3()) {
                instance$ServerboundResourcePackPacket$Action$DOWNLOADED = values[4];
                instance$ServerboundResourcePackPacket$Action$INVALID_URL = values[5];
                instance$ServerboundResourcePackPacket$Action$FAILED_RELOAD = values[6];
                instance$ServerboundResourcePackPacket$Action$DISCARDED = values[7];
            } else {
                instance$ServerboundResourcePackPacket$Action$DOWNLOADED = null;
                instance$ServerboundResourcePackPacket$Action$INVALID_URL = null;
                instance$ServerboundResourcePackPacket$Action$FAILED_RELOAD = null;
                instance$ServerboundResourcePackPacket$Action$DISCARDED = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final Constructor<?> constructor$ServerboundResourcePackPacket = requireNonNull(
            field$ClientboundResourcePackPushPacket$id != null
                    ? ReflectionUtils.getConstructor(clazz$ServerboundResourcePackPacket, UUID.class, clazz$ServerboundResourcePackPacket$Action)
                    : ReflectionUtils.getConstructor(clazz$ServerboundResourcePackPacket, clazz$ServerboundResourcePackPacket$Action)
    );

    public static final Class<?> clazz$DataComponentType = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass(
                    "core.component.DataComponentType"
            )
    );

    public static final Class<?> clazz$ClientIntentionPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.protocol.handshake.PacketHandshakingInSetProtocol",
                    "network.protocol.handshake.ClientIntentionPacket"
            )
    );

    public static final Field field$ClientIntentionPacket$protocolVersion = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientIntentionPacket, int.class, VersionHelper.isOrAbove1_20_2() ? 0 : 1
            )
    );

    // 1.20.2+
    public static final Class<?> clazz$ServerboundLoginAcknowledgedPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.login.ServerboundLoginAcknowledgedPacket")
            );

    public static final Field field$ServerboundResourcePackPacket$action = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundResourcePackPacket, clazz$ServerboundResourcePackPacket$Action, 0
            )
    );

    public static final Field field$CraftBlockStates$FACTORIES = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftBlockStates, "FACTORIES"
            )
    );

    public static final Class<?> clazz$CraftBlockStates$BlockEntityStateFactory = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("block.CraftBlockStates$BlockEntityStateFactory")
            )
    );

    public static final Class<?> clazz$ServerEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.EntityTrackerEntry",
                    "server.level.ServerEntity")
    );

    public static final Field field$ServerEntity$updateInterval = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ServerEntity, int.class, 0
            )
    );

    public static final Class<?> clazz$AbstractArrow = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.projectile.EntityArrow",
                    "world.entity.projectile.AbstractArrow"
            )
    );

    public static final Class<?> clazz$ClientboundCustomPayloadPacket = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("network.protocol.game.PacketPlayOutCustomPayload", "network.protocol.common.ClientboundCustomPayloadPacket"),
                    List.of("network.protocol.game.ClientboundCustomPayloadPacket", "network.protocol.common.ClientboundCustomPayloadPacket")
            )
    );

    public static final Constructor<?> constructor$ClientboundCustomPayloadPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundCustomPayloadPacket, 0)
    );

    // 1.20.2+
    public static final Constructor<?> constructor$DiscardedPayload = Optional.ofNullable(clazz$DiscardedPayload)
            .map(clazz -> ReflectionUtils.getTheOnlyConstructor(clazz))
            .orElse(null);

    public static final Class<?> clazz$PaperPluginClassLoader = ReflectionUtils.getClazz(
            "io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader"
    );

    public static final Field field$PaperPluginClassLoader$libraryLoader = Optional.ofNullable(clazz$PaperPluginClassLoader)
            .map(it -> ReflectionUtils.getDeclaredField(it, URLClassLoader.class, 0))
            .orElse(null);

    public static final Method method$SoundSource$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$SoundSource, clazz$SoundSource.arrayType()
            )
    );

    public static final Object instance$SoundSource$MASTER;
    public static final Object instance$SoundSource$MUSIC;
    public static final Object instance$SoundSource$RECORDS;
    public static final Object instance$SoundSource$WEATHER;
    public static final Object instance$SoundSource$BLOCKS;
    public static final Object instance$SoundSource$HOSTILE;
    public static final Object instance$SoundSource$NEUTRAL;
    public static final Object instance$SoundSource$PLAYERS;
    public static final Object instance$SoundSource$AMBIENT;
    public static final Object instance$SoundSource$VOICE;

    static {
        try {
            Object[] values = (Object[]) method$SoundSource$values.invoke(null);
            instance$SoundSource$MASTER = values[0];
            instance$SoundSource$MUSIC = values[1];
            instance$SoundSource$RECORDS = values[2];
            instance$SoundSource$WEATHER = values[3];
            instance$SoundSource$BLOCKS = values[4];
            instance$SoundSource$HOSTILE = values[5];
            instance$SoundSource$NEUTRAL = values[6];
            instance$SoundSource$PLAYERS = values[7];
            instance$SoundSource$AMBIENT = values[8];
            instance$SoundSource$VOICE = values[9];
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Class<?> clazz$MoverType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EnumMoveType",
                    "world.entity.MoverType"
            )
    );

    public static final Method method$MoverType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$MoverType, clazz$MoverType.arrayType()
            )
    );

    public static final Object instance$MoverType$SELF;
    public static final Object instance$MoverType$PLAYER;
    public static final Object instance$MoverType$PISTON;
    public static final Object instance$MoverType$SHULKER_BOX;
    public static final Object instance$MoverType$SHULKER;

    static {
        try {
            Object[] values = (Object[]) method$MoverType$values.invoke(null);
            instance$MoverType$SELF = values[0];
            instance$MoverType$PLAYER = values[1];
            instance$MoverType$PISTON = values[2];
            instance$MoverType$SHULKER_BOX = values[3];
            instance$MoverType$SHULKER = values[4];
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static final Class<?> clazz$AbstractArrow$Pickup = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.projectile.EntityArrow$PickupStatus",
                    "world.entity.projectile.AbstractArrow$Pickup"
            )
    );

    public static final Method method$AbstractArrow$Pickup$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$AbstractArrow$Pickup, clazz$AbstractArrow$Pickup.arrayType()
            )
    );

    public static final Object instance$AbstractArrow$Pickup$DISALLOWED;
    public static final Object instance$AbstractArrow$Pickup$ALLOWED;
    public static final Object instance$AbstractArrow$Pickup$CREATIVE_ONLY;

    static {
        try {
            Object[] values = (Object[]) method$AbstractArrow$Pickup$values.invoke(null);
            instance$AbstractArrow$Pickup$DISALLOWED = values[0];
            instance$AbstractArrow$Pickup$ALLOWED = values[1];
            instance$AbstractArrow$Pickup$CREATIVE_ONLY = values[2];
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

}
