package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.storage.DefaultStorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BukkitWorldManager implements WorldManager, Listener {
    private static BukkitWorldManager instance;
    private final BukkitCraftEngine plugin;
    private final Map<UUID, CEWorld> worlds;
    private CEWorld[] worldArray = new CEWorld[0];
    private final ReentrantReadWriteLock worldMapLock = new ReentrantReadWriteLock();
    private SchedulerTask tickTask;
    // cache
    private UUID lastVisitedUUID;
    private CEWorld lastVisitedWorld;
    private StorageAdaptor storageAdaptor;
    private boolean isTicking = false;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.worlds = new HashMap<>();
        this.storageAdaptor = new DefaultStorageAdaptor();
    }

    @Override
    public void setStorageAdaptor(@NotNull StorageAdaptor storageAdaptor) {
        this.storageAdaptor = storageAdaptor;
    }

    public static BukkitWorldManager instance() {
        return instance;
    }

    public CEWorld getWorld(World world) {
        return getWorld(world.getUID());
    }

    @Override
    public CEWorld getWorld(UUID uuid) {
        if (uuid.equals(this.lastVisitedUUID)) {
            return this.lastVisitedWorld;
        }
        this.worldMapLock.readLock().lock();
        try {
            CEWorld world = worlds.get(uuid);
            if (world != null) {
                this.lastVisitedUUID = uuid;
                this.lastVisitedWorld = world;
            }
            return world;
        } finally {
            this.worldMapLock.readLock().unlock();
        }
    }

    @Override
    public CEWorld[] getWorlds() {
        return this.worldArray;
    }

    private void resetWorldArray() {
        this.worldArray = this.worlds.values().toArray(new CEWorld[0]);
    }

    public void delayedInit() {
        // events and tasks
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        this.tickTask = this.plugin.scheduler().asyncRepeating(() -> {
            try {
                if (this.isTicking) {
                    return;
                }
                this.isTicking = true;
                for (CEWorld world : this.worldArray) {
                    world.tick();
                }
            } finally {
                this.isTicking = false;
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
        // load loaded chunks
        this.worldMapLock.writeLock().lock();
        try {
            for (World world : Bukkit.getWorlds()) {
                try {
                    CEWorld ceWorld = new BukkitCEWorld(new BukkitWorld(world), this.storageAdaptor);
                    this.worlds.put(world.getUID(), ceWorld);
                    this.resetWorldArray();
                    for (Chunk chunk : world.getLoadedChunks()) {
                        handleChunkLoad(ceWorld, chunk);
                    }
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Error loading world: " + world.getName(), e);
                }
            }
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (this.storageAdaptor instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
        if (this.tickTask != null && !this.tickTask.cancelled()) {
            this.tickTask.cancel();
        }
        for (World world : Bukkit.getWorlds()) {
            CEWorld ceWorld = getWorld(world.getUID());
            for (Chunk chunk : world.getLoadedChunks()) {
                handleChunkUnload(ceWorld, chunk);
            }
            try {
                ceWorld.worldDataStorage().close();
            } catch (IOException e) {
                this.plugin.logger().warn("Error unloading world: " + world.getName(), e);
            }
        }
        this.worlds.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        this.loadWorld(new BukkitWorld(event.getWorld()));
    }

    @Override
    public void loadWorld(net.momirealms.craftengine.core.world.World world) {
        this.worldMapLock.writeLock().lock();
        try {
            if (this.worlds.containsKey(world.uuid())) return;
            CEWorld ceWorld = new BukkitCEWorld(world, this.storageAdaptor);
            this.worlds.put(world.uuid(), ceWorld);
            this.resetWorldArray();
            for (Chunk chunk : ((World) world.platformWorld()).getLoadedChunks()) {
                handleChunkLoad(ceWorld, chunk);
            }
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
    }

    @Override
    public void loadWorld(CEWorld world) {
        this.worldMapLock.writeLock().lock();
        try {
            if (this.worlds.containsKey(world.world().uuid())) return;
            this.worlds.put(world.world().uuid(), world);
            this.resetWorldArray();
            for (Chunk chunk : ((World) world.world().platformWorld()).getLoadedChunks()) {
                handleChunkLoad(world, chunk);
            }
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
    }

    @Override
    public CEWorld createWorld(net.momirealms.craftengine.core.world.World world, WorldDataStorage storage) {
        return new BukkitCEWorld(world, storage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        unloadWorld(new BukkitWorld(event.getWorld()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldSave(WorldSaveEvent event) {
        for (CEWorld world : this.worldArray) {
            world.save();
        }
    }

    @Override
    public void unloadWorld(net.momirealms.craftengine.core.world.World world) {
        CEWorld ceWorld;
        this.worldMapLock.writeLock().lock();
        try {
            ceWorld = this.worlds.remove(world.uuid());
            if (ceWorld == null) {
                return;
            }
            if (ceWorld == this.lastVisitedWorld) {
                this.lastVisitedWorld = null;
                this.lastVisitedUUID = null;
            }
            this.resetWorldArray();
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
        for (Chunk chunk : ((World) world.platformWorld()).getLoadedChunks()) {
            handleChunkUnload(ceWorld, chunk);
        }
        try {
            ceWorld.worldDataStorage().close();
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to close world storage", e);
        }
    }

    @Override
    public <T> net.momirealms.craftengine.core.world.World wrap(T world) {
        if (world instanceof World w) {
            return new BukkitWorld(w);
        } else {
            throw new IllegalArgumentException(world.getClass() + " is not a Bukkit World");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        this.worldMapLock.readLock().lock();
        CEWorld world;
        try {
            world = worlds.get(event.getWorld().getUID());
            if (world == null) {
                return;
            }
        } finally {
            this.worldMapLock.readLock().unlock();
        }
        handleChunkLoad(world, event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        CEWorld world;
        this.worldMapLock.readLock().lock();
        try {
            world = worlds.get(event.getWorld().getUID());
            if (world == null) {
                return;
            }
        } finally {
            this.worldMapLock.readLock().unlock();
        }
        handleChunkUnload(world, event.getChunk());
    }

    private void handleChunkUnload(CEWorld world, Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        CEChunk ceChunk = world.getChunkAtIfLoaded(chunk.getX(), chunk.getZ());
        if (ceChunk != null) {
            if (ceChunk.dirty()) {
                try {
                    world.worldDataStorage().writeChunkAt(pos, ceChunk);
                    ceChunk.setDirty(false);
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write chunk tag at " + chunk.getX() + " " + chunk.getZ(), e);
                }
            }
            boolean unsaved = false;
            CESection[] ceSections = ceChunk.sections();
            Object worldServer = FastNMS.INSTANCE.field$CraftChunk$worldServer(chunk);
            Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(worldServer);
            Object levelChunk = FastNMS.INSTANCE.method$ServerChunkCache$getChunkAtIfLoadedMainThread(chunkSource, chunk.getX(), chunk.getZ());
            Object[] sections = FastNMS.INSTANCE.method$ChunkAccess$getSections(levelChunk);
            for (int i = 0; i < ceSections.length; i++) {
                CESection ceSection = ceSections[i];
                Object section = sections[i];
                WorldStorageInjector.uninjectLevelChunkSection(section);
                if (Config.restoreVanillaBlocks()) {
                    if (!ceSection.statesContainer().isEmpty()) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < 16; y++) {
                                    ImmutableBlockState customState = ceSection.getBlockState(x, y, z);
                                    if (!customState.isEmpty() && customState.vanillaBlockState() != null) {
                                        FastNMS.INSTANCE.method$LevelChunkSection$setBlockState(section, x, y, z, customState.vanillaBlockState().handle(), false);
                                        unsaved = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (unsaved /*&& !FastNMS.INSTANCE.method$LevelChunk$isUnsaved(levelChunk)*/) {
                FastNMS.INSTANCE.method$LevelChunk$markUnsaved(levelChunk);
            }
            ceChunk.unload();
        }
    }

    private void handleChunkLoad(CEWorld ceWorld, Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        if (ceWorld.isChunkLoaded(pos.longKey)) return;
        CEChunk ceChunk;
        try {
            ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld, pos);
            try {
                CESection[] ceSections = ceChunk.sections();
                Object worldServer = FastNMS.INSTANCE.field$CraftChunk$worldServer(chunk);
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(worldServer);
                Object levelChunk = FastNMS.INSTANCE.method$ServerChunkCache$getChunkAtIfLoadedMainThread(chunkSource, chunk.getX(), chunk.getZ());
                Object[] sections = FastNMS.INSTANCE.method$ChunkAccess$getSections(levelChunk);
                synchronized (sections) {
                    for (int i = 0; i < ceSections.length; i++) {
                        CESection ceSection = ceSections[i];
                        Object section = sections[i];
                        if (Config.syncCustomBlocks()) {
                            Object statesContainer = FastNMS.INSTANCE.field$LevelChunkSection$states(section);
                            Object data = CoreReflections.varHandle$PalettedContainer$data.get(statesContainer);
                            Object palette = CoreReflections.field$PalettedContainer$Data$palette.get(data);
                            boolean requiresSync = false;
                            if (CoreReflections.clazz$SingleValuePalette.isInstance(palette)) {
                                Object onlyBlockState = CoreReflections.field$SingleValuePalette$value.get(palette);
                                if (BlockStateUtils.isCustomBlock(onlyBlockState)) {
                                    requiresSync = true;
                                }
                            } else if (CoreReflections.clazz$LinearPalette.isInstance(palette)) {
                                Object[] blockStates = (Object[]) CoreReflections.field$LinearPalette$values.get(palette);
                                for (Object blockState : blockStates) {
                                    if (blockState != null) {
                                        if (BlockStateUtils.isCustomBlock(blockState)) {
                                            requiresSync = true;
                                            break;
                                        }
                                    }
                                }
                            } else if (CoreReflections.clazz$HashMapPalette.isInstance(palette)) {
                                Object biMap = CoreReflections.field$HashMapPalette$values.get(palette);
                                Object[] blockStates = (Object[]) CoreReflections.field$CrudeIncrementalIntIdentityHashBiMap$keys.get(biMap);
                                for (Object blockState : blockStates) {
                                    if (blockState != null) {
                                        if (BlockStateUtils.isCustomBlock(blockState)) {
                                            requiresSync = true;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                requiresSync = true;
                            }
                            if (requiresSync) {
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int y = 0; y < 16; y++) {
                                            Object mcState = FastNMS.INSTANCE.method$LevelChunkSection$getBlockState(section, x, y, z);
                                            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(mcState);
                                            if (optionalCustomState.isPresent()) {
                                                ceSection.setBlockState(x, y, z, optionalCustomState.get());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (Config.restoreCustomBlocks()) {
                            if (!ceSection.statesContainer().isEmpty()) {
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int y = 0; y < 16; y++) {
                                            ImmutableBlockState customState = ceSection.getBlockState(x, y, z);
                                            if (!customState.isEmpty() && customState.customBlockState() != null) {
                                                FastNMS.INSTANCE.method$LevelChunkSection$setBlockState(section, x, y, z, customState.customBlockState().handle(), false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        int finalI = i;
                        WorldStorageInjector.injectLevelChunkSection(section, ceSection, ceChunk, new SectionPos(pos.x, ceChunk.sectionY(i), pos.z),
                                (injected) -> sections[finalI] = injected);
                    }
                }
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to restore chunk at " + chunk.getX() + " " + chunk.getZ(), e);
                return;
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to read chunk tag at " + chunk.getX() + " " + chunk.getZ(), e);
            return;
        }
        ceChunk.load();
    }
}
