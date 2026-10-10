package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.event.ChunkLifecycleEvent;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitChunkAccess;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.storage.LifecycleCachedStorage;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ImposterProtoChunkProxy;
import net.momirealms.craftengine.proxy.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ReadChunkProxy;
import net.momirealms.craftengine.proxy.spottedleaf.moonrise.patches.chunk_system.scheduling.task.TaskResultProxy;
import org.bukkit.World;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitChunkLifecycle {
    private static final ConcurrentHashMap<Object, LoadContext> LOADS = new ConcurrentHashMap<>();

    private BukkitChunkLifecycle() {
    }

    public static void initialize() {
        Objects.requireNonNull(TaskResultProxy.INSTANCE, "Moonrise task result proxy is unavailable");
        Objects.requireNonNull(ReadChunkProxy.INSTANCE, "Moonrise read chunk proxy is unavailable");
    }

    private static CEWorld world(Object level) {
        BukkitWorldManager manager = BukkitWorldManager.instance();
        if (manager == null || !manager.initialized()) return null;
        World world = LevelProxy.INSTANCE.getWorld(level);
        return world == null ? null : manager.getStorageWorld(world);
    }

    public static void start(Object[] args) {
        CEWorld world = world(args[0]);
        if (world == null || !(world.worldDataStorage() instanceof LifecycleCachedStorage storage)) {
            return;
        }
        ChunkPos pos = new ChunkPos((int) args[1], (int) args[2]);
        LOADS.put(args[4], new LoadContext(world, pos, storage, storage.bind(pos, args[3])));
        notifyLifecycle(world, pos, ChunkLifecycleEvent.Stage.LOAD_START);
    }

    public static Object context(Object task) {
        return LOADS.get(task);
    }

    public static void complete(Object task) {
        LOADS.remove(task);
    }

    public static void read(Object context, Object result) {
        if (context == null || result == null) return;
        try {
            Object data = TaskResultProxy.INSTANCE.left(result);
            if (data != null) preload((LoadContext) context, ReadChunkProxy.INSTANCE.protoChunk(data));
        } catch (Throwable t) {
            failed((LoadContext) context, t);
        }
    }

    public static void empty(Object task, Object chunk) {
        LoadContext context = LOADS.get(task);
        if (context == null) return;
        try {
            preload(context, chunk);
        } catch (Throwable t) {
            failed(context, t);
        }
    }

    private static void preload(LoadContext context, Object chunk) throws Exception {
        if (chunk == null) {
            return;
        }
        notifyLifecycle(context.world, context.pos, ChunkLifecycleEvent.Stage.DATA_READ);
        if (ImposterProtoChunkProxy.CLASS.isInstance(chunk)) {
            chunk = ImposterProtoChunkProxy.INSTANCE.getWrapped(chunk);
        }
        context.storage.preload(context.entry, context.world, context.pos, new BukkitChunkAccess(chunk));
    }

    private static void failed(LoadContext context, Throwable t) {
        CraftEngine.instance().logger().warn("Failed to preload lifecycle chunk " + context.world.name() + " " + context.pos, t);
    }

    public static void release(Object[] args) {
        CEWorld world = world(args[0]);
        if (world != null && world.worldDataStorage() instanceof LifecycleCachedStorage storage) {
            ChunkPos pos = new ChunkPos((int) args[1], (int) args[2]);
            storage.release(pos, args[3]);
            notifyLifecycle(world, pos, ChunkLifecycleEvent.Stage.HOLDER_UNLOAD);
        }
    }

    private static void notifyLifecycle(CEWorld world, ChunkPos pos, ChunkLifecycleEvent.Stage stage) {
        if (ChunkLifecycleEvent.HANDLERS.getRegisteredListeners().length != 0) {
            EventUtils.fireAndForget(new ChunkLifecycleEvent((World) world.world.platformWorld(), pos.x, pos.z, stage));
        }
    }

    public static void clear() {
        LOADS.clear();
    }

    private record LoadContext(CEWorld world, ChunkPos pos, LifecycleCachedStorage storage, LifecycleCachedStorage.Entry entry) {
    }
}
