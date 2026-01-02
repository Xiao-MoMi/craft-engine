package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class CoreReflections {

    public static final Class<?> clazz$RandomSource = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("util.RandomSource"))
    );

    public static final Class<?> clazz$BitStorage = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "util.DataBits",
                    "util.BitStorage"
            )
    );

    public static final Method method$BitStorage$getBits = requireNonNull(
            ReflectionUtils.getMethod(clazz$BitStorage, int.class)
    );

    public static final Method method$BitStorage$getRaw = requireNonNull(
            ReflectionUtils.getMethod(clazz$BitStorage, long[].class)
    );

    public static final Method method$RandomSource$nextFloat = requireNonNull(
            ReflectionUtils.getMethod(clazz$RandomSource, float.class)
    );

    public static final Class<?> clazz$CrudeIncrementalIntIdentityHashBiMap = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "util.RegistryID",
                    "util.CrudeIncrementalIntIdentityHashBiMap"
            )
    );

    public static final Field field$CrudeIncrementalIntIdentityHashBiMap$keys = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CrudeIncrementalIntIdentityHashBiMap, Object.class.arrayType(), 0)
    );
    
    public static final Class<?> clazz$ResourceLocation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("resources.MinecraftKey"),
                    List.of("resources.ResourceLocation", "resources.Identifier")
            )
    );

    public static final Class<?> clazz$ResourceKey = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("resources.ResourceKey"))
    );

    public static final Field field$ResourceKey$registry = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ResourceKey, clazz$ResourceLocation, 0)
    );

    public static final Field field$ResourceKey$location = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ResourceKey, clazz$ResourceLocation, 1)
    );

    public static final Method method$ResourceKey$create = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$ResourceKey, clazz$ResourceKey, clazz$ResourceKey, clazz$ResourceLocation)
    );

    public static final Method method$ResourceLocation$fromNamespaceAndPath = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$ResourceLocation, clazz$ResourceLocation, String.class, String.class)
    );

    public static final Class<?> clazz$FileToIdConverter = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("resources.FileToIdConverter"))
    );

    public static final Method method$FileToIdConverter$json = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$FileToIdConverter, clazz$FileToIdConverter, String.class)
    );

    public static final MethodHandle methodHandle$FileToIdConverter$json;

    static {
        try {
            methodHandle$FileToIdConverter$json = ReflectionUtils.unreflectMethod(method$FileToIdConverter$json)
                    .asType(MethodType.methodType(Object.class, String.class));
        } catch (Throwable t) {
            throw new ReflectionInitException("Failed to initialize methodHandle$FileToIdConverter$json", t);
        }
    }
    
    public static final Class<?> clazz$RegistryOps = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "resources.RegistryOps",
                    "resources.RegistryOps"
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
                    ReflectionUtils.getConstructor(clazz$SoundEvent, clazz$ResourceLocation, Optional.class) :
                    ReflectionUtils.getDeclaredConstructor(clazz$SoundEvent, clazz$ResourceLocation, float.class, boolean.class)
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

    public static final Field field$SoundEvent$location = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$SoundEvent, clazz$ResourceLocation, 0)
    );

    public static final Class<?> clazz$SoundSource = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "sounds.SoundCategory",
                    "sounds.SoundSource"
            )
    );

    public static final Method method$SoundSource$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$SoundSource, clazz$SoundSource.arrayType())
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
            throw new ReflectionInitException("Failed to init SoundSource", e);
        }
    }

    // 1.21+
    public static final Class<?> clazz$PacketReport =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("data.info.PacketReport"));

    // 1.21+
    public static final Constructor<?> constructor$PacketReport = Optional.ofNullable(clazz$PacketReport)
            .map(it -> ReflectionUtils.getConstructor(it, 0))
            .orElse(null);

    // 1.21+
    public static final Method method$PacketReport$serializePackets = Optional.ofNullable(clazz$PacketReport)
            .map(it -> ReflectionUtils.getDeclaredMethod(it, JsonElement.class))
            .orElse(null);

    public static final Class<?> clazz$Component = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.IChatBaseComponent",
                    "network.chat.Component"
            )
    );

    // 1.20~1.21.5
    public static final Class<?> clazz$Component$Serializer =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.IChatBaseComponent$ChatSerializer",
                    "network.chat.Component$Serializer"
            );

    public static final Class<?> clazz$ComponentContents = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.chat.ComponentContents")
            )
    );

    public static final Method method$Component$empty = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Component, clazz$Component)
    );

    public static final Object instance$Component$empty;

    static {
        try {
            instance$Component$empty = method$Component$empty.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to get empty component", e);
        }
    }

    public static final Method method$Component$getString = requireNonNull(
            ReflectionUtils.getMethod(clazz$Component, String.class, new String[]{"getString", "a"})
    );

    public static final Class<?> clazz$HolderLookup$Provider = BukkitReflectionUtils.findReobfOrMojmapClass(
            VersionHelper.isOrAbove1_20_5() ? "core.HolderLookup$a" : "core.HolderLookup$b",
            "core.HolderLookup$Provider"
    );

    public static final Class<?> clazz$Holder = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.Holder"))
    );

    // 1.20.6+
    public static final Method method$Holder$getRegisteredName = ReflectionUtils.getMethod(clazz$Holder, String.class);

    public static final Class<?> clazz$Holder$Reference = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.Holder$c",
                    "core.Holder$Reference"
            )
    );

    public static final Method method$Holder$value = requireNonNull(
            ReflectionUtils.getMethod(clazz$Holder, new String[]{"a", "value"})
    );

    public static final Method method$Holder$direct = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Holder, clazz$Holder, Object.class)
    );

    public static final Class<?> clazz$LayeredRegistryAccess = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.LayeredRegistryAccess"))
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
            ReflectionUtils.getDeclaredField(clazz$LayeredRegistryAccess, clazz$RegistryAccess$Frozen, 0)
    );

    public static final Class<?> clazz$Registry = requireNonNull(
            requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.IRegistryWritable",
                    "core.WritableRegistry"
            )).getInterfaces()[0]
    );

//    public static final Method method$RegistryAccess$registryOrThrow = requireNonNull(
//            ReflectionUtils.getMethod(
//                    clazz$RegistryAccess, clazz$Registry, clazz$ResourceKey
//            )
//    );

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
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.registries.Registries"))
    );

    public static final Class<?> clazz$BuiltInRegistries = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.registries.BuiltInRegistries"))
    );

    public static final Class<?> clazz$DefaultedRegistry = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryBlocks",
                    "core.DefaultedRegistry"
            )
    );

//    public static final Method method$Registry$getKey = requireNonNull(
//            ReflectionUtils.getMethod(clazz$Registry, clazz$ResourceLocation, Object.class)
//    );

