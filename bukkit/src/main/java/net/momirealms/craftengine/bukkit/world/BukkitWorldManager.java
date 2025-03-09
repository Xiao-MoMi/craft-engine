package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BukkitInjector;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.serialization.ChunkSerializer;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BukkitWorldManager implements WorldManager, Listener {
    private static BukkitWorldManager instance;
    private final BukkitCraftEngine plugin;
    private final Map<UUID, CEWorld> worlds;
    private CEWorld[] worldArray;
    private final ReentrantReadWriteLock worldMapLock = new ReentrantReadWriteLock();
    private SchedulerTask tickTask;
    // cache
    private UUID lastVisitedUUID;
    private CEWorld lastVisitedWorld;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.worlds = new HashMap<>();
        resetWorldArray();
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

    private void resetWorldArray() {
        this.worldArray = this.worlds.values().toArray(new CEWorld[0]);
    }

    public void delayedInit() {
        // events and tasks
        Bukkit.getPluginManager().registerEvents(this, plugin.bootstrap());
        this.tickTask = plugin.scheduler().sync().runRepeating(() -> {
            for (CEWorld world : worldArray) {
                world.tick();
            }
        }, 1, 1);

        // load loaded chunks
        this.worldMapLock.writeLock().lock();
        try {
            for (World world : Bukkit.getWorlds()) {
                CEWorld ceWorld = new BukkitCEWorld(new BukkitWorld(world));
                this.worlds.put(world.getUID(), ceWorld);
                this.resetWorldArray();
                for (Chunk chunk : world.getLoadedChunks()) {
                    handleChunkLoad(ceWorld, chunk);
                }
            }
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (tickTask != null && !tickTask.cancelled()) {
            tickTask.cancel();
        }

        for (World world : Bukkit.getWorlds()) {
            CEWorld ceWorld = getWorld(world.getUID());
            for (Chunk chunk : world.getLoadedChunks()) {
                handleChunkUnload(ceWorld, chunk);
            }
        }
        this.worlds.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        CEWorld ceWorld = new BukkitCEWorld(new BukkitWorld(world));
        this.worldMapLock.writeLock().lock();
        try {
            if (this.worlds.containsKey(world.getUID())) return;
            this.worlds.put(event.getWorld().getUID(), ceWorld);
            this.resetWorldArray();
            for (Chunk chunk : world.getLoadedChunks()) {
                handleChunkLoad(ceWorld, chunk);
            }
        } finally {
            this.worldMapLock.writeLock().unlock();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        CEWorld ceWorld;
        this.worldMapLock.writeLock().lock();
        try {
            ceWorld = this.worlds.remove(world.getUID());
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
        for (Chunk chunk : world.getLoadedChunks()) {
            handleChunkUnload(ceWorld, chunk);
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
            try {
                world.worldDataStorage().writeChunkTagAt(pos, ChunkSerializer.serialize(ceChunk));
            } catch (IOException e) {
                plugin.logger().warn("Failed to write chunk tag at " + chunk.getX() + " " + chunk.getZ(), e);
                return;
            } finally {
                if (ConfigManager.restoreVanillaBlocks()) {
                    try {
                        CESection[] ceSections = ceChunk.sections();
                        Object worldServer = Reflections.field$CraftChunk$worldServer.get(chunk);
                        Object chunkSource = Reflections.field$ServerLevel$chunkSource.get(worldServer);
                        Object levelChunk = Reflections.method$ServerChunkCache$getChunkAtIfLoadedMainThread.invoke(chunkSource, chunk.getX(), chunk.getZ());
                        Object[] sections = (Object[]) Reflections.field$ChunkAccess$sections.get(levelChunk);
                        for (int i = 0; i < ceSections.length; i++) {
                            CESection ceSection = ceSections[i];
                            Object section = sections[i];
                            BukkitInjector.uninjectLevelChunkSection(section);
                            if (ceSection.statesContainer().isEmpty()) continue;
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    for (int y = 0; y < 16; y++) {
                                        ImmutableBlockState customState = ceSection.getBlockState(x, y, z);
                                        if (customState != null && customState.vanillaBlockState() != null) {
                                            Reflections.method$LevelChunkSection$setBlockState.invoke(section, x, y, z, customState.vanillaBlockState().handle(), false);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ReflectiveOperationException e) {
                        plugin.logger().warn("Failed to restore chunk at " + chunk.getX() + " " + chunk.getZ(), e);
                    }
                }
            }
            ceChunk.unload();
        }
    }

    private void handleChunkLoad(CEWorld ceWorld, Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        if (ceWorld.isChunkLoaded(pos.longKey)) return;
        CEChunk ceChunk;
        try {
            CompoundTag chunkNbt = ceWorld.worldDataStorage().readChunkTagAt(pos);
            if (chunkNbt != null) {
                ceChunk = ChunkSerializer.deserialize(ceWorld, pos, chunkNbt);
            } else {
                ceChunk = new CEChunk(ceWorld, pos);
            }
            try {
                CESection[] ceSections = ceChunk.sections();
                Object worldServer = Reflections.field$CraftChunk$worldServer.get(chunk);
                Object chunkSource = Reflections.field$ServerLevel$chunkSource.get(worldServer);
                Object levelChunk = Reflections.method$ServerChunkCache$getChunkAtIfLoadedMainThread.invoke(chunkSource, chunk.getX(), chunk.getZ());
                Object[] sections = (Object[]) Reflections.field$ChunkAccess$sections.get(levelChunk);
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    if (ConfigManager.restoreCustomBlocks()) {
                        if (!ceSection.statesContainer().isEmpty()) {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    for (int y = 0; y < 16; y++) {
                                        ImmutableBlockState customState = ceSection.getBlockState(x, y, z);
                                        if (customState != null && customState.customBlockState() != null) {
                                            Reflections.method$LevelChunkSection$setBlockState.invoke(section, x, y, z, customState.customBlockState().handle(), false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    BukkitInjector.injectLevelChunkSection(section, ceSection, ceWorld, new SectionPos(pos.x, ceChunk.sectionY(i), pos.z));
                }
                if (ConfigManager.enableRecipeSystem()) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> blockEntities = (Map<Object, Object>) Reflections.field$ChunkAccess$blockEntities.get(levelChunk);
                    for (Object blockEntity : blockEntities.values()) {
                        BukkitInjector.injectCookingBlockEntity(blockEntity);
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
