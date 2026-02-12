package net.momirealms.craftengine.bukkit.plugin.reflection.paper;

import com.google.gson.Gson;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public final class PaperReflections {
    private PaperReflections() {}

    public static final Class<?> clazz$AdventureComponent = requireNonNull(
            ReflectionUtils.getClazz("net{}kyori{}adventure{}text{}Component".replace("{}", "."))
    );

    public static final Field field$AsyncChatDecorateEvent$originalMessage = requireNonNull(
            ReflectionUtils.getDeclaredField(AsyncChatDecorateEvent.class, clazz$AdventureComponent, 0)
    );

    public static final Field field$AsyncChatDecorateEvent$result = requireNonNull(
            ReflectionUtils.getDeclaredField(AsyncChatDecorateEvent.class, clazz$AdventureComponent, 1)
    );

    public static final Class<?> clazz$ComponentSerializer = requireNonNull(
            ReflectionUtils.getClazz("net{}kyori{}adventure{}text{}serializer{}ComponentSerializer".replace("{}", "."))
    );

    public static final Class<?> clazz$GsonComponentSerializer = requireNonNull(
            ReflectionUtils.getClazz("net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer".replace("{}", "."))
    );

    public static final Class<?> clazz$GsonComponentSerializer$Builder = requireNonNull(
            ReflectionUtils.getClazz("net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer$Builder".replace("{}", "."))
    );

    public static final Method method$GsonComponentSerializer$builder = requireNonNull(
            ReflectionUtils.getMethod(clazz$GsonComponentSerializer, clazz$GsonComponentSerializer$Builder)
    );

    public static final Method method$GsonComponentSerializer$Builder$build = requireNonNull(
            ReflectionUtils.getMethod(clazz$GsonComponentSerializer$Builder, clazz$GsonComponentSerializer)
    );

    public static final Method method$ComponentSerializer$serialize = requireNonNull(
            ReflectionUtils.getMethod(clazz$ComponentSerializer, Object.class, new String[] {"serialize"}, clazz$AdventureComponent)
    );

    public static final Method method$ComponentSerializer$deserialize = requireNonNull(
            ReflectionUtils.getMethod(clazz$ComponentSerializer, Object.class, new String[] {"deserialize"}, Object.class)
    );

    public static final Method method$AsyncChatDecorateEvent$result = requireNonNull(
            ReflectionUtils.getMethod(AsyncChatDecorateEvent.class, void.class, clazz$AdventureComponent)
    );

    public static final Class<?> clazz$EntityLookup = requireNonNull(
            ReflectionUtils.getClazz(
                    "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup",
                    "io.papermc.paper.chunk.system.entity.EntityLookup"
            )
    );

    public static final Method method$Level$moonrise$getEntityLookup = requireNonNull(
            ReflectionUtils.getMethod(VersionHelper.isOrAbove1_21() ? CoreReflections.clazz$Level : CoreReflections.clazz$ServerLevel, clazz$EntityLookup)
    );

    public static final Method method$EntityLookup$get = requireNonNull(
            ReflectionUtils.getMethod(
                    clazz$EntityLookup, CoreReflections.clazz$Entity, int.class
            )
    );

    public static final Method method$GsonComponentSerializer$serializer = requireNonNull(
            ReflectionUtils.getMethod(clazz$GsonComponentSerializer, Gson.class)
    );

    public static final Object instance$GsonComponentSerializer;
    public static final Gson instance$GsonComponentSerializer$Gson;

    static {
        try {
            Object builder = method$GsonComponentSerializer$builder.invoke(null);
            instance$GsonComponentSerializer = method$GsonComponentSerializer$Builder$build.invoke(builder);
            instance$GsonComponentSerializer$Gson = (Gson) method$GsonComponentSerializer$serializer.invoke(instance$GsonComponentSerializer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$SignChangeEvent$line = requireNonNull(
            ReflectionUtils.getMethod(CraftBukkitReflections.clazz$SignChangeEvent, void.class, int.class, clazz$AdventureComponent)
    );

    public static final Method method$BookMeta$page = requireNonNull(
            ReflectionUtils.getMethod(CraftBukkitReflections.clazz$BookMeta, void.class, int.class, clazz$AdventureComponent)
    );

    public static final Class<?> clazz$RegionizedPlayerChunkLoader$PlayerChunkLoaderData = requireNonNull(
            ReflectionUtils.getClazz(
                    "ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData",
                    "io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader$PlayerChunkLoaderData"
            )
    );

    public static final Field field$ServerPlayer$chunkLoader = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    CoreReflections.clazz$ServerPlayer, PaperReflections.clazz$RegionizedPlayerChunkLoader$PlayerChunkLoaderData, 0
            )
    );

    public static final Field field$RegionizedPlayerChunkLoader$PlayerChunkLoaderData$sentChunks = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$RegionizedPlayerChunkLoader$PlayerChunkLoaderData, LongOpenHashSet.class, 0
            )
    );

    public static final Field field$EntityLookup$worldCallback = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$EntityLookup, CoreReflections.clazz$LevelCallback, 0
            )
    );

    public static final MethodHandle methodHandle$EntityLookup$worldCallbackGetter;
    public static final MethodHandle methodHandle$EntityLookup$worldCallbackSetter;

    static {
        try {
            methodHandle$EntityLookup$worldCallbackGetter = ReflectionUtils.unreflectGetter(field$EntityLookup$worldCallback)
                    .asType(MethodType.methodType(Object.class, Object.class));
            methodHandle$EntityLookup$worldCallbackSetter = requireNonNull(ReflectionUtils.unreflectSetter(field$EntityLookup$worldCallback))
                    .asType(MethodType.methodType(void.class, Object.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
