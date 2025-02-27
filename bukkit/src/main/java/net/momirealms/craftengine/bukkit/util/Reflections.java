package net.momirealms.craftengine.bukkit.util;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.*;
import sun.misc.Unsafe;

import java.io.BufferedReader;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.Component"),
                    BukkitReflectionUtils.assembleMCClass("network.chat.IChatBaseComponent")
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
            ReflectionUtils.getConstructor(
                    clazz$ClientboundSystemChatPacket, clazz$Component, boolean.class
            )
    );

    public static final Field field$ClientboundSystemChatPacket$overlay = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, boolean.class, 0
            )
    );

    public static final Class<?> clazz$LevelWriter = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.LevelWriter"),
                    BukkitReflectionUtils.assembleMCClass("world.level.IWorldWriter")
            )
    );

    public static final Class<?> clazz$LevelReader = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.LevelReader"),
                    BukkitReflectionUtils.assembleMCClass("world.level.IWorldReader")
            )
    );

    public static final Class<?> clazz$DimensionType = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.dimension.DimensionType"),
                    BukkitReflectionUtils.assembleMCClass("world.level.dimension.DimensionManager")
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

    public static final Field field$ClientboundSystemChatPacket$text =
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSystemChatPacket, String.class, 0
            );

    public static final Class<?> clazz$ClientboundBossEventPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBossEventPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBoss")
            )
    );

    public static final Class<?> clazz$ClientboundBossEventPacket$Operation = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBossEventPacket$Operation"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBoss$Action")
            )
    );

    public static final Constructor<?> constructor$ClientboundBossEventPacket = requireNonNull(
            ReflectionUtils.getDeclaredConstructor(
                    clazz$ClientboundBossEventPacket,
                    UUID.class, clazz$ClientboundBossEventPacket$Operation
            )
    );

    public static final Class<?> clazz$ClientboundBossEventPacket$AddOperation = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBossEventPacket$AddOperation"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBoss$a")
            )
    );


    public static final Class<?> clazz$BossEvent$BossBarColor = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.BossEvent$BossBarColor"),
                    BukkitReflectionUtils.assembleMCClass("world.BossBattle$BarColor")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.BossEvent$BossBarOverlay"),
                    BukkitReflectionUtils.assembleMCClass("world.BossBattle$BarStyle")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("resources.ResourceLocation"),
                    BukkitReflectionUtils.assembleMCClass("resources.MinecraftKey")
            )
    );

    public static Object allocateAddOperationInstance() throws InstantiationException {
        return UNSAFE.allocateInstance(clazz$ClientboundBossEventPacket$AddOperation);
    }

    public static final Class<?> clazz$ClientboundBossEventPacket$UpdateNameOperation = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBossEventPacket$UpdateNameOperation"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBoss$e")
            )
    );

    public static final Constructor<?> constructor$ClientboundBossEventPacket$UpdateNameOperation = requireNonNull(
            ReflectionUtils.getDeclaredConstructor(
                    clazz$ClientboundBossEventPacket$UpdateNameOperation,
                    clazz$Component
            )
    );

    public static final Class<?> clazz$SoundEvent = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("sounds.SoundEvent"),
                    BukkitReflectionUtils.assembleMCClass("sounds.SoundEffect")
            )
    );

    public static final Constructor<?> constructor$SoundEvent = requireNonNull(
            VersionHelper.isVersionNewerThan1_21_2() ?
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
        if (VersionHelper.isVersionNewerThan1_20()) {
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.Component$Serializer"),
                    BukkitReflectionUtils.assembleMCClass("network.chat.IChatBaseComponent$ChatSerializer")
            )
    );

    public static final Class<?> clazz$HolderLookup$Provider = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.HolderLookup$Provider"),
            BukkitReflectionUtils.assembleMCClass("core.HolderLookup$b")
    );

    public static final Method method$Component$Serializer$fromJson = ReflectionUtils.getMethod(
            clazz$Component$Serializer,
            new String[] { "fromJson" },
            String.class, clazz$HolderLookup$Provider
    );

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

    public static final Constructor<?> constructor$ClientboundBundlePacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundBundlePacket, Iterable.class)
    );

    public static final Class<?> clazz$Packet = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.Packet")
            )
    );

    public static final Class<?> clazz$ServerPlayer = requireNonNull(ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("server.level.ServerPlayer"),
            BukkitReflectionUtils.assembleMCClass("server.level.EntityPlayer")
    ));

    public static final Class<?> clazz$ServerGamePacketListenerImpl = requireNonNull(ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("server.network.ServerGamePacketListenerImpl"),
            BukkitReflectionUtils.assembleMCClass("server.network.PlayerConnection")
    ));

    public static final Class<?> clazz$ServerCommonPacketListenerImpl = requireNonNull(
            clazz$ServerGamePacketListenerImpl.getSuperclass()
    );

    public static final Class<?> clazz$Connection = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.Connection"),
                    BukkitReflectionUtils.assembleMCClass("network.NetworkManager")
            )
    );

    public static final Field field$ServerCommonPacketListenerImpl$connection = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_2() ?
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

    public static final Method method$CraftPlayer$getHandle = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftPlayer, new String[] { "getHandle" })
    );

    public static final Field field$ServerPlayer$connection = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$ServerPlayer, clazz$ServerGamePacketListenerImpl, 0)
    );

    public static final Method method$ServerGamePacketListenerImpl$sendPacket = requireNonNull(
            ReflectionUtils.getMethods(clazz$ServerGamePacketListenerImpl, void.class, clazz$Packet).get(0)
    );

    public static final Method method$Connection$sendPacketImmediate = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_2() ?
            ReflectionUtils.getDeclaredMethod(
                    clazz$Connection, void.class, new String[] {"sendPacket", "b"}, clazz$Packet, clazz$PacketSendListener, boolean.class
            ) :
            ReflectionUtils.getDeclaredMethod(
                    clazz$Connection, void.class, new String[] {"sendPacket"}, clazz$Packet, clazz$PacketSendListener, Boolean.class
            )
    );

    public static final Field field$NetworkManager = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_2() ?
            ReflectionUtils.getDeclaredField(clazz$ServerGamePacketListenerImpl.getSuperclass(), clazz$Connection, 0) :
            ReflectionUtils.getDeclaredField(clazz$ServerGamePacketListenerImpl, clazz$Connection, 0)
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.EntityType"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.EntityTypes")
            )
    );

    public static final Class<?> clazz$EntityType$EntityFactory = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.EntityType$EntityFactory"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.EntityTypes$b")
            )
    );

    public static final Field field$EntityType$factory = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$EntityType, clazz$EntityType$EntityFactory, 0
            )
    );

    public static final Class<?> clazz$ClientboundAddEntityPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutSpawnEntity"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundAddEntityPacket")
            )
    );

    public static final Class<?> clazz$VoxelShape = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.phys.shapes.VoxelShape")
            )
    );

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

    public static final Class<?> clazz$PacketPlayOutNamedEntitySpawn =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutNamedEntitySpawn")
            );

    public static final Class<?> clazz$ClientboundRemoveEntitiesPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundRemoveEntitiesPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutEntityDestroy")
            )
    );

    public static final Field field$ClientboundRemoveEntitiesPacket$entityIds = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundRemoveEntitiesPacket, 0
            )
    );

    public static final Field field$ClientboundAddEntityPacket$entityId = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundAddEntityPacket, int.class, 0
            )
    );

    public static final Field field$PacketPlayOutNamedEntitySpawn$entityId = clazz$PacketPlayOutNamedEntitySpawn != null ?
            ReflectionUtils.getDeclaredField(
                    clazz$PacketPlayOutNamedEntitySpawn, int.class, 0
            ) : null;

    public static final Class<?> clazz$Vec3 = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.phys.Vec3"),
                    BukkitReflectionUtils.assembleMCClass("world.phys.Vec3D")
            )
    );

    public static final Field field$Vec3$x = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Vec3, double.class, 0
            )
    );

    public static final Field field$Vec3$y = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$Vec3, double.class, 1
            )
    );

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

    public static final Constructor<?> constructor$ClientboundAddEntityPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundAddEntityPacket,
                    int.class, UUID.class,
                    double.class, double.class, double.class,
                    float.class, float.class,
                    clazz$EntityType,
                    int.class, clazz$Vec3, double.class
            )
    );

    public static final Constructor<?> constructor$ClientboundRemoveEntitiesPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundRemoveEntitiesPacket, int[].class)
    );

    public static final Class<?> clazz$ClientboundSetPassengersPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutMount"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetPassengersPacket")
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

    public static Object allocateClientboundSetPassengersPacketInstance() throws InstantiationException {
            return UNSAFE.allocateInstance(clazz$ClientboundSetPassengersPacket);
    }

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutEntityMetadata"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetEntityDataPacket")
            )
    );

    public static final Constructor<?> constructor$ClientboundSetEntityDataPacket = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ClientboundSetEntityDataPacket,
                    int.class, List.class)
    );

    public static final Class<?> clazz$EntityDataSerializers = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.syncher.EntityDataSerializers"),
                    BukkitReflectionUtils.assembleMCClass("network.syncher.DataWatcherRegistry")
            )
    );

    public static final Class<?> clazz$EntityDataSerializer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.syncher.EntityDataSerializer"),
                    BukkitReflectionUtils.assembleMCClass("network.syncher.DataWatcherSerializer")
            )
    );

    public static final Class<?> clazz$EntityDataAccessor = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.syncher.EntityDataAccessor"),
                    BukkitReflectionUtils.assembleMCClass("network.syncher.DataWatcherObject")
            )
    );

    public static final Constructor<?> constructor$EntityDataAccessor = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$EntityDataAccessor, int.class, clazz$EntityDataSerializer
            )
    );

    public static final Class<?> clazz$SynchedEntityData$DataValue = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.syncher.SynchedEntityData$DataValue"),
                    BukkitReflectionUtils.assembleMCClass("network.syncher.DataWatcher$b")
            )
    );

    public static final Method method$SynchedEntityData$DataValue$create = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$SynchedEntityData$DataValue, clazz$SynchedEntityData$DataValue, clazz$EntityDataAccessor, Object.class
            )
    );

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

    public static final Field field$ClientboundSetEntityDataPacket$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSetEntityDataPacket, int.class, 0
            )
    );

    public static final Field field$ClientboundSetEntityDataPacket$packedItems = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundSetEntityDataPacket, List.class, 0
            )
    );

    public static final Field field$SynchedEntityData$DataValue$id = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SynchedEntityData$DataValue, int.class, 0
            )
    );

    public static final Field field$SynchedEntityData$DataValue$value = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SynchedEntityData$DataValue, 2
            )
    );

    public static final Class<?> clazz$ClientboundUpdateAttributesPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundUpdateAttributesPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutUpdateAttributes")
            )
    );

    public static final Class<?> clazz$AttributeInstance = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeInstance"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifiable")
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
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getConstructor(clazz$AttributeModifier, String.class, double.class, clazz$AttributeModifier$Operation):
            (
                !VersionHelper.isVersionNewerThan1_21() ?
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutUpdateAttributes$AttributeSnapshot")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.Holder$Reference"),
                    BukkitReflectionUtils.assembleMCClass("core.Holder$c")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.Attribute"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeBase")
            )
    );

    public static final Constructor<?> constructor$ClientboundUpdateAttributesPacket$AttributeSnapshot = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_5() ?
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundGameEventPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutGameStateChange")
            )
    );

    public static final Class<?> clazz$ClientboundGameEventPacket$Type = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundGameEventPacket$Type"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutGameStateChange$a")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.GameType"),
                    BukkitReflectionUtils.assembleMCClass("world.level.EnumGamemode")
            )
    );

    public static final Method method$GameType$getId = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$GameType, new String[] { "getId", "a" }
            )
    );

    public static final Class<?> clazz$Biome = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.biome.Biome"),
                    BukkitReflectionUtils.assembleMCClass("world.level.biome.BiomeBase")
            )
    );

    public static final Class<?> clazz$CraftWorld = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleCBClass("CraftWorld")
            )
    );

    public static final Class<?> clazz$ServerLevel = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ServerLevel"),
                    BukkitReflectionUtils.assembleMCClass("server.level.WorldServer")
            )
    );

    public static final Field field$CraftWorld$ServerLevel = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftWorld, clazz$ServerLevel, 0
            )
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.RegistryAccess$Frozen"),
                    BukkitReflectionUtils.assembleMCClass("core.IRegistryCustom$Dimension")
            )
    );

    public static final Class<?> clazz$RegistryAccess = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.RegistryAccess"),
                    BukkitReflectionUtils.assembleMCClass("core.IRegistryCustom")
            )
    );

    public static final Field field$LayeredRegistryAccess$composite = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$LayeredRegistryAccess, clazz$RegistryAccess$Frozen, 0
            )
    );

    public static final Class<?> clazz$Registry = requireNonNull(
            requireNonNull(ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.WritableRegistry"),
                    BukkitReflectionUtils.assembleMCClass("core.IRegistryWritable")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.DefaultedRegistry"),
                    BukkitReflectionUtils.assembleMCClass("core.RegistryBlocks")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetPlayerTeamPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutScoreboardTeam")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutScoreboardTeam$b")
            )
    );

    public static final Field field$ClientboundSetPlayerTeamPacket$Parameters$nametagVisibility = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundSetPlayerTeamPacket$Parameters, String.class, 0
            )
    );

    public static final Class<?> clazz$ServerConnectionListener = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.network.ServerConnectionListener"),
                    BukkitReflectionUtils.assembleMCClass("server.network.ServerConnection")
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

    public static final Class<?> clazz$ClientboundBlockUpdatePacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBlockUpdatePacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBlockChange")
            )
    );

    public static final Class<?> clazz$ClientboundSectionBlocksUpdatePacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSectionBlocksUpdatePacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutMultiBlockChange")
            )
    );

    public static final Class<?> clazz$BlockPos = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.BlockPos"),
                    BukkitReflectionUtils.assembleMCClass("core.BlockPosition")
            )
    );

    public static final Class<?> clazz$SectionPos = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.SectionPos"),
                    BukkitReflectionUtils.assembleMCClass("core.SectionPosition")
            )
    );

    public static final Class<?> clazz$Vec3i = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.Vec3i"),
                    BukkitReflectionUtils.assembleMCClass("core.BaseBlockPosition")
            )
    );

    public static final Field field$Vec3i$x = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 0)
    );

    public static final Field field$Vec3i$y = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 1)
    );

    public static final Field field$Vec3i$z = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Vec3i, int.class, 2)
    );

    public static final Class<?> clazz$BlockState = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockState"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.IBlockData")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.IdMapper"),
                    BukkitReflectionUtils.assembleMCClass("core.RegistryBlockID")
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

    public static final Method method$IdMapper$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, int.class, Object.class)
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.LevelAccessor"),
                    BukkitReflectionUtils.assembleMCClass("world.level.GeneratorAccess")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.Direction"),
                    BukkitReflectionUtils.assembleMCClass("core.EnumDirection")
            )
    );

    public static final Method method$Direction$values = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$Direction, clazz$Direction.arrayType()
            )
    );

    public static final Object instance$Direction$DOWN;
    public static final Object instance$Direction$UP;
    public static final Object[] instance$Directions;

    static {
        try {
            instance$Directions = (Object[]) method$Direction$values.invoke(null);
            instance$Direction$DOWN = instance$Directions[0];
            instance$Direction$UP = instance$Directions[1];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

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

    public static final Method method$CraftBlock$at = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlock, clazz$CraftBlock, clazz$LevelAccessor, clazz$BlockPos
            )
    );

    public static final Method method$CraftBlockStates$getBlockState = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockStates, clazz$CraftBlockState, clazz$LevelAccessor, clazz$BlockPos
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

    public static final Method method$CraftBlockData$createData = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockData, new String[]{"createData"}, clazz$CraftBlockData, clazz$BlockState
            )
    );

    public static final Method method$CraftBlockData$fromData = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftBlockData, new String[]{"fromData"}, clazz$CraftBlockData, clazz$BlockState
            )
    );

    public static final Constructor<?> constructor$BlockPos = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$BlockPos, int.class, int.class, int.class
            )
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.FriendlyByteBuf"),
                    BukkitReflectionUtils.assembleMCClass("network.PacketDataSerializer")
            )
    );

    public static final Class<?> clazz$RegistryFriendlyByteBuf =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.RegistryFriendlyByteBuf")
            );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.PalettedContainer"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.DataPaletteBlock")
            )
    );

    public static final Class<?> clazz$PalettedContainer$Data = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.PalettedContainer$Data"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.DataPaletteBlock$c")
            )
    );

    public static final Class<?> clazz$BitStorage = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("util.BitStorage"),
                    BukkitReflectionUtils.assembleMCClass("util.DataBits")
            )
    );

    public static final Class<?> clazz$Palette = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.Palette"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.DataPalette")
            )
    );

    public static final Field field$PalettedContainer$data = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$PalettedContainer, clazz$PalettedContainer$Data, 0
            )
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

    public static final Field field$CraftChunk$worldServer = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$CraftChunk, clazz$ServerLevel, 0
            )
    );

    public static final Class<?> clazz$ChunkAccess = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.ChunkAccess"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.IChunkAccess")
            )
    );

    public static final Class<?> clazz$LevelChunk = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.LevelChunk"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.Chunk")
            )
    );

    public static final Class<?> clazz$LevelChunkSection = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.LevelChunkSection"),
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.ChunkSection")
            )
    );

    public static final Class<?> clazz$ServerChunkCache = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ServerChunkCache"),
                    BukkitReflectionUtils.assembleMCClass("server.level.ChunkProviderServer")
            )
    );

    public static final Field field$ServerLevel$chunkSource = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerLevel, clazz$ServerChunkCache, 0
            )
    );

    public static final Method method$ServerChunkCache$blockChanged = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerChunkCache, void.class, clazz$BlockPos
            )
    );

    public static final Method method$ServerChunkCache$getChunkAtIfLoadedMainThread = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerChunkCache, clazz$LevelChunk, int.class, int.class
            )
    );

    public static final Field field$ChunkAccess$sections = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ChunkAccess, clazz$LevelChunkSection.arrayType(), 0
            )
    );

    public static final Class<?> clazz$BlockEntity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.BlockEntity"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.TileEntity")
            )
    );

    public static final Class<?> clazz$AbstractFurnaceBlockEntity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.AbstractFurnaceBlockEntity"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.TileEntityFurnace")
            )
    );

    public static final Class<?> clazz$CampfireBlockEntity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.CampfireBlockEntity"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.entity.TileEntityCampfire")
            )
    );

    public static final Field field$ChunkAccess$blockEntities;

    static {
        Field targetField = null;
        for (Field field : clazz$ChunkAccess.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length == 2 &&
                            actualTypeArguments[0].equals(clazz$BlockPos) &&
                            actualTypeArguments[1].equals(clazz$BlockEntity)) {
                        field.setAccessible(true);
                        targetField = field;
                    }
                }
            }
        }
        field$ChunkAccess$blockEntities = targetField;
    }

    public static final Method method$LevelChunkSection$setBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class, clazz$BlockState, boolean.class
            )
    );

    public static final Class<?> clazz$StatePredicate = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBehaviour$StatePredicate"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBase$f")
            )
    );

    public static final Class<?> clazz$BlockBehaviour$Properties = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBehaviour$Properties"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBase$Info")
            )
    );

    public static final Class<?> clazz$BlockBehaviour = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBehaviour"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBase")
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
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffectList"),
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffect")
            )
    );

    public static final Class<?> clazz$MobEffectInstance = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffectInstance"),
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffect")
            )
    );

    public static final Class<?> clazz$ParticleType = requireNonNull(
            Optional.of(Objects.requireNonNull(ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.particles.ParticleType")))).map(it -> {
                if (it.getSuperclass() != Object.class) {
                    return it.getSuperclass();
                }
                return it;
            }).orElseThrow()
    );

    public static final Class<?> clazz$SoundType = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.SoundType"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.SoundEffectType")
            )
    );

    public static final Constructor<?> constructor$SoundType = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$SoundType, float.class, float.class, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent
            )
    );

    public static final Class<?> clazz$ItemLike = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.ItemLike"),
                    BukkitReflectionUtils.assembleMCClass("world.level.IMaterial")
            )
    );

    public static final Class<?> clazz$Item = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.Item")
            )
    );

    public static final Class<?> clazz$FluidState = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.Fluid"),
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FluidState")
            )
    );

    public static final Class<?> clazz$Fluid = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FluidType"),
                    BukkitReflectionUtils.assembleMCClass("world.level.material.Fluid")
            )
    );

    public static final Class<?> clazz$RecipeType = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeType"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.Recipes")
            )
    );

    public static final Class<?> clazz$WorldGenLevel = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.WorldGenLevel"),
                    BukkitReflectionUtils.assembleMCClass("world.level.GeneratorAccessSeed")
            )
    );

    public static final Class<?> clazz$ChunkGenerator = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.ChunkGenerator")
            )
    );

    // 1.20.1-1.20.2
    public static final Class<?> clazz$AbstractTreeGrower =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.grower.AbstractTreeGrower"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.grower.WorldGenTreeProvider")
            );

    public static final Class<?> clazz$ConfiguredFeature = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.levelgen.feature.ConfiguredFeature"),
                    BukkitReflectionUtils.assembleMCClass("world.level.levelgen.feature.WorldGenFeatureConfigured")
            )
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
    public static final Object instance$InternalRegistries$DIMENSION_TYPE;

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
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.StateDefinition"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockStateList")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.MappedRegistry"),
                    BukkitReflectionUtils.assembleMCClass("core.RegistryMaterials")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.MapColor"),
                    BukkitReflectionUtils.assembleMCClass("world.level.material.MaterialMapColor")
            )
    );

    public static final Method method$MapColor$byId = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$MapColor, clazz$MapColor, int.class
            )
    );

    public static final Class<?> clazz$PushReaction = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.PushReaction"),
                    BukkitReflectionUtils.assembleMCClass("world.level.material.EnumPistonReaction")
            )
    );

    public static final Method method$PushReaction$values = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$PushReaction, new String[] { "values" }
            )
    );

    public static final Class<?> clazz$NoteBlockInstrument = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.NoteBlockInstrument"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.BlockPropertyInstrument")
            )
    );

    public static final Method method$NoteBlockInstrument$values = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$NoteBlockInstrument, new String[] { "values" }
            )
    );

    public static final Class<?> clazz$BlockStateBase = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBehaviour$BlockStateBase"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.BlockBase$BlockData")
            )
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

    public static final Class<?> clazz$AABB = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.phys.AABB"),
                    BukkitReflectionUtils.assembleMCClass("world.phys.AxisAlignedBB")
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

    public static final Class<?> clazz$BlockGetter = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.BlockGetter"),
                    BukkitReflectionUtils.assembleMCClass("world.level.IBlockAccess")
            )
    );

    public static final Class<?> clazz$StateHolder = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.StateHolder"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.IBlockDataHolder")
            )
    );

    public static final Field field$StateHolder$owner = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$StateHolder, Object.class, 0
            )
    );

    public static final Class<?> clazz$CollisionContext = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.phys.shapes.CollisionContext"),
                    BukkitReflectionUtils.assembleMCClass("world.phys.shapes.VoxelShapeCollision")
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

    public static final Method method$BlockGetter$getBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockGetter, clazz$BlockState, clazz$BlockPos
            )
    );

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

    public static final Class<?> clazz$MappedRegistry$TagSet = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.MappedRegistry$TagSet")
            //BukkitReflectionUtils.assembleMCClass("core.RegistryMaterials$TagSet") 1.21.2+
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLevelParticlesPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutWorldParticles")
            )
    );

    public static final Constructor<?> constructor$ClientboundLevelParticlesPacket = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_5() ?
                    ReflectionUtils.getDeclaredConstructor(clazz$ClientboundLevelParticlesPacket, clazz$RegistryFriendlyByteBuf) :
                    ReflectionUtils.getConstructor(clazz$ClientboundLevelParticlesPacket, clazz$FriendlyByteBuf)
    );

    public static final Class<?> clazz$ParticleOptions = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.particles.ParticleOptions"),
                    BukkitReflectionUtils.assembleMCClass("core.particles.ParticleParam")
            )
    );

    public static final Class<?> clazz$BlockParticleOption = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.particles.BlockParticleOption"),
                    BukkitReflectionUtils.assembleMCClass("core.particles.ParticleParamBlock")
            )
    );

    public static final Field field$ClientboundLevelParticlesPacket$particle = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ClientboundLevelParticlesPacket, clazz$ParticleOptions, 0
            )
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLightUpdatePacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutLightUpdate")
            )
    );

    public static final Class<?> clazz$ChunkPos = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.ChunkPos"),
                    BukkitReflectionUtils.assembleMCClass("world.level.ChunkCoordIntPair")
            )
    );

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

    public static final Constructor<?> constructor$ClientboundLightUpdatePacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundLightUpdatePacket, clazz$ChunkPos, clazz$LevelLightEngine, BitSet.class, BitSet.class
            )
    );

    public static final Class<?> clazz$ChunkHolder = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ChunkHolder"),
                    BukkitReflectionUtils.assembleMCClass("server.level.PlayerChunk")
            )
    );

    public static final Class<?> clazz$ChunkMap = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ChunkMap"),
                    BukkitReflectionUtils.assembleMCClass("server.level.PlayerChunkMap")
            )
    );

    public static final Method method$ChunkHolder$getPlayers = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ChunkHolder, List.class, boolean.class
            )
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

    public static final Method method$ServerChunkCache$getVisibleChunkIfPresent = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerChunkCache, clazz$ChunkHolder, long.class
            )
    );

    public static final Class<?> clazz$LightLayer = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.LightLayer"),
                    BukkitReflectionUtils.assembleMCClass("world.level.EnumSkyBlock")
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
            VersionHelper.isVersionNewerThan1_21_2() ?
            ReflectionUtils.getMethod(clazz$ChunkHolder, boolean.class, clazz$LightLayer, int.class) :
                    ReflectionUtils.getMethod(clazz$ChunkHolder, void.class, clazz$LightLayer, int.class)
    );

    public static final Class<?> clazz$ServerboundPlayerActionPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundPlayerActionPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInBlockDig")
            )
    );

    public static final Class<?> clazz$ServerboundPlayerActionPacket$Action = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundPlayerActionPacket$Action"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInBlockDig$EnumPlayerDigType")
            )
    );

    public static final Field field$ServerboundPlayerActionPacket$pos = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundPlayerActionPacket, clazz$BlockPos, 0
            )
    );

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

    static {
        try {
            if (VersionHelper.isVersionNewerThan1_20_5()) {
                Object block_break_speed = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", VersionHelper.isVersionNewerThan1_21_2() ? "block_break_speed" : "player.block_break_speed");
                @SuppressWarnings("unchecked")
                Optional<Object> breakSpeedHolder = (Optional<Object>) method$Registry$getHolder0.invoke(instance$BuiltInRegistries$ATTRIBUTE, block_break_speed);
                instance$Holder$Attribute$block_break_speed = breakSpeedHolder.orElse(null);

                Object block_interaction_range = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", VersionHelper.isVersionNewerThan1_21_2() ? "block_interaction_range" : "player.block_interaction_range");
                @SuppressWarnings("unchecked")
                Optional<Object> blockInteractionRangeHolder = (Optional<Object>) method$Registry$getHolder0.invoke(instance$BuiltInRegistries$ATTRIBUTE, block_interaction_range);
                instance$Holder$Attribute$block_interaction_range = blockInteractionRangeHolder.orElse(null);
            } else {
                instance$Holder$Attribute$block_break_speed = null;
                instance$Holder$Attribute$block_interaction_range = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$ServerPlayer$getAttribute = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$AttributeInstance, clazz$Holder) :
            ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$AttributeInstance, clazz$Attribute)
    );

    public static final Class<?> clazz$ServerPlayerGameMode = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.ServerPlayerGameMode"),
                    BukkitReflectionUtils.assembleMCClass("server.level.PlayerInteractManager")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.Player"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.EntityHuman")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.Level"),
                    BukkitReflectionUtils.assembleMCClass("world.level.World")
            )
    );

    public static final Method method$Entity$level = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Entity, clazz$Level
            )
    );

    public static final Method method$BlockStateBase$getDestroyProgress = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$BlockStateBase, float.class, clazz$Player, clazz$BlockGetter, clazz$BlockPos
            )
    );

    public static final Class<?> clazz$ClientboundBlockDestructionPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundBlockDestructionPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutBlockBreakAnimation")
            )
    );

    public static final Constructor<?> constructor$ClientboundBlockDestructionPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundBlockDestructionPacket, int.class, clazz$BlockPos, int.class
            )
    );

    public static final Class<?> clazz$ServerboundSwingPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundSwingPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInArmAnimation")
            )
    );

    public static final Class<?> clazz$InteractionHand = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.InteractionHand"),
                    BukkitReflectionUtils.assembleMCClass("world.EnumHand")
            )
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.EquipmentSlot"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.EnumItemSlot")
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

    public static final Method method$Block$defaultBlockState = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Block, clazz$BlockState
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundInteractPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseEntity")
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$Action = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundInteractPacket$Action"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction")
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$InteractionAction = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundInteractPacket$InteractionAction"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseEntity$d")
            )
    );

    public static final Field field$ServerboundInteractPacket$InteractionAction$hand = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$ServerboundInteractPacket$InteractionAction, clazz$InteractionHand, 0
            )
    );

    public static final Class<?> clazz$ServerboundInteractPacket$InteractionAtLocationAction = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundInteractPacket$InteractionAtLocationAction"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseEntity$e")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundInteractPacket$ActionType"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseEntity$b")
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

    public static final Field field$ServerboundInteractPacket$entityId = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ServerboundInteractPacket, int.class, 0
            )
    );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundUpdateMobEffectPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutEntityEffect")
            )
    );

    public static final Class<?> clazz$ClientboundRemoveMobEffectPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundRemoveMobEffectPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutRemoveEntityEffect")
            )
    );

    public static final Object instance$MobEffecr$mining_fatigue;
    public static final Object instance$MobEffecr$haste;

    // for 1.20.1-1.20.4
    static {
        try {
            Object mining_fatigue = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "mining_fatigue");
            instance$MobEffecr$mining_fatigue = method$Registry$get.invoke(instance$BuiltInRegistries$MOB_EFFECT, mining_fatigue);
            Object haste = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "haste");
            instance$MobEffecr$haste = method$Registry$get.invoke(instance$BuiltInRegistries$MOB_EFFECT, haste);
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
            !VersionHelper.isVersionNewerThan1_20_5() ?
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
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, clazz$MobEffect, 0
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, clazz$Holder, 0
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$amplifier = requireNonNull(
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 0
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 1
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$duration = requireNonNull(
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 1
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, int.class, 2
            )
    );

    public static final Field field$ClientboundUpdateMobEffectPacket$flags = requireNonNull(
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 1
            ) :
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundUpdateMobEffectPacket, byte.class, 0
            )
    );

    public static final Method method$ServerPlayer$getEffect = requireNonNull(
            !VersionHelper.isVersionNewerThan1_20_5() ?
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, clazz$MobEffectInstance, clazz$MobEffect
            ) :
            ReflectionUtils.getMethod(
                    clazz$ServerPlayer, clazz$MobEffectInstance, clazz$Holder
            )
    );

    public static final Object instance$SoundEvent$EMPTY;

    static {
        try {
            Object key = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "intentionally_empty");
            instance$SoundEvent$EMPTY = method$Registry$get.invoke(instance$BuiltInRegistries$SOUND_EVENT, key);
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundSetCreativeModeSlotPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInSetCreativeSlot")
            )
    );

    public static final Field field$ServerboundSetCreativeModeSlotPacket$slotNum = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_5() ?
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

    public static final Method method$CraftItemStack$asCraftMirror = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftItemStack, new String[]{"asCraftMirror"}, clazz$CraftItemStack, clazz$ItemStack
            )
    );

    public static final Method method$CraftItemStack$asNMSMirror = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$CraftItemStack, new String[]{"asNMSCopy"}, clazz$ItemStack, ItemStack.class
            )
    );

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
            VersionHelper.isVersionNewerThan1_21_2() ?
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.FallingBlock"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.BlockFalling")
            )
    );

    public static final Method method$FallingBlock$isFree = requireNonNull(
            ReflectionUtils.getStaticMethod(
                    clazz$FallingBlock, boolean.class, clazz$BlockState
            )
    );

    public static final Class<?> clazz$FallingBlockEntity = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.item.FallingBlockEntity"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.item.EntityFallingBlock")
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

    static {
        try {
            Object air = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "air");
            instance$Blocks$AIR = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, air);
            instance$Blocks$AIR$defaultState = method$Block$defaultBlockState.invoke(instance$Blocks$AIR);
            Object fire = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "fire");
            instance$Blocks$FIRE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, fire);
            Object stone = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "stone");
            instance$Blocks$STONE = method$Registry$get.invoke(instance$BuiltInRegistries$BLOCK, stone);
            instance$Blocks$STONE$defaultState = method$Block$defaultBlockState.invoke(instance$Blocks$STONE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$BlockStateBase$hasTag = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$BlockStateBase, boolean.class, clazz$TagKey
            )
    );

    public static final Method method$Level$removeBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Level, boolean.class, clazz$BlockPos, boolean.class
            )
    );

    public static final Class<?> clazz$MutableBlockPos = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.BlockPos$MutableBlockPos"),
                    BukkitReflectionUtils.assembleMCClass("core.BlockPosition$MutableBlockPosition")
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

    public static final Class<?> clazz$LeavesBlock = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.LeavesBlock"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.BlockLeaves")
            )
    );

    public static final Class<?> clazz$IntegerProperty = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.IntegerProperty"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.BlockStateInteger")
            )
    );

    public static final Class<?> clazz$Property = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.Property"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.state.properties.IBlockState")
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

    public static final Method method$Level$setBlock = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Level, boolean.class, clazz$BlockPos, clazz$BlockState, int.class
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundRespawnPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutRespawn")
            )
    );

    public static final Class<?> clazz$ClientboundLoginPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLoginPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutLogin")
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
                if (VersionHelper.isVersionNewerThan1_20_5()) {
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
    public static final Object instance$EntityType$FALLING_BLOCK;
    public static final Object instance$EntityType$INTERACTION;

    static {
        try {
            Object textDisplay = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "text_display");
            instance$EntityType$TEXT_DISPLAY = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, textDisplay);
            Object itemDisplay = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "item_display");
            instance$EntityType$ITEM_DISPLAY = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, itemDisplay);
            Object fallingBlock = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "falling_block");
            instance$EntityType$FALLING_BLOCK = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, fallingBlock);
            Object interaction = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "interaction");
            instance$EntityType$INTERACTION = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ENTITY_TYPE, interaction);
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
            instance$RecipeType$CRAFTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "crafting"));
            instance$RecipeType$SMELTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "smelting"));
            instance$RecipeType$BLASTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "blasting"));
            instance$RecipeType$SMOKING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "smoking"));
            instance$RecipeType$CAMPFIRE_COOKING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "campfire_cooking"));
            instance$RecipeType$STONECUTTING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "stonecutting"));
            instance$RecipeType$SMITHING = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$RECIPE_TYPE, method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "smithing"));
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.FireBlock"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.BlockFire")
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
    public static final Object instance$ItemStack$Air;

    static {
        try {
            Object air = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "air");
            instance$Items$AIR = method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$ITEM, air);
            instance$ItemStack$Air = constructor$ItemStack.newInstance(instance$Items$AIR);
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

    // 1.21.3+
    public static final Class<?> clazz$ClientboundEntityPositionSyncPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundEntityPositionSyncPacket")
            );

    public static final Field field$ClientboundEntityPositionSyncPacket$id = Optional.ofNullable(clazz$ClientboundEntityPositionSyncPacket)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, int.class, 0))
            .orElse(null);

    public static final Class<?> clazz$ClientboundMoveEntityPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundMoveEntityPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutEntity")
            )
    );

    public static final Field field$ClientboundMoveEntityPacket$entityId = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(
                    clazz$ClientboundMoveEntityPacket, int.class, 0
            )
    );

    public static final Class<?> clazz$ClientboundMoveEntityPacket$Pos = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundMoveEntityPacket$Pos"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutEntity$PacketPlayOutRelEntityMove")
            )
    );

    public static final Class<?> clazz$ServerboundUseItemOnPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundUseItemOnPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayInUseItem")
            )
    );

    @SuppressWarnings("deprecation")
    public static final Method method$World$spawnEntity = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_2() ?
                    ReflectionUtils.getMethod(World.class, Entity.class, Location.class, EntityType.class, CreatureSpawnEvent.SpawnReason.class, Consumer.class) :
                    ReflectionUtils.getMethod(World.class, Entity.class, Location.class, EntityType.class, CreatureSpawnEvent.SpawnReason.class, org.bukkit.util.Consumer.class)
    );

    // 1.21.4+
    public static final Class<?> clazz$ServerboundPickItemFromEntityPacket =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ServerboundPickItemFromEntityPacket")
            );

    public static final Field field$ServerboundPickItemFromEntityPacket$id = Optional.ofNullable(clazz$ServerboundPickItemFromEntityPacket)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, int.class, 0))
            .orElse(null);

    public static final Class<?> clazz$ClientboundSoundPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundSoundPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutNamedSoundEffect")
            )
    );

    public static final Class<?> clazz$SoundSource = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("sounds.SoundSource"),
                    BukkitReflectionUtils.assembleMCClass("sounds.SoundCategory")
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
            ReflectionUtils.getStaticMethod(
                    clazz$CraftEventFactory, BlockPlaceEvent.class, clazz$ServerLevel, clazz$Player, clazz$InteractionHand, BlockState.class, int.class, int.class, int.class
            )
    );

    public static final Class<?> clazz$Abilities = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.Abilities"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.PlayerAbilities")
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

    static {
        try {
            Object waterId = method$ResourceLocation$fromNamespaceAndPath.invoke(null, "minecraft", "water");
            instance$Fluids$WATER = method$Registry$get.invoke(instance$BuiltInRegistries$FLUID, waterId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$FlowingFluid = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FlowingFluid"),
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FluidTypeFlowing")
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
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.IResourceManager"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.ResourceManager")
            )
    );

    public static final Method method$FileToIdConverter$listMatchingResources = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$FileToIdConverter, Map.class, new String[]{"listMatchingResources", "a"}, clazz$ResourceManager
            )
    );

    public static final Class<?> clazz$Resource = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.IResource"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.Resource")
            )
    );

    public static final Method method$Resource$openAsReader = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$Resource, BufferedReader.class
            )
    );

    public static final Class<?> clazz$MultiPackResourceManager = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.MultiPackResourceManager"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.ResourceManager")
            )
    );

    public static final Class<?> clazz$PackType = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.PackType"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.EnumResourcePackType")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.repository.ResourcePackRepository"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.repository.PackRepository")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.repository.ResourcePackLoader"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.repository.Pack")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.PackResources"),
                    BukkitReflectionUtils.assembleMCClass("server.packs.IResourcePack")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeManager"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.CraftingManager")
            )
    );

    public static final Class<?> clazz$RecipeManager$CachedCheck = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeManager$CachedCheck"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.CraftingManager$a")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.Inventory"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.player.PlayerInventory")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.Ingredient"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeItemStack")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapedRecipe"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapedRecipes")
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

    // 1.20.2+
    public static final Field field$RecipeHolder$recipe = Optional.ofNullable(clazz$RecipeHolder)
            .map(it -> ReflectionUtils.getDeclaredField(it, 1))
            .orElse(null);

    public static final Field field$RecipeHolder$id = Optional.ofNullable(clazz$RecipeHolder)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0))
            .orElse(null);

    public static final Class<?> clazz$ShapelessRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapelessRecipe"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapelessRecipes")
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
        if (VersionHelper.isVersionNewerThan1_21_2()) {
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
        } else if (VersionHelper.isVersionNewerThan1_20_2()) {
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.inventory.ResultContainer"),
                    BukkitReflectionUtils.assembleMCClass("world.inventory.InventoryCraftResult")
            )
    );

    public static final Class<?> clazz$Container = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.Container"),
                    BukkitReflectionUtils.assembleMCClass("world.IInventory")
            )
    );

    public static final Class<?> clazz$Recipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.Recipe"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.IRecipe")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.LivingEntity"),
                    BukkitReflectionUtils.assembleMCClass("world.entity.EntityLiving")
            )
    );

    // 1.20.5+
    public static final Method method$ItemStack$hurtAndBreak =
            ReflectionUtils.getMethod(
                    clazz$ItemStack, void.class, int.class, clazz$LivingEntity, clazz$EquipmentSlot
            );

    // for 1.20.1-1.21.1
    public static final Class<?> clazz$AbstractCookingRecipe = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.AbstractCookingRecipe"),
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeCooking")
            )
    );

    // for 1.20.1-1.21.1
    public static final Field field$AbstractCookingRecipe$input =
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractCookingRecipe, clazz$Ingredient, 0
            );

    // for 1.21.2+
    public static final Class<?> clazz$SingleItemRecipe =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.item.crafting.SingleItemRecipe")
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

    public static final Field field$AbstractFurnaceBlockEntity$recipeType = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$AbstractFurnaceBlockEntity, clazz$RecipeType, 0
            )
    );

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

    // 1.20.1-1.21.1
    public static final Method method$RecipeManager$getRecipeFor0 =
            ReflectionUtils.getMethod(
                    clazz$RecipeManager, Optional.class, clazz$RecipeType, clazz$Container, clazz$Level, clazz$ResourceLocation
            );

    // 1.21.2+
    public static final Method method$RecipeManager$getRecipeFor1 =
            ReflectionUtils.getMethod(
                    clazz$RecipeManager, Optional.class, clazz$RecipeType, clazz$RecipeInput, clazz$Level, clazz$ResourceKey
            );

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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.SimpleContainer"),
                    BukkitReflectionUtils.assembleMCClass("world.InventorySubcontainer")
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.block.BonemealableBlock"),
                    BukkitReflectionUtils.assembleMCClass("world.level.block.IBlockFragilePlantElement")
            )
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            VersionHelper.isVersionNewerThan1_20_2() ?
                    ReflectionUtils.getMethod(
                            clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState
                    ) :
                    ReflectionUtils.getMethod(
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
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundLevelEventPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutWorldEvent")
            )
    );

    public static final Constructor<?> constructor$ClientboundLevelEventPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundLevelEventPacket, int.class, clazz$BlockPos, int.class, boolean.class
            )
    );

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
            ReflectionUtils.getMethod(
                    clazz$ServerLevel, void.class, clazz$Player, int.class, clazz$BlockPos, int.class
            )
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
            ReflectionUtils.getDeclaredMethod(
                    clazz$ServerGamePacketListenerImpl, void.class, clazz$ItemStack
            );

    public static final Class<?> clazz$ClientboundOpenScreenPacket = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.ClientboundOpenScreenPacket"),
                    BukkitReflectionUtils.assembleMCClass("network.protocol.game.PacketPlayOutOpenWindow")
            )
    );

    public static final Class<?> clazz$MenuType = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.inventory.MenuType"),
                    BukkitReflectionUtils.assembleMCClass("world.inventory.Containers")
            )
    );

    public static final Constructor<?> constructor$ClientboundOpenScreenPacket = requireNonNull(
            ReflectionUtils.getConstructor(
                    clazz$ClientboundOpenScreenPacket, int.class, clazz$MenuType, clazz$Component
            )
    );

    public static final Class<?> clazz$AbstractContainerMenu = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.inventory.AbstractContainerMenu"),
                    BukkitReflectionUtils.assembleMCClass("world.inventory.Container")
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
}