//    // use ResourceLocation
//    public static final Method method$Registry$getHolder0;
//    // use ResourceKey
//    public static final Method method$Registry$getHolder1;
//
//    static {
//        List<Method> methods = ReflectionUtils.getMethods(clazz$Registry, Optional.class, clazz$ResourceLocation);
//        Method theMethod1 = null;
//        for (Method method : methods) {
//            Type returnType = method.getGenericReturnType();
//            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
//                if (returnType instanceof ParameterizedType parameterizedType) {
//                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//                    if (actualTypeArguments.length == 1) {
//                        if (actualTypeArguments[0] instanceof ParameterizedType) {
//                            theMethod1 = method;
//                        }
//                    }
//                }
//            }
//        }
//        method$Registry$getHolder0 = theMethod1;
//    }
//
//    static {
//        List<Method> methods = ReflectionUtils.getMethods(clazz$Registry, Optional.class, clazz$ResourceKey);
//        Method theMethod1 = null;
//        for (Method method : methods) {
//            Type returnType = method.getGenericReturnType();
//            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceKey) {
//                if (returnType instanceof ParameterizedType parameterizedType) {
//                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//                    if (actualTypeArguments.length == 1) {
//                        if (actualTypeArguments[0] instanceof ParameterizedType) {
//                            theMethod1 = method;
//                        }
//                    }
//                }
//            }
//        }
//        method$Registry$getHolder1 = theMethod1;
//    }

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
            ReflectionUtils.getMethod(clazz$IdMap, Object.class, int.class)
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

    public static final Method method$Registry$asHolderIdMap = requireNonNull(
            ReflectionUtils.getMethod(clazz$Registry, clazz$IdMap)
    );

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
    public static final Object[] instance$Direction$values;

    static {
        try {
            instance$Direction$values = (Object[]) method$Direction$values.invoke(null);
            instance$Direction$DOWN = instance$Direction$values[0];
            instance$Direction$UP = instance$Direction$values[1];
            instance$Direction$NORTH = instance$Direction$values[2];
            instance$Direction$SOUTH = instance$Direction$values[3];
            instance$Direction$WEST = instance$Direction$values[4];
            instance$Direction$EAST = instance$Direction$values[5];
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init Direction", e);
        }
    }

    public static final Method method$Vec3i$relative = requireNonNull(
            ReflectionUtils.getMethod(clazz$Vec3i, clazz$Vec3i, clazz$Direction)
    );

    public static final Method method$BlockPos$relative = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockPos, clazz$BlockPos, clazz$Direction)
    );

    public static final Class<?> clazz$ParticleType = requireNonNull(
            Optional.of(Objects.requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass("core.particles.Particle", "core.particles.ParticleType"))).map(it -> {
                if (it.getSuperclass() != Object.class) {
                    return it.getSuperclass();
                }
                return it;
            }).orElseThrow()
    );

    public static final Class<?> clazz$ParticleTypes = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.particles.Particles",
                    "core.particles.ParticleTypes"
            )
    );

    public static final Class<?> clazz$MappedRegistry = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryMaterials",
                    "core.MappedRegistry"
            )
    );

    public static final Field field$MappedRegistry$frozen = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MappedRegistry, boolean.class, 0)
    );

    public static final Method method$MappedRegistry$freeze = requireNonNull(
            ReflectionUtils.getMethod(clazz$MappedRegistry, clazz$Registry)
    );

    public static final Field field$MappedRegistry$byValue = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MappedRegistry, Map.class, 2)
    );

    public static final Field field$MappedRegistry$unregisteredIntrusiveHolders = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MappedRegistry, Map.class, 5)
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
            ReflectionUtils.getDeclaredMethod(clazz$Holder$Reference, void.class, Collection.class)
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
    
    public static final Field field$Holder$Reference$tags = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Holder$Reference, Set.class, 0)
    );

    public static final Class<?> clazz$TagKey = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("tags.TagKey"))
    );

    public static final Field field$TagKey$location = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$TagKey, clazz$ResourceLocation, 0)
    );

    public static final Method method$TagKey$create = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$TagKey, clazz$TagKey, clazz$ResourceKey, clazz$ResourceLocation)
    );

    public static final Field field$Direction$data3d = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Direction, int.class, 0)
    );

    public static final Field field$Holder$Reference$value = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Holder$Reference, Object.class, 0)
    );

    public static final Class<?> clazz$MutableBlockPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.BlockPosition$MutableBlockPosition",
                    "core.BlockPos$MutableBlockPos"
            )
    );

    public static final Constructor<?> constructor$MutableBlockPos = requireNonNull(
            ReflectionUtils.getConstructor(clazz$MutableBlockPos)
    );

    public static final Method method$MutableBlockPos$setWithOffset = requireNonNull(
            ReflectionUtils.getMethod(clazz$MutableBlockPos, clazz$MutableBlockPos, clazz$Vec3i, clazz$Direction)
    );

    public static final Method method$BlockPos$mutable = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockPos, clazz$MutableBlockPos)
    );

    public static final Class<?> clazz$NonNullList = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.NonNullList"))
    );

    public static final Method method$NonNullList$set = requireNonNull(
            ReflectionUtils.getMethod(clazz$NonNullList, Object.class, int.class, Object.class)
    );

    // 1.20.5+
    public static final Class<?> clazz$DataComponentPatch = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.DataComponentPatch")
    );

    public static final Class<?> clazz$DataComponentType = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.DataComponentType")
    );

    public static final Class<?> clazz$TypedDataComponent = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.TypedDataComponent")
    );

    // 1.21.5+
    public static final Class<?> clazz$DataComponentPredicate$Type = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.predicates.DataComponentPredicate$Type")
    ), VersionHelper.isOrAbove1_21_5());


    public static final Method method$Registry$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$Registry, int.class, Object.class)
    );

    public static final Class<?> clazz$Tag = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "nbt.NBTBase",
                    "nbt.Tag"
            )
    );

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

    public static final Class<?> clazz$SynchedEntityData = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcher",
                    "network.syncher.SynchedEntityData"
            )
    );

    // public static final Method method$SynchedEntityData$get = requireNonNull(
    //         ReflectionUtils.getDeclaredMethod(clazz$SynchedEntityData, Object.class, new String[]{"get", VersionHelper.isOrAbove1_20_5() ? "a" : "b"}, clazz$EntityDataAccessor)
    // );

    public static final Class<?> clazz$SynchedEntityData$DataValue = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    VersionHelper.isOrAbove1_20_5() ? "network.syncher.DataWatcher$c" : "network.syncher.DataWatcher$b",
                    "network.syncher.SynchedEntityData$DataValue"
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

    public static final Constructor<?> constructor$RegistryFriendlyByteBuf = Optional.ofNullable(clazz$RegistryFriendlyByteBuf)
            .map(it -> ReflectionUtils.getConstructor(it, 0))
            .orElse(null);

    public static final Constructor<?> constructor$FriendlyByteBuf = requireNonNull(
            ReflectionUtils.getConstructor(clazz$FriendlyByteBuf, ByteBuf.class)
    );

    public static final Method method$FriendlyByteBuf$writeByte = requireNonNull(
            ReflectionUtils.getMethod(clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, int.class)
    );

    public static final Method method$FriendlyByteBuf$writeLongArray = requireNonNull(
            ReflectionUtils.getMethod(clazz$FriendlyByteBuf, clazz$FriendlyByteBuf, long[].class)
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
            ReflectionUtils.getMethod(clazz$BossEvent$BossBarOverlay, new String[]{"valueOf"}, String.class)
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
            ReflectionUtils.getDeclaredField(clazz$EntityType, clazz$EntityType$EntityFactory, 0)
    );

    public static final Class<?> clazz$VoxelShape = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.phys.shapes.VoxelShape"))
    );

    public static final Class<?> clazz$Vec3 = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.Vec3D",
                    "world.phys.Vec3"
            )
    );

    public static final Constructor<?> constructor$Vec3 = requireNonNull(
            ReflectionUtils.getConstructor(clazz$Vec3, double.class, double.class, double.class)
    );

    public static final Field field$Vec3$Zero = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Vec3, clazz$Vec3, 0)
    );

    public static final Object instance$Vec3$Zero;

    static {
        try {
            instance$Vec3$Zero = field$Vec3$Zero.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get instance$Vec3$Zero", e);
        }
    }

    public static final Class<?> clazz$AttributeInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.AttributeModifiable",
                    "world.entity.ai.attributes.AttributeInstance"
            )
    );

    public static final Method method$AttributeInstance$getValue = requireNonNull(
            ReflectionUtils.getMethod(clazz$AttributeInstance, double.class, new String[]{"getValue", "f"})
    );


    public static final Class<?> clazz$AttributeModifier = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier")
            )
    );

    public static final Class<?> clazz$AttributeModifier$Operation = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier$Operation"))
    );

    public static final Method method$AttributeModifier$Operation$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$AttributeModifier$Operation, clazz$AttributeModifier$Operation.arrayType())
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
            !VersionHelper.isOrAbove1_20_5() ? ReflectionUtils.getConstructor(clazz$AttributeModifier, String.class, double.class, clazz$AttributeModifier$Operation) :
                    (!VersionHelper.isOrAbove1_21() ? ReflectionUtils.getConstructor(clazz$AttributeModifier, UUID.class, String.class, double.class, clazz$AttributeModifier$Operation) :
                            (ReflectionUtils.getConstructor(clazz$AttributeModifier, clazz$ResourceLocation, double.class, clazz$AttributeModifier$Operation)))
    );

    public static final Class<?> clazz$Attribute = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.AttributeBase",
                    "world.entity.ai.attributes.Attribute"
            )
    );

    public static final Field field$Attribute$id = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Attribute, String.class, 0)
    );

    public static final Field field$AttributeModifier$amount = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AttributeModifier, double.class, 0)
    );

    public static final Class<?> clazz$GameType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.EnumGamemode",
                    "world.level.GameType"
            )
    );

    public static final Method method$GameType$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$GameType, new String[] { "getId", "a" })
    );

    public static final Class<?> clazz$Biome = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.biome.BiomeBase",
                    "world.level.biome.Biome"
            )
    );

    public static final Class<?> clazz$Team$Visibility = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.scores.ScoreboardTeamBase$EnumTeamPush",
                    "world.scores.Team$Visibility"
            )
    );

    public static final Class<?> clazz$BlockState = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.IBlockData",
                    "world.level.block.state.BlockState"
            )
    );

    public static final Class<?> clazz$Block = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.block.Block"))
    );

    public static final Field field$BLOCK_STATE_REGISTRY = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Block, clazz$IdMapper, 0)
    );

    public static final Method method$IdMapper$add = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, void.class, new String[] {"add", "b"}, Object.class)
    );

    public static final Object instance$Block$BLOCK_STATE_REGISTRY;

    static {
        try {
            instance$Block$BLOCK_STATE_REGISTRY = field$BLOCK_STATE_REGISTRY.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$LevelAccessor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.GeneratorAccess",
                    "world.level.LevelAccessor"
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

    public static final Class<?> clazz$Palette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPalette",
                    "world.level.chunk.Palette"
            )
    );

    public static final Field field$PalettedContainer$data = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$PalettedContainer, clazz$PalettedContainer$Data, 0)
    );

    public static final VarHandle varHandle$PalettedContainer$data = requireNonNull(
            ReflectionUtils.findVarHandle(field$PalettedContainer$data)
    );

    public static final Field field$PalettedContainer$Data$storage = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$PalettedContainer$Data, clazz$BitStorage, 0)
    );

    public static final Field field$PalettedContainer$Data$palette = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$PalettedContainer$Data, clazz$Palette, 0)
    );

    // public static final Method method$Palette$write = requireNonNull(
    //         ReflectionUtils.getMethod(clazz$Palette, void.class, clazz$FriendlyByteBuf)
    // );

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

    public static final Method method$LevelChunkSection$setBlockState = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class, clazz$BlockState, boolean.class)
    );

    public static final Method method$LevelChunkSection$getBlockState = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class)
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
            ReflectionUtils.getStaticMethod(clazz$BlockBehaviour$Properties, clazz$BlockBehaviour$Properties)
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
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffect") // paper柠檬酸了
            )
    );

    public static final Class<?> clazz$MobEffectInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.effect.MobEffect",
                    "world.effect.MobEffectInstance"
            )
    );

    public static final Class<?> clazz$SoundType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.SoundEffectType",
                    "world.level.block.SoundType"
            )
    );

    public static final Constructor<?> constructor$SoundType = requireNonNull(
            ReflectionUtils.getConstructor(clazz$SoundType, float.class, float.class, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent, clazz$SoundEvent)
    );

    public static final Class<?> clazz$ItemLike = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IMaterial",
                    "world.level.ItemLike"
            )
    );

    public static final Class<?> clazz$Item = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.Item"))
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
                    BukkitReflectionUtils.assembleMCClass("world.level.material.Fluid") // paper柠檬酸了
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
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.levelgen.placement.PlacedFeature"))
    );

    // 1.21+
    public static final Class<?> clazz$JukeboxSong = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("world.item.JukeboxSong")
    );

    public static final Class<?> clazz$StateDefinition = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockStateList",
                    "world.level.block.state.StateDefinition"
            )
    );

    public static final Class<?> clazz$StateDefinition$Builder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockStateList$a",
                    "world.level.block.state.StateDefinition$Builder"
            )
    );

    public static final Class<?> clazz$StateDefinition$Factory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockStateList$b",
                    "world.level.block.state.StateDefinition$Factory"
            )
    );

    public static final Class<?> clazz$ServerLevel = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.WorldServer",
                    "server.level.ServerLevel"
            )
    );

    public static final Class<?> clazz$Explosion = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.Explosion"))
    );

    public static final Constructor<?> constructor$StateDefinition$Builder = requireNonNull(
            ReflectionUtils.getTheOnlyConstructor(clazz$StateDefinition$Builder)
    );

    public static final Method method$StateDefinition$Builder$create = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateDefinition$Builder, clazz$StateDefinition, Function.class, clazz$StateDefinition$Factory)
    );

    public static final Field field$Block$StateDefinition = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Block, clazz$StateDefinition, 0)
    );

    public static final Field field$StateDefinition$states = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$StateDefinition, ImmutableList.class, 0)
    );

    public static final Class<?> clazz$MapColor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.MaterialMapColor",
                    "world.level.material.MapColor"
            )
    );

    public static final Method method$MapColor$byId = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$MapColor, clazz$MapColor, int.class)
    );

    public static final Field field$MapColor$id = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MapColor, int.class, 1)
    );

    public static final Class<?> clazz$PushReaction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.EnumPistonReaction",
                    "world.level.material.PushReaction"
            )
    );

    public static final Method method$PushReaction$values = requireNonNull(
            ReflectionUtils.getMethod(clazz$PushReaction, new String[] { "values" })
    );

    public static final Object[] instance$PushReaction$values;

    static {
        try {
            instance$PushReaction$values = (Object[]) method$PushReaction$values.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init PushReaction", e);
        }
    }

    public static final Class<?> clazz$NoteBlockInstrument = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockPropertyInstrument",
                    "world.level.block.state.properties.NoteBlockInstrument"
            )
    );

    public static final Method method$NoteBlockInstrument$values = requireNonNull(
            ReflectionUtils.getMethod(clazz$NoteBlockInstrument, new String[] { "values" })
    );

    public static final Object[] instance$NoteBlockInstrument$values;

    static {
        try {
            instance$NoteBlockInstrument$values = (Object[]) method$NoteBlockInstrument$values.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init NoteBlockInstrument", e);
        }
    }

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
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$BlockStateBase$Cache, 0)
    );

    // 1.20-1.21.1
    public static final Field field$BlockStateBase$Cache$lightBlock =
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockStateBase$Cache, int.class, 0);

    public static final Method method$BlockStateBase$initCache = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, void.class, new String[] { "initCache", "a" })
    );

    public static final Field field$BlockStateBase$isRedstoneConductor = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$StatePredicate, 0)
    );

    public static final Field field$BlockStateBase$isSuffocating = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$StatePredicate, 1)
    );

    public static final Field field$BlockStateBase$isViewBlocking = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$StatePredicate, 2)
    );

    public static final Field field$BlockStateBase$fluidState = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$FluidState, 0)
    );

    public static final Field field$BlockStateBase$pushReaction = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$PushReaction, 0)
    );

    public static final Field field$BlockStateBase$mapColor = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$MapColor, 0)
    );

    public static final Field field$BlockStateBase$instrument = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, clazz$NoteBlockInstrument, 0)
    );

    public static final Field field$BlockStateBase$hardness = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, float.class, 0)
    );

    public static final Field field$BlockStateBase$useShapeForLightOcclusion = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 0)
    );

    public static final Field field$BlockStateBase$burnable = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 2)
    );

    public static final Field field$BlockStateBase$isRandomlyTicking = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 9)
    );

    // 1.21.2以前用
    public static final Field field$BlockStateBase$isConditionallyFullOpaque = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, VersionHelper.isOrAbove1_21() ? 10 : 11)
    );

    // 1.21.2+，其他版本在cache里
    public static final Field field$BlockStateBase$propagatesSkylightDown = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 11)
    );

    public static final Field field$BlockStateBase$Cache$propagatesSkylightDown = ReflectionUtils.getDeclaredField(
            clazz$BlockStateBase$Cache, boolean.class, 2
    );

    public static final Field field$BlockStateBase$requiresCorrectToolForDrops = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 5)
    );

    public static final Field field$BlockStateBase$canOcclude = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 6)
    );

    public static final Field field$BlockStateBase$replaceable = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, boolean.class, 8)
    );

    public static final Field field$BlockStateBase$lightEmission = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockStateBase, int.class, 0)
    );

    // 1.21.2+
    public static final Field field$BlockStateBase$lightBlock =
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockStateBase, int.class, 1);

    // 1.20-1.21.1
    public static final Field field$BlockStateBase$opacityIfCached =
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockStateBase, int.class, 1);

    public static final Class<?> clazz$AABB = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.AxisAlignedBB",
                    "world.phys.AABB"
            )
    );

    public static final Field field$AABB$minX = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 0)
    );

    public static final Field field$AABB$minY = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 1)
    );

    public static final Field field$AABB$minZ = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 2)
    );

    public static final Field field$AABB$maxX = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 3)
    );

    public static final Field field$AABB$maxY = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 4)
    );

    public static final Field field$AABB$maxZ = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$AABB, double.class, 5)
    );

    public static final Method method$Block$box = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Block, clazz$VoxelShape, double.class, double.class, double.class, double.class, double.class, double.class)
    );

    public static final Constructor<?> constructor$AABB = requireNonNull(
            ReflectionUtils.getConstructor(clazz$AABB, double.class, double.class, double.class, double.class, double.class, double.class)
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

    public static final Class<?> clazz$CollisionContext = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.shapes.VoxelShapeCollision",
                    "world.phys.shapes.CollisionContext"
            )
    );

    public static final Class<?> clazz$PathComputationType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.pathfinder.PathMode",
                    "world.level.pathfinder.PathComputationType"
            )
    );

    public static final Method method$PathComputationType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$PathComputationType, clazz$PathComputationType.arrayType())
    );

    public static final Object instance$PathComputationType$LAND;
    public static final Object instance$PathComputationType$WATER;
    public static final Object instance$PathComputationType$AIR;

    static {
        try {
            Object[] objs = (Object[]) method$PathComputationType$values.invoke(null);
            instance$PathComputationType$LAND = objs[0];
            instance$PathComputationType$WATER = objs[1];
            instance$PathComputationType$AIR = objs[2];
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize PathComputationType", e);
        }
    }

    public static final Method method$BlockBehaviour$isPathFindable = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$PathComputationType) :
            ReflectionUtils.getMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$PathComputationType)
    );

    public static final Method method$BlockBehaviour$getShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getShape", "a"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext)
    );

    public static final Method method$BlockBehaviour$getCollisionShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getCollisionShape", VersionHelper.isOrAbove1_20_3() ? "b" : "c"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext)
    );

    public static final Method method$BlockBehaviour$getBlockSupportShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getBlockSupportShape", "b_"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos)
    );

    public static final Field field$BlockBehaviour$properties = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, clazz$BlockBehaviour$Properties, 0)
    );

    public static final Field field$BlockBehaviour$explosionResistance = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, float.class, 0)
    );

    public static final Field field$BlockBehaviour$friction = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, float.class, 1)
    );

    public static final Field field$BlockBehaviour$speedFactor = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, float.class, 2)
    );

    public static final Field field$BlockBehaviour$jumpFactor = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, float.class, 3)
    );

    public static final Field field$BlockBehaviour$soundType = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour, clazz$SoundType, 0)
    );

    public static final Field field$SoundType$breakSound = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SoundType, clazz$SoundEvent, 0)
    );

    public static final Field field$SoundType$stepSound = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SoundType, clazz$SoundEvent, 1)
    );

    public static final Field field$SoundType$placeSound = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SoundType, clazz$SoundEvent, 2)
    );

    public static final Field field$SoundType$hitSound = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SoundType, clazz$SoundEvent, 3)
    );

    public static final Field field$SoundType$fallSound = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SoundType, clazz$SoundEvent, 4)
    );

    public static final Field field$BlockBehaviour$Properties$hasCollision = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$BlockBehaviour$Properties, boolean.class, 0)
    );

    public static final Class<?> clazz$ChunkPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.ChunkCoordIntPair",
                    "world.level.ChunkPos"
            )
    );

    public static final Constructor<?> constructor$ChunkPos = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ChunkPos, int.class, int.class)
    );

    public static final Class<?> clazz$LevelLightEngine = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.lighting.LevelLightEngine"))
    );

    public static final Class<?> clazz$LightLayer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.EnumSkyBlock",
                    "world.level.LightLayer"
            )
    );

    public static final Method method$LightLayer$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$LightLayer, clazz$LightLayer.arrayType())
    );

    public static final Object instance$LightLayer$BLOCK;

    static {
        try {
            instance$LightLayer$BLOCK = ((Object[]) method$LightLayer$values.invoke(null))[1];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$Player = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.EntityHuman",
                    "world.entity.player.Player"
            )
    );

    public static final Class<?> clazz$Entity = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.Entity"))
    );

    public static final Field field$Entity$ENTITY_COUNTER = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Entity, AtomicInteger.class, 0)
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

    public static final Method method$BlockBehaviour$hasAnalogOutputSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, new String[]{"hasAnalogOutputSignal",
            VersionHelper.isOrAbove1_20_5() ? "c_" : "d_"}, clazz$BlockState)
    );

    public static final Method method$BlockBehaviour$getAnalogOutputSignal = requireNonNull(
            VersionHelper.isOrAbove1_21_9()
                    ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getAnalogOutputSignal", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Direction)
                    : ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getAnalogOutputSignal", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos)
    );

    public static final Method method$Entity$level = requireNonNull(
            ReflectionUtils.getMethod(clazz$Entity, clazz$Level)
    );

    public static final Class<?> clazz$InteractionHand = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.EnumHand",
                    "world.InteractionHand"
            )
    );

    public static final Method method$InteractionHand$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$InteractionHand, clazz$InteractionHand.arrayType())
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

    public static final Object[] instance$EquipmentSlot$values;
    public static final Object instance$EquipmentSlot$MAINHAND;
    public static final Object instance$EquipmentSlot$OFFHAND;
    public static final Object instance$EquipmentSlot$FEET;
    public static final Object instance$EquipmentSlot$LEGS;
    public static final Object instance$EquipmentSlot$CHEST;
    public static final Object instance$EquipmentSlot$HEAD;
    public static final Object instance$EquipmentSlot$BODY;
    public static final Object instance$EquipmentSlot$SADDLE;

    static {
        try {
            instance$EquipmentSlot$values = (Object[]) method$EquipmentSlot$values.invoke(null);
            instance$EquipmentSlot$MAINHAND = instance$EquipmentSlot$values[0];
            instance$EquipmentSlot$OFFHAND = instance$EquipmentSlot$values[1];
            instance$EquipmentSlot$FEET = instance$EquipmentSlot$values[2];
            instance$EquipmentSlot$LEGS = instance$EquipmentSlot$values[3];
            instance$EquipmentSlot$CHEST = instance$EquipmentSlot$values[4];
            instance$EquipmentSlot$HEAD = instance$EquipmentSlot$values[5];
            if (VersionHelper.isOrAbove1_20_5()) {
                instance$EquipmentSlot$BODY = instance$EquipmentSlot$values[6];
            } else {
                instance$EquipmentSlot$BODY = null;
            }
            if (VersionHelper.isOrAbove1_21_5()) {
                instance$EquipmentSlot$SADDLE = instance$EquipmentSlot$values[7];
            } else {
                instance$EquipmentSlot$SADDLE = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init EquipmentSlot", e);
        }
    }

    public static final Field field$Block$defaultBlockState = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Block, clazz$BlockState, 0)
    );

    public static final Method method$Entity$getOnPos = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Entity, clazz$BlockPos, float.class)
    );

    public static final Class<?> clazz$ItemStack = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.ItemStack"))
    );

    public static final Object instance$ItemStack$EMPTY;

    static {
        try {
            instance$ItemStack$EMPTY = requireNonNull(ReflectionUtils.getDeclaredField(clazz$ItemStack, clazz$ItemStack, 0)).get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$ItemEnchantments =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass( "world.item.enchantment.ItemEnchantments"));

    public static final Field field$ItemEnchantments$enchantments = Optional.ofNullable(clazz$ItemEnchantments)
            .map(it -> ReflectionUtils.getInstanceDeclaredField(it, 0))
            .orElse(null);

    // 1.21.3+
    public static final Class<?> clazz$ScheduledTickAccess =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.ScheduledTickAccess"));

    public static final Method method$BlockBehaviour$updateShape = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$LevelReader, clazz$ScheduledTickAccess, clazz$BlockPos, clazz$Direction, clazz$BlockPos, clazz$BlockState, clazz$RandomSource) :
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Direction, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockPos)
    );

    public static final Method method$BlockBehaviour$canSurvive = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$LevelReader, clazz$BlockPos)
    );

    public static final Method method$Block$stepOn = requireNonNull(
            ReflectionUtils.getMethod(clazz$Block, void.class, new String[] {"stepOn", "a"}, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$Entity)
    );

    public static final Method method$BlockBehaviour$onExplosionHit = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, VersionHelper.isOrAbove1_21_2() ? clazz$ServerLevel : clazz$Level, clazz$BlockPos, clazz$Explosion, BiConsumer.class),
            VersionHelper.isOrAbove1_21()
    );

    public static final Class<?> clazz$Fallable = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.block.Fallable"))
    );

    public static final Class<?> clazz$FallingBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFalling",
                    "world.level.block.FallingBlock"
            )
    );

    public static final Method method$FallingBlock$isFree = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$FallingBlock, boolean.class, clazz$BlockState)
    );

    public static final Class<?> clazz$FallingBlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.item.EntityFallingBlock",
                    "world.entity.item.FallingBlockEntity"
            )
    );

    public static final Method method$Fallable$onLand = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fallable, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$BlockState, clazz$FallingBlockEntity)
    );

    public static final Method method$Fallable$onBrokenAfterFall = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fallable, void.class, clazz$Level, clazz$BlockPos, clazz$FallingBlockEntity)
    );

    public static final Method method$FallingBlockEntity$fall = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$FallingBlockEntity, clazz$FallingBlockEntity, clazz$Level, clazz$BlockPos, clazz$BlockState)
    );

    public static final Method method$FallingBlockEntity$setHurtsEntities = requireNonNull(
            ReflectionUtils.getMethod(clazz$FallingBlockEntity, void.class, float.class, int.class)
    );

    public static final Field field$FallingBlockEntity$blockState = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$FallingBlockEntity, clazz$BlockState, VersionHelper.isOrAbove1_21_5() ? 1 : 0)
    );

    public static final Field field$FallingBlockEntity$cancelDrop = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$FallingBlockEntity, boolean.class, 1)
    );

    public static final Field field$Entity$xo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Entity, double.class, 0)
    );

    public static final Field field$Entity$yo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Entity, double.class, 1)
    );

    public static final Field field$Entity$zo = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Entity, double.class, 2)
    );

    public static final Method method$Level$removeBlock = requireNonNull(
            ReflectionUtils.getMethod(clazz$Level, boolean.class, clazz$BlockPos, boolean.class)
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

    public static final Field field$Property$name = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Property, String.class, 0)
    );

    public static final Field field$LeavesBlock$DISTANCE = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$LeavesBlock, clazz$IntegerProperty, 0)
    );

    public static final Method method$StateHolder$hasProperty = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, boolean.class, clazz$Property)
    );

    public static final Method method$StateHolder$getValue = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, Object.class, new String[] {"getValue", "c"}, clazz$Property)
    );

    public static final Method method$StateHolder$setValue = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, Object.class, new String[] {"setValue", VersionHelper.isOrAbove1_21_2() ? "b" : "a"}, clazz$Property, Comparable.class)
    );

    public static final Method method$Block$updateFromNeighbourShapes = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Block, clazz$BlockState, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos)
    );

    public static final Method method$BlockStateBase$updateNeighbourShapes = requireNonNull(
            ReflectionUtils.getMethod(
                    // flags   // depth
                    clazz$BlockStateBase, void.class, clazz$LevelAccessor, clazz$BlockPos, int.class, int.class
            )
    );

    public static final Method method$BlockState$getShape = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, clazz$VoxelShape, new String[]{"getShape", "a"}, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext)
    );

    public static final Method method$VoxelShape$isEmpty = requireNonNull(
            ReflectionUtils.getMethod(clazz$VoxelShape, boolean.class)
    );

    public static final Method method$VoxelShape$bounds = requireNonNull(
            ReflectionUtils.getMethod(clazz$VoxelShape, clazz$AABB)
    );

    public static final Method method$LevelWriter$setBlock = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelWriter, boolean.class, clazz$BlockPos, clazz$BlockState, int.class)
    );

    public static final Method method$CollisionContext$of = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CollisionContext, clazz$CollisionContext, clazz$Entity)
    );

    public static final Method method$CollisionContext$empty = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CollisionContext, clazz$CollisionContext)
    );

    public static final Method method$BlockStateBase$canSurvive = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, boolean.class, clazz$LevelReader, clazz$BlockPos)
    );

    public static final Method method$BlockStateBase$onPlace = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class)
    );

    public static final Method method$ItemStack$isTag = requireNonNull(
            ReflectionUtils.getMethod(clazz$ItemStack, boolean.class, clazz$TagKey)
    );

    public static final Class<?> clazz$FireBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFire",
                    "world.level.block.FireBlock"
            )
    );

    public static final Method method$FireBlock$setFlammable = requireNonNull(
            Optional.ofNullable(ReflectionUtils.getMethod(clazz$FireBlock, void.class, clazz$Block, int.class, int.class))
                    .orElse(ReflectionUtils.getDeclaredMethod(clazz$FireBlock, void.class, clazz$Block, int.class, int.class))
    );

    public static final Field field$LevelChunkSection$states = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$LevelChunkSection, clazz$PalettedContainer, 0)
    );

    public static final Constructor<?> constructor$ItemStack = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ItemStack, clazz$ItemLike)
    );

    public static final Class<?> clazz$Display$ItemDisplay = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.Display$ItemDisplay"))
    );

    public static final Class<?> clazz$Abilities = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.PlayerAbilities",
                    "world.entity.player.Abilities"
            )
    );

    public static final Field field$Abilities$invulnerable = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Abilities, boolean.class, 0)
    );

    public static final Field field$Abilities$flying = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Abilities, boolean.class, 1)
    );

    public static final Field field$Abilities$mayfly = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Abilities, boolean.class, 2)
    );

    public static final Field field$Abilities$mayBuild = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Abilities, boolean.class, 4)
    );

    public static final Class<?> clazz$FlowingFluid = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.FluidTypeFlowing",
                    "world.level.material.FlowingFluid"
            )
    );

    public static final Method method$FlowingFluid$getSource = requireNonNull(
            ReflectionUtils.getMethod(clazz$FlowingFluid, clazz$FluidState, boolean.class)
    );

    public static final Method method$FluidState$isSource = requireNonNull(
            ReflectionUtils.getMethod(clazz$FluidState, boolean.class, new String[]{"isSource", "b"})
    );

    public static final Method method$FluidState$createLegacyBlock = requireNonNull(
            ReflectionUtils.getMethod(clazz$FluidState, clazz$BlockState)
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
            ReflectionUtils.getMethod(clazz$RecipeManager, new String[]{"finalizeRecipeLoading"});

    public static final Class<?> clazz$RecipeMap =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeMap"));

    public static final Field field$RecipeManager$recipes = Optional.ofNullable(clazz$RecipeMap)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$RecipeManager, it, 0))
            .orElse(null);

    public static final Method method$RecipeMap$removeRecipe = Optional.ofNullable(clazz$RecipeMap)
            .map(it -> ReflectionUtils.getMethod(it, boolean.class, clazz$ResourceKey))
            .orElse(null);

    public static final Class<?> clazz$FeatureFlagSet = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.flag.FeatureFlagSet"))
    );

    public static final Field field$RecipeManager$featureflagset =
            ReflectionUtils.getDeclaredField(clazz$RecipeManager, clazz$FeatureFlagSet, 0);

    public static final Class<?> clazz$Inventory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.PlayerInventory",
                    "world.entity.player.Inventory"
            )
    );

    public static final Field field$Inventory$items = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Inventory, clazz$NonNullList, 0)
    );

    public static final Class<?> clazz$Ingredient = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeItemStack",
                    "world.item.crafting.Ingredient"
            )
    );

    public static final Field field$Ingredient$itemStacks = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient,
                    VersionHelper.isOrAbove1_21_4() ? Set.class : VersionHelper.isOrAbove1_21_2() ? List.class : clazz$ItemStack.arrayType(),
                    VersionHelper.isOrAbove1_21_4() ? 0 : VersionHelper.isOrAbove1_21_2() ? 1 : 0
            )
    );

    public static final MethodHandle methodHandle$RecipeManager$finalizeRecipeLoading;
    public static final MethodHandle methodHandle$RecipeManager$featureflagsetGetter;
    public static final MethodHandle methodHandle$RecipeManager$featureflagsetSetter;
    public static final MethodHandle methodHandle$Ingredient$itemStacksSetter;

    static {
        try {
            if (method$RecipeManager$finalizeRecipeLoading != null) {
                methodHandle$RecipeManager$finalizeRecipeLoading = ReflectionUtils.unreflectMethod(method$RecipeManager$finalizeRecipeLoading)
                        .asType(MethodType.methodType(void.class, Object.class));
            } else {
                methodHandle$RecipeManager$finalizeRecipeLoading = null;
            }
            if (field$RecipeManager$featureflagset != null) {
                methodHandle$RecipeManager$featureflagsetGetter = ReflectionUtils.unreflectGetter(field$RecipeManager$featureflagset)
                        .asType(MethodType.methodType(Object.class, Object.class));
                methodHandle$RecipeManager$featureflagsetSetter = ReflectionUtils.unreflectSetter(field$RecipeManager$featureflagset)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$RecipeManager$featureflagsetGetter = null;
                methodHandle$RecipeManager$featureflagsetSetter = null;
            }
            methodHandle$Ingredient$itemStacksSetter = ReflectionUtils.unreflectSetter(field$Ingredient$itemStacks)
                    .asType(MethodType.methodType(void.class, Object.class, VersionHelper.isOrAbove1_21_4() ? Set.class : VersionHelper.isOrAbove1_21_2() ? List.class : Object.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize CoreReflections", e);
        }
    }

    // Since 1.21.2, exact has been removed
    public static final Field field$Ingredient$exact =
            ReflectionUtils.getDeclaredField(clazz$Ingredient, boolean.class, 0);

    public static final Class<?> clazz$ShapedRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.ShapedRecipes",
                    "world.item.crafting.ShapedRecipe"
            )
    );

    // 1.20.3+
    public static final Class<?> clazz$ShapedRecipePattern =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapedRecipePattern"));

    // 1.20.1-1.20.2
    public static final Field field$1_20_1$ShapedRecipe$recipeItems =
            ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, clazz$NonNullList, 0);

    // 1.20.3+
    public static final Field field$1_20_3$ShapedRecipe$pattern=
            ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, clazz$ShapedRecipePattern, 0);

    // 1.20.3-1.21.1
    public static final Field field$ShapedRecipePattern$ingredients1_20_3 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$NonNullList, 0))
            .orElse(null);

    // 1.21.2+
    public static final Field field$ShapedRecipePattern$ingredients1_21_2 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, List.class, 0))
            .orElse(null);

    // 1.20.1-1.21.1
    public static final Field field$Ingredient$values =
            ReflectionUtils.getInstanceDeclaredField(clazz$Ingredient, 0);

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
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.PlacementInfo"));

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

    public static final MethodHandle methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter;
    public static final MethodHandle methodHandle$1_20_3$ShapedRecipe$patternGetter;
    public static final MethodHandle methodHandle$ShapedRecipePattern$ingredients1_20_3Getter;
    public static final MethodHandle methodHandle$ShapedRecipePattern$ingredients1_21_2Getter;
    public static final MethodHandle methodHandle$Ingredient$valuesGetter;
    public static final MethodHandle methodHandle$ShapelessRecipe$placementInfoSetter;
    public static final MethodHandle methodHandle$ShapedRecipe$placementInfoSetter;
    public static final MethodHandle methodHandle$ShapelessRecipe$ingredientsGetter;

    static {
        try {
            if (field$1_20_1$ShapedRecipe$recipeItems != null) {
                methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter = ReflectionUtils.unreflectGetter(field$1_20_1$ShapedRecipe$recipeItems)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter = null;
            }
            if (field$1_20_3$ShapedRecipe$pattern != null) {
                methodHandle$1_20_3$ShapedRecipe$patternGetter = ReflectionUtils.unreflectGetter(field$1_20_3$ShapedRecipe$pattern)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$1_20_3$ShapedRecipe$patternGetter = null;
            }
            if (field$ShapedRecipePattern$ingredients1_20_3 != null) {
                methodHandle$ShapedRecipePattern$ingredients1_20_3Getter = ReflectionUtils.unreflectGetter(field$ShapedRecipePattern$ingredients1_20_3)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapedRecipePattern$ingredients1_20_3Getter = null;
            }
            if (field$ShapedRecipePattern$ingredients1_21_2 != null) {
                methodHandle$ShapedRecipePattern$ingredients1_21_2Getter = ReflectionUtils.unreflectGetter(field$ShapedRecipePattern$ingredients1_21_2)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapedRecipePattern$ingredients1_21_2Getter = null;
            }
            if (field$Ingredient$values != null) {
                methodHandle$Ingredient$valuesGetter = ReflectionUtils.unreflectGetter(field$Ingredient$values)
                        .asType(MethodType.methodType(Object[].class, Object.class));
            } else {
                methodHandle$Ingredient$valuesGetter = null;
            }
            if (field$ShapelessRecipe$placementInfo != null) {
                methodHandle$ShapelessRecipe$placementInfoSetter = ReflectionUtils.unreflectSetter(field$ShapelessRecipe$placementInfo)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$ShapelessRecipe$placementInfoSetter = null;
            }
            if (field$ShapedRecipe$placementInfo != null) {
                methodHandle$ShapedRecipe$placementInfoSetter = ReflectionUtils.unreflectSetter(field$ShapedRecipe$placementInfo)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$ShapedRecipe$placementInfoSetter = null;
            }
            if (field$ShapelessRecipe$ingredients != null) {
                methodHandle$ShapelessRecipe$ingredientsGetter = ReflectionUtils.unreflectGetter(field$ShapelessRecipe$ingredients)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapelessRecipe$ingredientsGetter = null;
            }
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize CoreReflections", e);
        }
    }

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

    public static final Field field$ResultContainer$recipeUsed =
            Optional.ofNullable(ReflectionUtils.getDeclaredField(clazz$ResultContainer, clazz$Recipe, 0))
                    .orElse(ReflectionUtils.getDeclaredField(clazz$ResultContainer, clazz$RecipeHolder, 0));

    public static final Class<?> clazz$LivingEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityLiving",
                    "world.entity.LivingEntity"
            )
    );

    // 1.20.5+
    public static final Method method$ItemStack$hurtAndBreak =
            ReflectionUtils.getMethod(clazz$ItemStack, void.class, int.class, clazz$LivingEntity, clazz$EquipmentSlot);

    // for 1.20.1-1.21.1
    public static final Class<?> clazz$AbstractCookingRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeCooking",
                    "world.item.crafting.AbstractCookingRecipe"
            )
    );

    // for 1.20.1-1.21.1
    public static final Field field$AbstractCookingRecipe$input =
            ReflectionUtils.getDeclaredField(clazz$AbstractCookingRecipe, clazz$Ingredient, 0);

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

    public static final MethodHandle methodHandle$AbstractCookingRecipe$inputGetter;
    public static final MethodHandle methodHandle$SingleItemRecipe$inputGetter;

    static {
        try {
            if (field$AbstractCookingRecipe$input != null) {
                methodHandle$AbstractCookingRecipe$inputGetter = ReflectionUtils.unreflectGetter(field$AbstractCookingRecipe$input)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$AbstractCookingRecipe$inputGetter = null;
            }
            if (field$SingleItemRecipe$input != null) {
                methodHandle$SingleItemRecipe$inputGetter = ReflectionUtils.unreflectGetter(field$SingleItemRecipe$input)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$SingleItemRecipe$inputGetter = null;
            }
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize methodHandle$SingleItemRecipe$inputGetter", e);
        }
    }

    public static final Field field$AbstractFurnaceBlockEntity$quickCheck = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractFurnaceBlockEntity, clazz$RecipeManager$CachedCheck, 0)
    );

    // 1.20.1-1.21.1
    public static final Field field$CampfireBlockEntity$quickCheck =
            ReflectionUtils.getDeclaredField(clazz$CampfireBlockEntity, clazz$RecipeManager$CachedCheck, 0);

    // 1.21+
    public static final Class<?> clazz$RecipeInput =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.RecipeInput"));

    public static final Class<?> clazz$SingleRecipeInput =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.SingleRecipeInput"));

    public static final Constructor<?> constructor$SingleRecipeInput = Optional.ofNullable(clazz$SingleRecipeInput)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$ItemStack))
            .orElse(null);

    // 1.21+
    public static final Field field$SingleRecipeInput$item = Optional.ofNullable(clazz$SingleRecipeInput)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$ItemStack, 0))
            .orElse(null);

    public static final Field field$AbstractFurnaceBlockEntity$items = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractFurnaceBlockEntity, clazz$NonNullList, 0)
    );

    public static final Class<?> clazz$SimpleContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.InventorySubcontainer",
                    "world.SimpleContainer"
            )
    );

    public static final Field field$SimpleContainer$items = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SimpleContainer, clazz$NonNullList, 0)
    );

    public static final Method method$LevelReader$getMaxLocalRawBrightness = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelReader, int.class, clazz$BlockPos)
    );

    public static final Method method$ConfiguredFeature$place = requireNonNull(
            ReflectionUtils.getMethod(clazz$ConfiguredFeature, boolean.class, clazz$WorldGenLevel, clazz$ChunkGenerator, clazz$RandomSource, clazz$BlockPos)
    );

    public static final Method method$PlacedFeature$place = requireNonNull(
            ReflectionUtils.getMethod(clazz$PlacedFeature, boolean.class, clazz$WorldGenLevel, clazz$ChunkGenerator, clazz$RandomSource, clazz$BlockPos)
    );

    public static final Class<?> clazz$BonemealableBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.IBlockFragilePlantElement",
                    "world.level.block.BonemealableBlock"
            )
    );

    public static final Class<?> clazz$WorldlyContainerHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.IInventoryHolder",
                    "world.WorldlyContainerHolder"
            )
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getInstanceMethod(clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState) :
                    ReflectionUtils.getInstanceMethod(clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$WorldlyContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.IWorldInventory",
                    "world.WorldlyContainer"
            )
    );


    public static final Method method$WorldlyContainerHolder$getContainer = requireNonNull(
            ReflectionUtils.getMethod(clazz$WorldlyContainerHolder, clazz$WorldlyContainer, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos)
    );
    public static final Method method$BonemealableBlock$isBonemealSuccess = requireNonNull(
            ReflectionUtils.getMethod(clazz$BonemealableBlock, boolean.class, clazz$Level, clazz$RandomSource, clazz$BlockPos, clazz$BlockState)
    );
    
    public static final Method method$PalettedContainer$getAndSet = Objects.requireNonNull(
            ReflectionUtils.getMethod(clazz$PalettedContainer, Object.class, new String[] {"a", "getAndSet"}, int.class, int.class, int.class, Object.class)
    );

    public static final Class<?> clazz$MenuType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Containers",
                    "world.inventory.MenuType"
            )
    );

    public static final Class<?> clazz$AbstractContainerMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Container",
                    "world.inventory.AbstractContainerMenu"
            )
    );

    public static final Field field$AbstractContainerMenu$title = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractContainerMenu, clazz$Component, 0)
    );

    public static final Field field$Player$containerMenu = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Player, clazz$AbstractContainerMenu, 0)
    );

    public static final Field field$AbstractContainerMenu$containerId = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractContainerMenu, int.class, 1)
    );

    public static final Field field$AbstractContainerMenu$menuType = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractContainerMenu, clazz$MenuType, 0)
    );

    public static final Method method$AbstractContainerMenu$broadcastChanges = requireNonNull(
            ReflectionUtils.getMethod(clazz$AbstractContainerMenu, void.class, new String[]{ "broadcastChanges", "d" })
    );

    public static final Method method$AbstractContainerMenu$broadcastFullState = requireNonNull(
            ReflectionUtils.getMethod(clazz$AbstractContainerMenu, void.class, new String[]{ "broadcastFullState", "e" })
    );

    public static final Field field$AbstractContainerMenu$checkReachable = requireNonNull(
            ReflectionUtils.getDeclaredFieldBackwards(clazz$AbstractContainerMenu, boolean.class, 0)
    );

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

    public static final Class<?> clazz$FireworkRocketRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeFireworks",
                    "world.item.crafting.FireworkRocketRecipe"
            )
    );

    public static final Class<?> clazz$FireworkStarRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeFireworksStar",
                    "world.item.crafting.FireworkStarRecipe"
            )
    );

    public static final Class<?> clazz$FireworkStarFadeRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeFireworksFade",
                    "world.item.crafting.FireworkStarFadeRecipe"
            )
    );

    public static final Class<?> clazz$AnvilMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.ContainerAnvil",
                    "world.inventory.AnvilMenu"
            )
    );

    public static final Class<?> clazz$SmithingTransformRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.SmithingTransformRecipe"))
    );

    // 1.21.5+
    public static final Class<?> clazz$TransmuteResult =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.TransmuteResult"));

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

    public static final Class<?> clazz$SmithingTrimRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.SmithingTrimRecipe"))
    );

    public static final Constructor<?> constructor$SmithingTrimRecipe = requireNonNull(
            VersionHelper.isOrAbove1_21_5() ?
            ReflectionUtils.getConstructor(clazz$SmithingTrimRecipe, clazz$Ingredient, clazz$Ingredient, clazz$Ingredient, clazz$Holder) :
            VersionHelper.isOrAbove1_21_2() ?
            ReflectionUtils.getConstructor(clazz$SmithingTrimRecipe, Optional.class, Optional.class, Optional.class) :
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getConstructor(clazz$SmithingTrimRecipe, clazz$Ingredient, clazz$Ingredient, clazz$Ingredient) :
            ReflectionUtils.getConstructor(clazz$SmithingTrimRecipe, clazz$ResourceLocation, clazz$Ingredient, clazz$Ingredient, clazz$Ingredient)
    );

    public static final Method method$RecipeManager$addRecipe = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getMethod(clazz$RecipeManager, void.class, clazz$RecipeHolder) :
                    ReflectionUtils.getMethod(clazz$RecipeManager, void.class, clazz$Recipe)
    );

    public static final Method method$ItemStack$getItem = requireNonNull(
            ReflectionUtils.getMethod(clazz$ItemStack, clazz$Item)
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
            ReflectionUtils.getStaticMethod(clazz$ClipContext$Fluid, clazz$ClipContext$Fluid.arrayType())
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
            ReflectionUtils.getDeclaredMethod(clazz$Item, clazz$BlockHitResult, clazz$Level, clazz$Player, clazz$ClipContext$Fluid)
    );

    public static final Method method$BlockHitResult$withPosition = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockHitResult, clazz$BlockHitResult, clazz$BlockPos)
    );

    public static final Field field$BlockHitResult$blockPos = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockHitResult, clazz$BlockPos, 0)
    );

    public static final Field field$BlockHitResult$direction = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockHitResult, clazz$Direction, 0)
    );

    public static final Field field$BlockHitResult$miss = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockHitResult, boolean.class, 0)
    );

    public static final Field field$BlockHitResult$inside = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockHitResult, boolean.class, 1)
    );

    public static final Class<?> clazz$HitResult = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.MovingObjectPosition",
                    "world.phys.HitResult"
            )
    );

    public static final Field field$HitResult$location = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$HitResult, clazz$Vec3, 0)
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
            ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState, clazz$FluidState)
    );

    public static final Method method$SimpleWaterloggedBlock$pickupBlock = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LivingEntity, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                    : VersionHelper.isOrAbove1_20_2()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$Player, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                    : ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
    );

    public static final Method method$Fluid$getTickDelay = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fluid, int.class, clazz$LevelReader)
    );

    public static final Method method$Fluid$defaultFluidState = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fluid, clazz$FluidState, 0)
    );

    public static final Class<?> clazz$SingleValuePalette = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.chunk.SingleValuePalette"))
    );

    public static final Field field$SingleValuePalette$value = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$SingleValuePalette, Object.class, 0)
    );

    public static final Class<?> clazz$HashMapPalette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteHash",
                    "world.level.chunk.HashMapPalette"
            )
    );

    public static final Field field$HashMapPalette$values = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$HashMapPalette, clazz$CrudeIncrementalIntIdentityHashBiMap, 0)
    );

    public static final Class<?> clazz$LinearPalette = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteLinear",
                    "world.level.chunk.LinearPalette"
            )
    );

    public static final Field field$LinearPalette$values = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$LinearPalette, Object.class.arrayType(), 0)
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
            ReflectionUtils.getDeclaredField(clazz$Entity, clazz$SynchedEntityData, 0)
    );

    public static final Class<?> clazz$SupportType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockSupport",
                    "world.level.block.SupportType"
            )
    );

    public static final Method method$SupportType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$SupportType, clazz$SupportType.arrayType())
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
            ReflectionUtils.getMethod(clazz$BlockStateBase, boolean.class, clazz$BlockGetter, clazz$BlockPos, clazz$Direction, clazz$SupportType)
    );

    public static final Class<?> clazz$BlockInWorld = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.pattern.ShapeDetectorBlock",
                    "world.level.block.state.pattern.BlockInWorld"
            )
    );

    public static final Field field$BlockInWorld$state = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockInWorld, clazz$BlockState, 0)
    );

    public static final Method method$BlockBehaviour$getDescriptionId = requireNonNull(
            VersionHelper.isOrAbove1_21_2()
                    ? ReflectionUtils.getMethod(clazz$BlockBehaviour, String.class)
                    : ReflectionUtils.getMethod(clazz$Block, String.class)
    );

    public static final Class<?> clazz$BlockAndTintGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IBlockLightAccess",
                    "world.level.BlockAndTintGetter"
            )
    );

    public static final Method method$BlockAndTintGetter$getRawBrightness = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockAndTintGetter, int.class, clazz$BlockPos, int.class)
    );

    public static final Field field$Entity$boundingBox = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$Entity, clazz$AABB, 0)
    );

    public static final Class<?> clazz$Shulker = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.monster.EntityShulker",
                    "world.entity.monster.Shulker"
            )
    );

    public static final Class<?> clazz$Pose = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityPose",
                    "world.entity.Pose"
            )
    );

    public static final Method method$Pose$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Pose, clazz$Pose.arrayType())
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
//    public static final Constructor<?> constructor$AttributeInstance =
//            ReflectionUtils.getConstructor(clazz$AttributeInstance, clazz$Holder, Consumer.class);

//    public static final Method method$AttributeInstance$setBaseValue = requireNonNull(
//            ReflectionUtils.getMethod(clazz$AttributeInstance, void.class, double.class)
//    );

    public static final Class<?> clazz$Rotation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockRotation",
                    "world.level.block.Rotation"
            )
    );

    public static final Method method$Rotation$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$Rotation, clazz$Rotation.arrayType())
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
            ReflectionUtils.getMethod(clazz$Rotation, int.class, new String[]{"ordinal"})
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
            ReflectionUtils.getMethod(clazz$Mirror, int.class, new String[]{"ordinal"})
    );

    public static final Method method$BlockBehaviour$rotate = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Rotation)
    );

    public static final Method method$BlockBehaviour$mirror = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Mirror)
    );

    public static final Method method$BlockStateBase$rotate = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, clazz$BlockState, clazz$Rotation)
    );

    public static final Method method$BlockStateBase$mirror = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, clazz$BlockState, clazz$Mirror)
    );

    public static final Method method$Entity$getType = requireNonNull(
            ReflectionUtils.getMethod(clazz$Entity, clazz$EntityType)
    );

    public static final Class<?> clazz$AbstractArrow = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("world.entity.projectile.EntityArrow", "world.entity.projectile.arrow.EntityArrow"),
                    List.of("world.entity.projectile.AbstractArrow", "world.entity.projectile.arrow.AbstractArrow")
            )
    );

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
                    List.of("world.entity.projectile.EntityArrow$PickupStatus", "world.entity.projectile.arrow.EntityArrow$PickupStatus"),
                    List.of("world.entity.projectile.AbstractArrow$Pickup", "world.entity.projectile.arrow.AbstractArrow$Pickup")
            )
    );

    public static final Method method$AbstractArrow$Pickup$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$AbstractArrow$Pickup, clazz$AbstractArrow$Pickup.arrayType())
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

    public static final Class<?> clazz$Orientation =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.redstone.Orientation",
                    "world.level.redstone.Orientation"
            );

    public static final Method method$BlockBehaviour$neighborChanged = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$Orientation, boolean.class) :
                    Optional.ofNullable(ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$BlockPos, boolean.class))
                            .orElse(ReflectionUtils.getMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$BlockPos, boolean.class))
    );

    public static final Class<?> clazz$InventoryMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.ContainerPlayer",
                    "world.inventory.InventoryMenu"
            )
    );

    public static final Field field$Player$inventoryMenu = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Player, clazz$InventoryMenu, 0)
    );

    public static final Method method$AbstractContainerMenu$incrementStateId = requireNonNull(
            ReflectionUtils.getMethod(clazz$AbstractContainerMenu, int.class, new String[]{"incrementStateId", "k"})
    );

    public static final Field field$BlockParticleOption$blockState = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$BlockParticleOption, clazz$BlockState, 0)
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

    public static final Field field$ServerPlayer$connection = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$ServerPlayer, clazz$ServerGamePacketListenerImpl, 0)
    );

    public static final Method method$ServerLevel$getNoiseBiome = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerLevel, clazz$Holder, int.class, int.class, int.class)
    );

    public static final Class<?> clazz$MinecraftServer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.MinecraftServer"))
    );

    public static final Field field$MinecraftServer$registries = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$MinecraftServer, clazz$LayeredRegistryAccess, 0)
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

    public static final Class<?> clazz$ServerChunkCache = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.ChunkProviderServer",
                    "server.level.ServerChunkCache"
            )
    );

    public static final Class<?> clazz$ChunkHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerChunk",
                    "server.level.ChunkHolder"
            )
    );

    public static final Class<?> clazz$ChunkMap = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.level.PlayerChunkMap"),  // 这里paper会自动获取到NM.server.level.ChunkMap
                    BukkitReflectionUtils.assembleMCClass("server.level.ChunkMap") // paper柠檬酸了
            )
    );

    public static final Class<?> clazz$ChunkHolder$PlayerProvider = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.PlayerChunk$d",
                    "server.level.ChunkHolder$PlayerProvider"
            )
    );

    public static final Field field$ChunkHolder$playerProvider = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ChunkHolder, clazz$ChunkHolder$PlayerProvider, 0)
    );

    public static final Method method$ChunkHolder$PlayerProvider$getPlayers = requireNonNull(
            ReflectionUtils.getMethod(clazz$ChunkHolder$PlayerProvider, List.class, clazz$ChunkPos, boolean.class)
    );

    public static final Method method$ChunkHolder$getPlayers =
            ReflectionUtils.getMethod(clazz$ChunkHolder, List.class, boolean.class);

    public static final Field field$ChunkHolder$lightEngine = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ChunkHolder, clazz$LevelLightEngine, 0)
    );

    public static final Field field$ChunkHolder$blockChangedLightSectionFilter = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ChunkHolder, BitSet.class, 0)
    );

    public static final Field field$ChunkHolder$skyChangedLightSectionFilter = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ChunkHolder, BitSet.class, 1)
    );

    public static final Method method$ServerChunkCache$getVisibleChunkIfPresent = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$ServerChunkCache, clazz$ChunkHolder, long.class)
    );

    public static final Method method$ChunkHolder$sectionLightChanged = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getMethod(clazz$ChunkHolder, boolean.class, clazz$LightLayer, int.class) :
                    ReflectionUtils.getMethod(clazz$ChunkHolder, void.class, clazz$LightLayer, int.class)
    );

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
            ReflectionUtils.getDeclaredField(clazz$ServerPlayer, clazz$ServerPlayerGameMode, 0)
    );

    public static final Field field$ServerPlayerGameMode$destroyProgressStart = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerPlayerGameMode, int.class, 0)
    );


    public static final Field field$ServerPlayerGameMode$gameTicks = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerPlayerGameMode, int.class, 1)
    );

    public static final Field field$ServerPlayerGameMode$delayedTickStart = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerPlayerGameMode, int.class, 2)
    );

    public static final Field field$ServerPlayerGameMode$isDestroyingBlock = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerPlayerGameMode, boolean.class, 0)
    );

    public static final Field field$ServerPlayerGameMode$hasDelayedDestroy = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerPlayerGameMode, boolean.class, 1)
    );

    public static final Method method$ServerPlayerGameMode$destroyBlock = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerPlayerGameMode, boolean.class, clazz$BlockPos)
    );

    public static final Method method$ServerPlayer$getEffect = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ?
                    ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$MobEffectInstance, clazz$MobEffect) :
                    ReflectionUtils.getMethod(clazz$ServerPlayer, clazz$MobEffectInstance, clazz$Holder)
    );

    public static final Field field$ServerLevel$uuid = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ServerLevel, UUID.class, 0)
    );

    public static final Method method$ServerLevel$checkEntityCollision = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerLevel, boolean.class, clazz$BlockState, clazz$Entity, clazz$CollisionContext, clazz$BlockPos, boolean.class)
    );

    public static final Class<?> clazz$ResourceManager = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.IResourceManager"),  // 这里paper会自动获取到NM.server.packs.resources.ResourceManager
                    BukkitReflectionUtils.assembleMCClass("server.packs.resources.ResourceManager") // paper柠檬酸了
            )
    );

    public static final Class<?> clazz$Resource = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.resources.IResource",
                    "server.packs.resources.Resource"
            )
    );

    public static final Method method$Resource$openAsReader = requireNonNull(
            ReflectionUtils.getMethod(clazz$Resource, BufferedReader.class)
    );

    public static final MethodHandle methodHandle$Resource$openAsReader;

    static {
        try {
            methodHandle$Resource$openAsReader = ReflectionUtils.unreflectMethod(method$Resource$openAsReader)
                    .asType(MethodType.methodType(Reader.class, Object.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to init methodHandle$Resource$openAsReader", e);
        }
    }

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
            ReflectionUtils.getStaticMethod(clazz$PackType, clazz$PackType.arrayType())
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
            ReflectionUtils.getMethod(clazz$MinecraftServer, clazz$PackRepository)
    );

    public static final Field field$PackRepository$selected = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$PackRepository, List.class, 0)
    );

    public static final MethodHandle methodHandle$MinecraftServer$getPackRepository;
    public static final MethodHandle methodHandle$PackRepository$selectedGetter;

    static {
        try {
            methodHandle$MinecraftServer$getPackRepository = ReflectionUtils.unreflectMethod(method$MinecraftServer$getPackRepository)
                    .asType(MethodType.methodType(Object.class, Object.class));
            methodHandle$PackRepository$selectedGetter = ReflectionUtils.unreflectGetter(field$PackRepository$selected)
                    .asType(MethodType.methodType(List.class, Object.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize reflection for methodHandle$MinecraftServer$getPackRepository", e);
        }
    }

    public static final Class<?> clazz$Pack = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.repository.ResourcePackLoader",
                    "server.packs.repository.Pack"
            )
    );

    public static final Method method$PackRepository$getPack = requireNonNull(
            ReflectionUtils.getMethod(clazz$PackRepository, clazz$Pack, String.class)
    );

    public static final Method method$Pack$getId = requireNonNull(
            ReflectionUtils.getMethod(clazz$Pack, String.class)
    );

    public static final Method method$MinecraftServer$reloadResources = requireNonNull(
            ReflectionUtils.getMethod(clazz$MinecraftServer, CompletableFuture.class, Collection.class)
    );

    public static final Class<?> clazz$PackResources = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.packs.IResourcePack",
                    "server.packs.PackResources"
            )
    );

    public static final Method method$Pack$open = requireNonNull(
            ReflectionUtils.getMethod(clazz$Pack, clazz$PackResources)
    );

    public static final Constructor<?> constructor$MultiPackResourceManager = requireNonNull(
            ReflectionUtils.getConstructor(clazz$MultiPackResourceManager, clazz$PackType, List.class)
    );

    public static final Method method$MinecraftServer$getRecipeManager = requireNonNull(
            ReflectionUtils.getMethod(clazz$MinecraftServer, clazz$RecipeManager)
    );

    public static final Class<?> clazz$DedicatedPlayerList = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedPlayerList"))
    );

    public static final Method method$DedicatedPlayerList$reloadRecipes = requireNonNull(
            ReflectionUtils.getMethod(clazz$DedicatedPlayerList, new String[] {"reloadRecipeData", "reloadRecipes"})
    );

    public static final MethodHandle methodHandle$DedicatedPlayerList$reloadRecipes;
    public static final MethodHandle methodHandle$Pack$open;
    public static final MethodHandle methodHandle$MultiPackResourceManagerConstructor;

    static {
        try {
            methodHandle$DedicatedPlayerList$reloadRecipes = ReflectionUtils.unreflectMethod(method$DedicatedPlayerList$reloadRecipes)
                    .asType(MethodType.methodType(void.class, Object.class));
            methodHandle$Pack$open = ReflectionUtils.unreflectMethod(method$Pack$open)
                    .asType(MethodType.methodType(Object.class, Object.class));
            methodHandle$MultiPackResourceManagerConstructor = ReflectionUtils.unreflectConstructor(constructor$MultiPackResourceManager)
                    .asType(MethodType.methodType(AutoCloseable.class, Object.class, List.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to init methodHandle$DedicatedPlayerList$reloadRecipes", e);
        }
    }

    public static final Method method$ServerChunkCache$getGenerator = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerChunkCache, clazz$ChunkGenerator)
    );

    public static final Method method$ServerLevel$sendBlockUpdated = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$ServerLevel, void.class, clazz$BlockPos, clazz$BlockState, clazz$BlockState, int.class
            )
    );

    public static final Method method$ServerLevel$levelEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$ServerLevel, void.class, clazz$Entity, int.class, clazz$BlockPos, int.class)
                    : ReflectionUtils.getMethod(clazz$ServerLevel, void.class, clazz$Player, int.class, clazz$BlockPos, int.class)
    );

    public static final Method method$ServerGamePacketListenerImpl$tryPickItem =
            VersionHelper.isOrAbove1_21_5() ?
                    ReflectionUtils.getDeclaredMethod(clazz$ServerGamePacketListenerImpl, void.class, clazz$ItemStack, clazz$BlockPos, clazz$Entity, boolean.class) :
                    ReflectionUtils.getDeclaredMethod(clazz$ServerGamePacketListenerImpl, void.class, clazz$ItemStack);

    public static final Method method$ServerPlayer$nextContainerCounter = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerPlayer, int.class, new String[] {"nextContainerCounter"})
    );

    public static final Method method$ServerPlayer$initMenu = requireNonNull(
            ReflectionUtils.getMethod(clazz$ServerPlayer, void.class, clazz$AbstractContainerMenu)
    );

    public static final Class<?> clazz$DedicatedServerProperties = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServerProperties"))
    );

    public static final Class<?> clazz$DedicatedServerSettings = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServerSettings"))
    );

    public static final Class<?> clazz$DedicatedServer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.dedicated.DedicatedServer"))
    );

    public static final Field field$DedicatedServerSettings$properties = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$DedicatedServerSettings, clazz$DedicatedServerProperties, 0)
    );

    public static final Field field$DedicatedServer$settings = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$DedicatedServer, clazz$DedicatedServerSettings, 0)
    );

    public static final Class<?> clazz$MinecraftServer$ServerResourcePackInfo = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.MinecraftServer$ServerResourcePackInfo"))
    );

    public static final Field field$DedicatedServerProperties$serverResourcePackInfo = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$DedicatedServerProperties, Optional.class, 0)
    );

    public static final Constructor<?> constructor$ServerResourcePackInfo = requireNonNull(
            ReflectionUtils.getConstructor(clazz$MinecraftServer$ServerResourcePackInfo, 0)
    );

    // 1.20.2+
    // 从 1.21.2+ 才有 particleStatus
    public static final Class<?> clazz$ClientInformation =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.level.ClientInformation"));

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
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.level.ParticleStatus"));

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

    public static final Method method$BonemealableBlock$performBonemeal = requireNonNull(
            ReflectionUtils.getMethod(clazz$BonemealableBlock, void.class, clazz$ServerLevel, clazz$RandomSource, clazz$BlockPos, clazz$BlockState)
    );
    
    public static final Method method$BlockBehaviour$tick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"tick", "a"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource)
    );

    public static final Method method$BlockBehaviour$randomTick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"randomTick", "b"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource)
    );

    public static final Method method$BlockBehaviour$onPlace = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"onPlace", VersionHelper.isOrAbove1_21_5() ? "a" : "b"},
                    clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$InsideBlockEffectApplier = BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.entity.InsideBlockEffectApplier",
            "world.entity.InsideBlockEffectApplier"
    );

    public static final Method method$BlockBehaviour$entityInside = requireNonNull(
            VersionHelper.isOrAbove1_21_10()
                    ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity, clazz$InsideBlockEffectApplier, boolean.class)
                    : VersionHelper.isOrAbove1_21_5()
                        ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity, clazz$InsideBlockEffectApplier)
                        : ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity)
    );

    // 1.21.5+
    public static final Method method$BlockBehaviour$affectNeighborsAfterRemoval = ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"affectNeighborsAfterRemoval", "a"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, boolean.class);

    public static final Method method$BlockBehaviour$getSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getSignal", "a"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$Direction)
    );

    public static final Method method$BlockBehaviour$getDirectSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getDirectSignal", "b"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$Direction)
    );

    public static final Method method$BlockBehaviour$isSignalSource = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, new String[]{
                    "isSignalSource",
                    !VersionHelper.isOrAbove1_20_5() ? "f_" : // 1.20.1-1.20.4
                    !VersionHelper.isOrAbove1_21_2() ? "e_" /* 1.20.5-1.21.1 */ : "f_" // 1.21.2+
            }, clazz$BlockState)
    );

    public static final Method method$FileToIdConverter$listMatchingResources = requireNonNull(
            ReflectionUtils.getMethod(clazz$FileToIdConverter, Map.class, new String[]{"listMatchingResources", "a"}, clazz$ResourceManager)
    );

    public static final MethodHandle methodHandle$FileToIdConverter$listMatchingResources;

    static {
        try {
            methodHandle$FileToIdConverter$listMatchingResources = ReflectionUtils.unreflectMethod(method$FileToIdConverter$listMatchingResources)
                    .asType(MethodType.methodType(Map.class, Object.class, AutoCloseable.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize methodHandle$FileToIdConverter$listMatchingResources", e);
        }
    }

    public static final Method method$RegistryOps$create = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$RegistryOps, clazz$RegistryOps, DynamicOps.class, clazz$HolderLookup$Provider)
    );

    public static final Method method$DefaultedRegistry$get = requireNonNull(
            ReflectionUtils.getMethod(clazz$DefaultedRegistry, Object.class, clazz$ResourceLocation)
    );

    public static final Class<?> clazz$BlockStateParser = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                "commands.arguments.blocks.ArgumentBlock",
                    "commands.arguments.blocks.BlockStateParser"
            )
    );

    public static final Class<?> clazz$BlockStateParser$BlockResult = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "commands.arguments.blocks.ArgumentBlock$a",
                    "commands.arguments.blocks.BlockStateParser$BlockResult"
            )
    );

    public static final Class<?> clazz$HolderLookup = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.HolderLookup",
                    "core.HolderLookup"
            )
    );

    public static final Class<?> clazz$HolderLookup$RegistryLookup = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.HolderLookup$c",
                    "core.HolderLookup$RegistryLookup"
            )
    );

    public static final Method method$BlockStateParser$BlockResult$blockState = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateParser$BlockResult, clazz$BlockState, 0)
    );

    public static final Method method$BlockStateParser$parseForBlock = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$BlockStateParser, clazz$BlockStateParser$BlockResult, new String[]{"parseForBlock", "a"}, clazz$HolderLookup, String.class, boolean.class)
    );

    // 1.21.1-
    public static final Method method$Registry$asLookup = ReflectionUtils.getMethod(
            clazz$Registry, clazz$HolderLookup$RegistryLookup, new String[]{"asLookup", "p"}
    );

    // public static final Field field$ServerEntity$broadcast = requireNonNull(
    //         ReflectionUtils.getDeclaredField(
    //                 clazz$ServerEntity, Consumer.class, 0
    //         )
    // );

    // public static final MethodHandle methodHandle$ServerEntity$broadcastSetter;
    public static final MethodHandle methodHandle$ServerEntity$updateIntervalSetter;
    public static final MethodHandle methodHandle$ServerPlayer$connectionGetter;
    public static final MethodHandle methodHandle$ServerPlayer$getAttributeMethod;

    static {
        try {
            // methodHandle$ServerEntity$broadcastSetter = requireNonNull(
            //         ReflectionUtils.unreflectSetter(field$ServerEntity$broadcast)
            //                 .asType(MethodType.methodType(void.class, Object.class, Consumer.class))
            // );
            methodHandle$ServerEntity$updateIntervalSetter = requireNonNull(
                    ReflectionUtils.unreflectSetter(field$ServerEntity$updateInterval)
                            .asType(MethodType.methodType(void.class, Object.class, int.class))
            );
            methodHandle$ServerPlayer$connectionGetter = requireNonNull(
                    ReflectionUtils.unreflectGetter(field$ServerPlayer$connection)
                            .asType(MethodType.methodType(Object.class, Object.class))
            );
            methodHandle$ServerPlayer$getAttributeMethod = requireNonNull(
                    ReflectionUtils.unreflectMethod(method$ServerPlayer$getAttribute)
                            .asType(MethodType.methodType(Object.class, Object.class, Object.class))
            );
        } catch (IllegalAccessException e) {
            throw new ReflectionInitException("Failed to initialize reflection", e);
        }
    }

    public static final Class<?> clazz$BaseFireBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFireAbstract",
                    "world.level.block.BaseFireBlock"
            )
    );

    public static final Method method$BaseFireBlock$canBePlacedAt = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$BaseFireBlock, boolean.class, clazz$Level, clazz$BlockPos, clazz$Direction)
    );

    public static final Field field$FireBlock$igniteOdds = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$FireBlock, Object2IntMap.class, 0)
    );

    public static final Field field$FireBlock$burnOdds = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$FireBlock, Object2IntMap.class, 1)
    );

    public static final Class<?> clazz$EnchantmentMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.ContainerEnchantTable",
                    "world.inventory.EnchantmentMenu"
            )
    );

    public static final Class<?> clazz$RedStoneWireBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockRedstoneWire",
                    "world.level.block.RedStoneWireBlock"
            )
    );

    // 1.20.5+
    public static final Field field$ItemStack$CODEC = ReflectionUtils.getDeclaredField(clazz$ItemStack, "CODEC", "b");

    public static final Codec<Object> instance$ItemStack$CODEC = getItemStack$CODEC();

    @SuppressWarnings("unchecked")
    private static Codec<Object> getItemStack$CODEC() {
        try {
            return VersionHelper.isOrAbove1_20_5() ? (Codec<Object>) field$ItemStack$CODEC.get(null) : null;
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init ItemStack$CODEC", e);
        }
    }

    public static final Class<?> clazz$StairBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockStairs",
                    "world.level.block.StairBlock"
            )
    );

    public static final Class<?> clazz$StairsShape = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockPropertyStairsShape",
                    "world.level.block.state.properties.StairsShape"
            )
    );

    public static final Method method$StairsShape$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$StairsShape, clazz$StairsShape.arrayType())
    );

    public static final Method method$StairsShape$ordinal = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$StairsShape, new String[]{"ordinal"}
            )
    );

    public static final Object instance$StairsShape$STRAIGHT;
    public static final Object instance$StairsShape$INNER_LEFT;
    public static final Object instance$StairsShape$INNER_RIGHT;
    public static final Object instance$StairsShape$OUTER_LEFT;
    public static final Object instance$StairsShape$OUTER_RIGHT;

    static {
        try {
            Object[] values = (Object[]) method$StairsShape$values.invoke(null);
            instance$StairsShape$STRAIGHT = values[0];
            instance$StairsShape$INNER_LEFT = values[1];
            instance$StairsShape$INNER_RIGHT = values[2];
            instance$StairsShape$OUTER_LEFT = values[3];
            instance$StairsShape$OUTER_RIGHT = values[4];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$EnumProperty = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockStateEnum",
                    "world.level.block.state.properties.EnumProperty"
            )
    );

    // 1.20~1.21.1
    public static final Class<?> clazz$DirectionProperty =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockStateDirection",
                    "world.level.block.state.properties.DirectionProperty"
            );

    public static final Field field$StairBlock$FACING = requireNonNull(
            VersionHelper.isOrAbove1_21_2()
                    ? ReflectionUtils.getDeclaredField(clazz$StairBlock, clazz$EnumProperty, 0)
                    : ReflectionUtils.getDeclaredField(clazz$StairBlock, clazz$DirectionProperty, 0)
    );

    public static final Field field$StairBlock$HALF = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$StairBlock, clazz$EnumProperty, VersionHelper.isOrAbove1_21_2() ? 1 : 0
            )
    );

    public static final Field field$StairBlock$SHAPE = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$StairBlock, clazz$EnumProperty, VersionHelper.isOrAbove1_21_2() ? 2 : 1
            )
    );

    public static final Object instance$StairBlock$FACING;
    public static final Object instance$StairBlock$HALF;
    public static final Object instance$StairBlock$SHAPE;

    static {
        try {
            instance$StairBlock$FACING = requireNonNull(field$StairBlock$FACING.get(null));
            instance$StairBlock$HALF = requireNonNull(field$StairBlock$HALF.get(null));
            instance$StairBlock$SHAPE = requireNonNull(field$StairBlock$SHAPE.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$BasePressurePlateBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockPressurePlateAbstract",
                    "world.level.block.BasePressurePlateBlock"
            )
    );

    public static final Field field$BasePressurePlateBlock$TOUCH_AABB = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$BasePressurePlateBlock, clazz$AABB, 0
            )
    );

    public static final Object instance$BasePressurePlateBlock$TOUCH_AABB;

    static {
        try {
            instance$BasePressurePlateBlock$TOUCH_AABB = requireNonNull(field$BasePressurePlateBlock$TOUCH_AABB.get(null));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Class<?> clazz$LootParams$Builder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.storage.loot.LootParams$a",
                    "world.level.storage.loot.LootParams$Builder"
            )
    );

    public static final Method method$BlockStateBase$getDrops = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, List.class, clazz$LootParams$Builder)
    );

    public static final Class<?> clazz$LootContextParams = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.storage.loot.parameters.LootContextParameters",
                    "world.level.storage.loot.parameters.LootContextParams"
            )
    );

    public static final Class<?> clazz$BlockStateProperties = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.BlockProperties",
                    "world.level.block.state.properties.BlockStateProperties"
            )
    );

    public static final Class<?> clazz$CompoundTag = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "nbt.NBTTagCompound",
                    "nbt.CompoundTag"
            )
    );

    public static final Class<?> clazz$TrimPattern = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.equipment.trim.TrimPattern",
                    "world.item.equipment.trim.TrimPattern"
            ) :
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.armortrim.TrimPattern",
                    "world.item.armortrim.TrimPattern"
            )
    );

    public static final Class<?> clazz$TrimMaterial = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    BukkitReflectionUtils.findReobfOrMojmapClass(
                            "world.item.equipment.trim.TrimMaterial",
                            "world.item.equipment.trim.TrimMaterial"
                    ) :
                    BukkitReflectionUtils.findReobfOrMojmapClass(
                            "world.item.armortrim.TrimMaterial",
                            "world.item.armortrim.TrimMaterial"
                    )
    );

    public static final Class<?> clazz$MaterialAssetGroup = BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.item.equipment.trim.MaterialAssetGroup",
            "world.item.equipment.trim.MaterialAssetGroup"
    );

    public static final Method method$MaterialAssetGroup$create = Optional.ofNullable(clazz$MaterialAssetGroup)
            .map(it -> ReflectionUtils.getStaticMethod(it, it, String.class)).orElse(null);

    public static final Constructor<?> constructor$TrimPattern = requireNonNull(
            VersionHelper.isOrAbove1_21_5() ?
            ReflectionUtils.getConstructor(clazz$TrimPattern, clazz$ResourceLocation, clazz$Component, boolean.class) :
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getConstructor(clazz$TrimPattern, clazz$ResourceLocation, clazz$Holder, clazz$Component, boolean.class) :
            ReflectionUtils.getConstructor(clazz$TrimPattern, clazz$ResourceLocation, clazz$Holder, clazz$Component)
    );

    public static final Constructor<?> constructor$TrimMaterial = requireNonNull(
            VersionHelper.isOrAbove1_21_5() ?
            ReflectionUtils.getConstructor(clazz$TrimMaterial, clazz$MaterialAssetGroup, clazz$Component) :
            VersionHelper.isOrAbove1_21_4() ?
            ReflectionUtils.getConstructor(clazz$TrimMaterial, String.class, clazz$Holder, Map.class, clazz$Component) :
            ReflectionUtils.getConstructor(clazz$TrimMaterial, String.class, clazz$Holder, float.class, Map.class, clazz$Component)
    );

    public static final Class<?> clazz$ServerConfigurationPacketListenerImpl = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.network.ServerConfigurationPacketListenerImpl")
            ),
            VersionHelper.isOrAbove1_20_2()
    );

    // 1.20.2+
    public static final Field field$ServerConfigurationPacketListenerImpl$configurationTasks = Optional.ofNullable(clazz$ServerConfigurationPacketListenerImpl)
            .map(it -> ReflectionUtils.getDeclaredField(it, Queue.class, 0))
            .orElse(null);

    public static final MethodHandle methodHandle$ServerConfigurationPacketListenerImpl$configurationTasksGetter;

    static {
        try {
            if (VersionHelper.isOrAbove1_20_2()) {
                methodHandle$ServerConfigurationPacketListenerImpl$configurationTasksGetter =
                        ReflectionUtils.unreflectGetter(field$ServerConfigurationPacketListenerImpl$configurationTasks)
                                .asType(MethodType.methodType(Queue.class, Object.class));
            } else {
                methodHandle$ServerConfigurationPacketListenerImpl$configurationTasksGetter = null;
            }
        } catch (IllegalAccessException e) {
            throw new ReflectionInitException("Failed to initialize reflection", e);
        }
    }

    // 1.20.2+
    public static final Method method$ServerConfigurationPacketListenerImpl$startNextTask = Optional.ofNullable(clazz$ServerConfigurationPacketListenerImpl)
            .map(it -> ReflectionUtils.getDeclaredMethod(it, void.class, VersionHelper.isOrAbove1_20_5() ? new String[]{"startNextTask", "o"} : new String[]{"startNextTask", "p"}))
            .orElse(null);

    public static final MethodHandle methodHandle$ServerConfigurationPacketListenerImpl$startNextTask;

    static {
        try {
            if (VersionHelper.isOrAbove1_20_2()) {
                methodHandle$ServerConfigurationPacketListenerImpl$startNextTask =
                        ReflectionUtils.unreflectMethod(method$ServerConfigurationPacketListenerImpl$startNextTask)
                                .asType(MethodType.methodType(void.class, Object.class));
            } else {
                methodHandle$ServerConfigurationPacketListenerImpl$startNextTask = null;
            }
        } catch (IllegalAccessException e) {
            throw new ReflectionInitException("Failed to initialize reflection", e);
        }
    }

    public static final Class<?> clazz$JoinWorldTask = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.network.config.JoinWorldTask")
            ),
            VersionHelper.isOrAbove1_20_2()
    );

    // 1.20.2+
    public static final Constructor<?> constructor$JoinWorldTask = Optional.ofNullable(clazz$JoinWorldTask)
            .map(ReflectionUtils::getTheOnlyConstructor)
            .orElse(null);

    public static final Class<?> clazz$ConfigurationTask$Type = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.network.ConfigurationTask$a",
                    "server.network.ConfigurationTask$Type"
            ),
            VersionHelper.isOrAbove1_20_2()
    );

    // 1.20.2+
    public static final Field field$JoinWorldTask$TYPE = Optional.ofNullable(clazz$JoinWorldTask)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$ConfigurationTask$Type, 0))
            .orElse(null);

    public static final Class<?> clazz$ServerResourcePackConfigurationTask = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("server.network.config.ServerResourcePackConfigurationTask")
            ),
            VersionHelper.isOrAbove1_20_2()
    );

    // 1.20.2+
    public static final Field field$ServerResourcePackConfigurationTask$TYPE = Optional.ofNullable(clazz$ServerResourcePackConfigurationTask)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$ConfigurationTask$Type, 0))
            .orElse(null);

    public static final Object instance$JoinWorldTask;
    public static final Object instance$JoinWorldTask$TYPE;
    public static final Object instance$ServerResourcePackConfigurationTask$TYPE;

    static {
        try {
            if (VersionHelper.isOrAbove1_20_2()) {
                instance$JoinWorldTask = constructor$JoinWorldTask.newInstance();
                instance$JoinWorldTask$TYPE = field$JoinWorldTask$TYPE.get(null);
                instance$ServerResourcePackConfigurationTask$TYPE = field$ServerResourcePackConfigurationTask$TYPE.get(null);
            } else {
                instance$JoinWorldTask = null;
                instance$JoinWorldTask$TYPE = null;
                instance$ServerResourcePackConfigurationTask$TYPE = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize reflection", e);
        }
    }

    // 注释的这些说不定以后调试有用
    // public static final Class<?> clazz$ConfigurationTask = MiscUtils.requireNonNullIf(
    //         ReflectionUtils.getClazz(
    //                 BukkitReflectionUtils.assembleMCClass("server.network.ConfigurationTask")
    //         ),
    //         VersionHelper.isOrAbove1_20_2()
    // );
    //
    // public static final Field field$ServerConfigurationPacketListenerImpl$currentTask = MiscUtils.requireNonNullIf(
    //         ReflectionUtils.getDeclaredField(clazz$ServerConfigurationPacketListenerImpl, clazz$ConfigurationTask, 0),
    //         VersionHelper.isOrAbove1_20_2()
    // );

    // 1.20.2+
    public static final Method method$ServerConfigurationPacketListenerImpl$finishCurrentTask = Optional.ofNullable(clazz$ServerConfigurationPacketListenerImpl)
            .map(it -> ReflectionUtils.getDeclaredMethod(it, void.class, clazz$ConfigurationTask$Type))
            .orElse( null);

    public static final Field field$ServerCommonPacketListenerImpl$closed = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredField(clazz$ServerCommonPacketListenerImpl, "closed", "n"),
            VersionHelper.isOrAbove1_20_5()
    );

    public static final MethodHandle methodHandle$ServerConfigurationPacketListenerImpl$finishCurrentTask;
    public static final MethodHandle methodHandle$ServerCommonPacketListenerImpl$closedSetter;

    static {
        try {
            if (VersionHelper.isOrAbove1_20_2()) {
                methodHandle$ServerConfigurationPacketListenerImpl$finishCurrentTask =
                        ReflectionUtils.unreflectMethod(method$ServerConfigurationPacketListenerImpl$finishCurrentTask)
                                .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$ServerConfigurationPacketListenerImpl$finishCurrentTask = null;
            }
            if (VersionHelper.isOrAbove1_20_5()) {
                methodHandle$ServerCommonPacketListenerImpl$closedSetter =
                        ReflectionUtils.unreflectSetter(field$ServerCommonPacketListenerImpl$closed)
                                .asType(MethodType.methodType(void.class, Object.class, boolean.class));
            } else {
                methodHandle$ServerCommonPacketListenerImpl$closedSetter = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize reflection", e);
        }
    }

    public static final Method method$Block$playerWillDestroy = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Block,
                    VersionHelper.isOrAbove1_20_3() ? clazz$BlockState : void.class,
                    clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$Player
            )
    );

    public static final Class<?> clazz$BlockItem = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.ItemBlock",
                    "world.item.BlockItem"
            )
    );

    public static final Class<?> clazz$ArmorTrim = requireNonNull(
            ReflectionUtils.getClazz(
                    VersionHelper.isOrAbove1_21_2() ?
                    BukkitReflectionUtils.assembleMCClass("world.item.equipment.trim.ArmorTrim") :
                    BukkitReflectionUtils.assembleMCClass("world.item.armortrim.ArmorTrim")
            )
    );

    public static final Field field$ArmorTrim$CODEC = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$ArmorTrim, Codec.class, 0)
    );

    public static final Codec<?> instance$ArmorTrim$CODEC;

    static {
        try {
            instance$ArmorTrim$CODEC = (Codec<?>) field$ArmorTrim$CODEC.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize ArmorTrim CODEC", e);
        }
    }

    public static final Method method$ArmorTrim$setTrim = ReflectionUtils.getStaticMethod(
            clazz$ArmorTrim, boolean.class, clazz$RegistryAccess, clazz$ItemStack, clazz$ArmorTrim
    );

    public static final Method method$ArmorTrim$getTrim =
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getStaticMethod(clazz$ArmorTrim, Optional.class, clazz$RegistryAccess, clazz$ItemStack, boolean.class) :
            ReflectionUtils.getStaticMethod(clazz$ArmorTrim, Optional.class, clazz$RegistryAccess, clazz$ItemStack);

    public static final Method method$BlockBehaviour$spawnAfterBreak = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$ItemStack, boolean.class)
    );

    // 1.20~1.21.4
    public static final Method method$BlockBehaviour$onRemove = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"a", "onRemove"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class),
            !VersionHelper.isOrAbove1_21_5()
    );

    public static final Object instance$CollisionContext$empty;

    static {
        try {
            instance$CollisionContext$empty = requireNonNull(method$CollisionContext$empty.invoke(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 1.21.6+
    public static final Method method$CollisionContext$placementContext = MiscUtils.requireNonNullIf(ReflectionUtils.getStaticMethod(
            clazz$CollisionContext, clazz$CollisionContext, clazz$Player
    ), VersionHelper.isOrAbove1_21_6());

    public static final Constructor<?> constructor$BlockHitResult = requireNonNull(
            ReflectionUtils.getConstructor(
                    CoreReflections.clazz$BlockHitResult, CoreReflections.clazz$Vec3, CoreReflections.clazz$Direction, CoreReflections.clazz$BlockPos, boolean.class
            )
    );

    public static final Class<?> clazz$HashOps = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("util.HashOps")),
            VersionHelper.isOrAbove1_21_5()
    );

    public static final Field field$HashOps$CRC32C_INSTANCE = Optional.ofNullable(clazz$HashOps)
            .map(it -> ReflectionUtils.getDeclaredField(it, it, 0))
            .orElse(null);

    public static final Object instance$HashOps$CRC32C_INSTANCE;

    static {
        try {
            if (VersionHelper.isOrAbove1_21_5()) {
                instance$HashOps$CRC32C_INSTANCE = field$HashOps$CRC32C_INSTANCE.get(null);
            } else {
                instance$HashOps$CRC32C_INSTANCE = null;
            }
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize HashOps", e);
        }
    }

    public static final Class<?> clazz$SnowLayerBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockSnow",
                    "world.level.block.SnowLayerBlock"
            )
    );

    public static final Field field$SnowLayerBlock$LAYERS = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$SnowLayerBlock, clazz$IntegerProperty, 0
            )
    );

    public static final Object instance$SnowLayerBlock$LAYERS;

    static {
        try {
            instance$SnowLayerBlock$LAYERS = field$SnowLayerBlock$LAYERS.get(null);
        } catch (IllegalAccessException e) {
            throw new ReflectionInitException("Failed to initialize SnowLayerBlock$LAYERS", e);
        }
    }

    public static final Class<?> clazz$DyeItem = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.ItemDye",
                    "world.item.DyeItem"
            )
    );

    public static final Method method$Recipe$matches = requireNonNull(
            VersionHelper.isOrAbove1_21() ?
            ReflectionUtils.getMethod(clazz$Recipe, boolean.class, clazz$RecipeInput, clazz$Level) :
            ReflectionUtils.getMethod(clazz$Recipe, boolean.class, clazz$Container, clazz$Level)
    );

    public static final Method method$Recipe$assemble = requireNonNull(
            VersionHelper.isOrAbove1_21() ?
            ReflectionUtils.getMethod(clazz$Recipe, clazz$ItemStack, clazz$RecipeInput, clazz$HolderLookup$Provider) :
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getMethod(clazz$Recipe, clazz$ItemStack, clazz$Container, clazz$HolderLookup$Provider) :
            ReflectionUtils.getMethod(clazz$Recipe, clazz$ItemStack, clazz$Container, clazz$RegistryAccess)
    );

    public static final Class<?> clazz$CraftingBookCategory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingBookCategory",
                    "world.item.crafting.CraftingBookCategory"
            )
    );

    public static final Method method$CraftingBookCategory$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftingBookCategory, clazz$CraftingBookCategory.arrayType())
    );

    public static final Object instance$CraftingBookCategory$BUILDING;
    public static final Object instance$CraftingBookCategory$REDSTONE;
    public static final Object instance$CraftingBookCategory$EQUIPMENT;
    public static final Object instance$CraftingBookCategory$MISC;

    static {
        try {
            Object[] values = (Object[]) method$CraftingBookCategory$values.invoke(null);
            instance$CraftingBookCategory$BUILDING = values[0];
            instance$CraftingBookCategory$REDSTONE = values[1];
            instance$CraftingBookCategory$EQUIPMENT = values[2];
            instance$CraftingBookCategory$MISC = values[3];
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize CraftingBookCategory", e);
        }
    }

    public static final Class<?> clazz$CraftingInput = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingInput",
                    "world.item.crafting.CraftingInput"
            ), VersionHelper.isOrAbove1_21()
    );

    public static final Class<?> clazz$CraftingContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.InventoryCrafting",
                    "world.inventory.CraftingContainer"
            )
    );

    public static final Class<?> clazz$DyeableLeatherItem = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.IDyeable",
                    "world.item.DyeableLeatherItem"
            ), !VersionHelper.isOrAbove1_20_5()
    );

    public static final Class<?> clazz$LootPoolEntryType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.storage.loot.entries.LootEntryType",
                    "world.level.storage.loot.entries.LootPoolEntryType"
            )
    );

    public static final Method method$BlockAndTintGetter$getLightEngine = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockAndTintGetter, clazz$LevelLightEngine)
    );

    public static final Method method$Block$fallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Block, void.class, clazz$Level, clazz$BlockState, clazz$BlockPos, clazz$Entity, VersionHelper.isOrAbove1_21_5() ? double.class : float.class)
    );

    public static final Method method$Block$updateEntityMovementAfterFallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Block, void.class, clazz$BlockGetter, clazz$Entity)
    );

    public static final Class<?> clazz$AdvancementRewards = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementRewards",
                    "advancements.AdvancementRewards"
            )
    );

    public static final Field field$AdvancementRewards$EMPTY = requireNonNull(
            ReflectionUtils.getStaticDeclaredField(clazz$AdvancementRewards, clazz$AdvancementRewards, 0)
    );

    public static final Object instance$AdvancementRewards$EMPTY;

    static {
        try {
            instance$AdvancementRewards$EMPTY = field$AdvancementRewards$EMPTY.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize AdvancementRewards$EMPTY", e);
        }
    }

    public static final Class<?> clazz$AdvancementRequirements = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementRequirements",
                    "advancements.AdvancementRequirements"
            ), VersionHelper.isOrAbove1_20_2()
    );

    public static final Constructor<?> constructor$AdvancementRequirements = Optional.ofNullable(clazz$AdvancementRequirements)
            .map(it -> {
                if (VersionHelper.isOrAbove1_20_3()) {
                    return ReflectionUtils.getConstructor(it, List.class);
                } else {
                    return ReflectionUtils.getConstructor(it, String[][].class);
                }
            }).orElse(null);

    public static final Class<?> clazz$AdvancementProgress = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementProgress",
                    "advancements.AdvancementProgress"
            )
    );

    public static final Constructor<?> constructor$AdvancementProgress = requireNonNull(
            ReflectionUtils.getConstructor(clazz$AdvancementProgress)
    );

    public static final Method method$AdvancementProgress$update = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getMethod(clazz$AdvancementProgress, void.class, clazz$AdvancementRequirements) :
            ReflectionUtils.getMethod(clazz$AdvancementProgress, void.class, Map.class, String[][].class)
    );

    public static final Class<?> clazz$AdvancementType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementFrameType",
                    VersionHelper.isOrAbove1_20_3() ? "advancements.AdvancementType" : "advancements.FrameType"
            )
    );

    public static final Method method$AdvancementType$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$AdvancementType, clazz$AdvancementType.arrayType())
    );

    public static final Object[] instance$AdvancementType$values;

    static {
        try {
            instance$AdvancementType$values = (Object[]) method$AdvancementType$values.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize AdvancementTypes", e);
        }
    }

    public static final Class<?> clazz$DisplayInfo = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementDisplay",
                    "advancements.DisplayInfo"
            )
    );

    public static final Constructor<?> constructor$DisplayInfo = requireNonNull(
            VersionHelper.isOrAbove1_20_3() ?
            ReflectionUtils.getConstructor(clazz$DisplayInfo, clazz$ItemStack, clazz$Component, clazz$Component, Optional.class, clazz$AdvancementType, boolean.class, boolean.class, boolean.class) :
            ReflectionUtils.getConstructor(clazz$DisplayInfo, clazz$ItemStack, clazz$Component, clazz$Component, clazz$ResourceLocation, clazz$AdvancementType, boolean.class, boolean.class, boolean.class)
    );

    public static final Class<?> clazz$Criterion = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.Criterion",
                    "advancements.Criterion"
            )
    );

    public static final Class<?> clazz$CriterionTrigger = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.CriterionTrigger",
                    "advancements.CriterionTrigger"
            )
    );

    public static final Class<?> clazz$CriterionTriggerInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.CriterionInstance",
                    "advancements.CriterionTriggerInstance"
            )
    );

    public static final Class<?> clazz$ImpossibleTrigger = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("advancements.critereon.CriterionTriggerImpossible", "advancements.criterion.CriterionTriggerImpossible"),
                    List.of("advancements.critereon.ImpossibleTrigger", "advancements.criterion.ImpossibleTrigger")
            )
    );

    public static final Constructor<?> constructor$ImpossibleTrigger = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ImpossibleTrigger)
    );

    public static final Class<?> clazz$ImpossibleTrigger$TriggerInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("advancements.critereon.CriterionTriggerImpossible$a", "advancements.criterion.CriterionTriggerImpossible$a"),
                    List.of("advancements.critereon.ImpossibleTrigger$TriggerInstance", "advancements.criterion.ImpossibleTrigger$TriggerInstance")
            )
    );

    public static final Constructor<?> constructor$Criterion = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getConstructor(clazz$Criterion, clazz$CriterionTrigger, clazz$CriterionTriggerInstance) :
            ReflectionUtils.getConstructor(clazz$Criterion, clazz$CriterionTriggerInstance)
    );

    public static final Constructor<?> constructor$ImpossibleTrigger$TriggerInstance = requireNonNull(
            ReflectionUtils.getConstructor(clazz$ImpossibleTrigger$TriggerInstance)
    );

    public static final Class<?> clazz$Advancement = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.Advancement",
                    "advancements.Advancement"
            )
    );

    public static final Constructor<?> constructor$Advancement = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
            ReflectionUtils.getConstructor(clazz$Advancement, Optional.class, Optional.class, clazz$AdvancementRewards, Map.class, clazz$AdvancementRequirements, boolean.class) :
            ReflectionUtils.getConstructor(clazz$Advancement, clazz$ResourceLocation, clazz$Advancement, clazz$DisplayInfo, clazz$AdvancementRewards, Map.class, String[][].class, boolean.class)
    );

    public static final Class<?> clazz$CriterionProgress = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.CriterionProgress",
                    "advancements.CriterionProgress"
            )
    );

    public static final Method method$AdvancementProgress$grantProgress = requireNonNull(
            ReflectionUtils.getMethod(clazz$AdvancementProgress, boolean.class, new String[]{"grantProgress", "a"}, String.class)
    );

    public static final Class<?> clazz$AdvancementHolder = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "advancements.AdvancementHolder",
                    "advancements.AdvancementHolder"
            ), VersionHelper.isOrAbove1_20_2()
    );

    public static final Constructor<?> constructor$AdvancementHolder = Optional.ofNullable(clazz$AdvancementHolder)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$ResourceLocation, clazz$Advancement))
            .orElse(null);

    public static final Class<?> clazz$FenceGateBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockFenceGate",
                    "world.level.block.FenceGateBlock"
            )
    );

    public static final Class<?> clazz$GameEvent = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.gameevent.GameEvent")
            )
    );

    public static final Class<?> clazz$HolderSet = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("core.HolderSet")
            )
    );

    public static final Method method$BlockStateBase$isBlock = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockStateBase, boolean.class, new String[]{"is", "a"}, clazz$Block)
    );

    public static final Class<?> clazz$Projectile = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.projectile.IProjectile",
                    "world.entity.projectile.Projectile"
            )
    );

    public static final Method method$BlockBehaviour$onProjectileHit = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"onProjectileHit", "a"}, clazz$Level, clazz$BlockState, clazz$BlockHitResult, clazz$Projectile)
    );

    public static final Field field$EnumProperty$values = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$EnumProperty, VersionHelper.isOrAbove1_21_2() ? List.class : ImmutableSet.class, 0)
    );

    public static final Method method$Block$setPlacedBy = requireNonNull(
            ReflectionUtils.getMethod(clazz$Block, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$LivingEntity, clazz$ItemStack)
    );

    public static final Class<?> clazz$ItemCost = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.trading.ItemCost",
                    "world.item.trading.ItemCost"
            ), VersionHelper.isOrAbove1_20_5()
    );

    public static final Class<?> clazz$MerchantOffer = requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.item.trading.MerchantRecipe",
            "world.item.trading.MerchantOffer"
    ));

    public static final Class<?> clazz$Merchant = requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.item.trading.IMerchant",
            "world.item.trading.Merchant"
    ));

    public static final Class<?> clazz$BlockEntityType = requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.level.block.entity.TileEntityTypes",
            "world.level.block.entity.BlockEntityType"
    ));

    public static final Method method$BlockGetter$getBlockFloorHeight = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockGetter, double.class, clazz$BlockPos)
    );

    public static final Method method$LivingEntity$getLocalBoundsForPose = requireNonNull(
            ReflectionUtils.getMethod(clazz$LivingEntity, clazz$AABB, clazz$Pose)
    );

    public static final Class<?> clazz$DismountHelper = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.vehicle.DismountUtil",
                    "world.entity.vehicle.DismountHelper"
            )
    );

    public static final Class<?> clazz$CollisionGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.ICollisionAccess",
                    "world.level.CollisionGetter"
            )
    );

    public static final Method method$DismountHelper$canDismountTo0 = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$DismountHelper, boolean.class, clazz$CollisionGetter, clazz$LivingEntity, clazz$AABB)
    );

    public static final Method method$DismountHelper$canDismountTo1 = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$DismountHelper, boolean.class, clazz$CollisionGetter, clazz$Vec3, clazz$LivingEntity, clazz$Pose)
    );

    public static final Class<?> clazz$WorldGenContext = MiscUtils.requireNonNullIf(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.chunk.status.WorldGenContext")), VersionHelper.isOrAbove1_20_5()
    );

    public static final Field field$ChunkMap$worldGenContext = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredField(clazz$ChunkMap, clazz$WorldGenContext, 0), VersionHelper.isOrAbove1_20_5()
    );

    public static final Field field$ChunkMap$chunkGenerator = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredField(clazz$ChunkMap, clazz$ChunkGenerator, 0), !VersionHelper.isOrAbove1_20_5()
    );

    public static final Class<?> clazz$BlockStateProvider = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider",
                    "world.level.levelgen.feature.stateproviders.BlockStateProvider"
            )
    );

    public static final Class<?> clazz$BlockStateProviderType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviders",
                    "world.level.levelgen.feature.stateproviders.BlockStateProviderType"
            )
    );

    public static final Class<?> clazz$Feature = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.WorldGenerator",
                    "world.level.levelgen.feature.Feature"
            )
    );

    public static final Class<?> clazz$EmptyBlockGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.BlockAccessAir",
                    "world.level.EmptyBlockGetter"
            )
    );

    public static final Method method$EmptyBlockGetter$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$EmptyBlockGetter, clazz$EmptyBlockGetter.arrayType())
    );

    public static final Object instance$EmptyBlockGetter$INSTANCE;

    static {
        try {
            Object[] values = (Object[]) method$EmptyBlockGetter$values.invoke(null);
            instance$EmptyBlockGetter$INSTANCE = values[0];
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init EmptyBlockGetter$INSTANCE", e);
        }
    }

    public static final Class<?> clazz$EntityDimensions = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntitySize",
                    "world.entity.EntityDimensions"
            )
    );

    public static final Field field$EntityType$dimensions = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$EntityType, clazz$EntityDimensions, 0)
    );

    public static final Field field$EntityDimensions$width = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$EntityDimensions, float.class, 0)
    );

    public static final Field field$EntityDimensions$height = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$EntityDimensions, float.class, 1)
    );

    public static final Field field$EntityDimensions$fixed = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$EntityDimensions, boolean.class, 0)
    );

    public static final Class<?> clazz$PlayerTeam = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.scores.ScoreboardTeam",
                    "world.scores.PlayerTeam"
            )
    );

    public static final Class<?> clazz$Rotations = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.Vector3f",
                    "core.Rotations"
            )
    );

    public static final Constructor<?> constructor$Rotations = requireNonNull(
            ReflectionUtils.getConstructor(clazz$Rotations, float.class, float.class, float.class)
    );

    public static final Object instance$ArmorStand$DEFAULT_HEAD_POSE;
    public static final Object instance$ArmorStand$DEFAULT_BODY_POSE;
    public static final Object instance$ArmorStand$DEFAULT_LEFT_ARM_POSE;
    public static final Object instance$ArmorStand$DEFAULT_RIGHT_ARM_POSE;
    public static final Object instance$ArmorStand$DEFAULT_LEFT_LEG_POSE;
    public static final Object instance$ArmorStand$DEFAULT_RIGHT_LEG_POSE;

    static {
        try {
            instance$ArmorStand$DEFAULT_HEAD_POSE = constructor$Rotations.newInstance(0.0F, 0.0F, 0.0F);
            instance$ArmorStand$DEFAULT_BODY_POSE = constructor$Rotations.newInstance(0.0F, 0.0F, 0.0F);
            instance$ArmorStand$DEFAULT_LEFT_ARM_POSE = constructor$Rotations.newInstance(-10.0F, 0.0F, -10.0F);
            instance$ArmorStand$DEFAULT_RIGHT_ARM_POSE = constructor$Rotations.newInstance(-15.0F, 0.0F, 10.0F);
            instance$ArmorStand$DEFAULT_LEFT_LEG_POSE = constructor$Rotations.newInstance(-1.0F, 0.0F, -1.0F);
            instance$ArmorStand$DEFAULT_RIGHT_LEG_POSE = constructor$Rotations.newInstance(1.0F, 0.0F, 1.0F);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init ArmorStand", e);
        }
    }

    public static final Field field$DedicatedServerProperties$enforceSecureProfile = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$DedicatedServerProperties, VersionHelper.isOrAbove1_21_11() ? new String[]{"enforceSecureProfile", "ag"}
                            : VersionHelper.isOrAbove1_21_9() ? new String[]{"enforceSecureProfile", "af"}
                            : VersionHelper.isOrAbove1_21_2() ? new String[]{"enforceSecureProfile", "X"}
                            : VersionHelper.isOrAbove1_21() ? new String[]{"enforceSecureProfile", "Y"}
                            : VersionHelper.isOrAbove1_20_5() ? new String[]{"enforceSecureProfile", "X"}
                            : new String[]{"enforceSecureProfile", "W"}
            )
    );

    public static final MethodHandle methodHandle$DedicatedServerProperties$enforceSecureProfileSetter = requireNonNull(
            ReflectionUtils.unreflectSetter(field$DedicatedServerProperties$enforceSecureProfile)
    ).asType(MethodType.methodType(void.class, Object.class, boolean.class));

    public static final Class<?> clazz$LevelCallback = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.entity.LevelCallback")
            )
    );

}
