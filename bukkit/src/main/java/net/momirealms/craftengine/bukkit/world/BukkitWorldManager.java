package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.paper.PaperReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ConcurrentUUID2ReferenceChainedHashTable;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
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
import java.util.Optional;
import java.util.UUID;

public class BukkitWorldManager implements WorldManager, Listener {
    private static BukkitWorldManager instance;
    private final BukkitCraftEngine plugin;
    private final ConcurrentUUID2ReferenceChainedHashTable<CEWorld> worlds;
    private CEWorld[] worldArray;
    private StorageAdaptor storageAdaptor;
    private boolean initialized = false;
    private UUID lastWorldUUID = null;
    private CEWorld lastWorld = null;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.worlds = ConcurrentUUID2ReferenceChainedHashTable.createWithCapacity(10, 0.5f);
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
        if (uuid == this.lastWorldUUID || uuid.equals(this.lastWorldUUID)) {
            return this.lastWorld;
        }
        CEWorld world = this.worlds.get(uuid);
        if (world != null) {
            this.lastWorldUUID = uuid;
            this.lastWorld = world;
        } else {
            World bukkitWorld = Bukkit.getWorld(uuid);
            if (bukkitWorld != null) {
                world = this.loadWorld(wrap(bukkitWorld));
            }
        }
        return world;
    }

    @Override
    public CEWorld[] getWorlds() {
        return this.worldArray;
    }

    private void resetWorldArray() {
        this.worldArray = this.worlds.values().toArray(new CEWorld[0]);
    }

    public void delayedInit() {
        // 此时大概率为空，暂且保留代码
        for (World world : Bukkit.getWorlds()) {
            BukkitWorld wrappedWorld = wrap(world);
            try {
                CEWorld ceWorld = this.worlds.computeIfAbsent(world.getUID(), k -> new BukkitCEWorld(wrappedWorld, this.storageAdaptor));
                injectChunkGenerator(ceWorld);
                for (Chunk chunk : world.getLoadedChunks()) {
                    handleChunkLoad(ceWorld, chunk, false);
                    CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getChunkKey());
                    if (loadedChunk != null) {
                        loadedChunk.setEntitiesLoaded(true);
                    }
                }
                ceWorld.setTicking(true);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Error loading world: " + world.getName(), e);
            }
        }
        this.resetWorldArray();
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        this.initialized = true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (this.storageAdaptor instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
        for (World world : Bukkit.getWorlds()) {
            CEWorld ceWorld = getWorld(world.getUID());
            ceWorld.setTicking(false);
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
        this.lastWorld = null;
        this.lastWorldUUID = null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        UUID uuid = world.getUID();
        if (this.worlds.containsKey(uuid)) return;
        CEWorld ceWorld = new BukkitCEWorld(wrap(world), this.storageAdaptor);
        this.worlds.put(uuid, ceWorld);
        this.resetWorldArray();
        this.injectChunkGenerator(ceWorld);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        UUID uuid = world.getUID();
        if (this.worlds.containsKey(uuid)) {
            CEWorld ceWorld = this.worlds.get(uuid);
            for (Chunk chunk : world.getLoadedChunks()) {
                handleChunkLoad(ceWorld, chunk, true);
                CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getChunkKey());
                if (loadedChunk != null) {
                    loadedChunk.setEntitiesLoaded(true);
                }
            }
            ceWorld.setTicking(true);
        } else {
            this.loadWorld(wrap(world));
        }
    }

    @Override
    public CEWorld loadWorld(net.momirealms.craftengine.core.world.World world) {
        UUID uuid = world.uuid();
        if (this.worlds.containsKey(uuid)) {
            return this.worlds.get(uuid);
        }
        CEWorld ceWorld = new BukkitCEWorld(world, this.storageAdaptor);
        this.worlds.put(uuid, ceWorld);
        this.resetWorldArray();
        this.injectChunkGenerator(ceWorld);
        for (Chunk chunk : ((World) world.platformWorld()).getLoadedChunks()) {
            handleChunkLoad(ceWorld, chunk, false);
        }
        ceWorld.setTicking(true);
        return ceWorld;
    }

    @Override
    public void loadWorld(CEWorld world, boolean forceInit) {
        UUID uuid = world.world().uuid();
        if (this.worlds.containsKey(uuid)) {
            if (!forceInit) {
                return;
            }
        }
        this.worlds.put(uuid, world);
        this.resetWorldArray();
        this.injectChunkGenerator(world);
        for (Chunk chunk : ((World) world.world().platformWorld()).getLoadedChunks()) {
            handleChunkLoad(world, chunk, false);
        }
        world.setTicking(true);
    }

    private void injectChunkGenerator(CEWorld world) {
        Object serverLevel = world.world.serverWorld();
        injectWorldCallback(serverLevel);
        Object serverChunkCache = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
        Object chunkMap = FastNMS.INSTANCE.field$ServerChunkCache$chunkMap(serverChunkCache);
        FastNMS.INSTANCE.injectedWorldGen(world, chunkMap);
    }

    // 用于从实体tick列表中移除家具实体以降低遍历开销
    private void injectWorldCallback(Object serverLevel) {
        try {
            Object entityLookup = FastNMS.INSTANCE.method$ServerLevel$getEntityLookup(serverLevel);
            Object worldCallback = PaperReflections.methodHandle$EntityLookup$worldCallbackGetter.invokeExact(entityLookup);
            Object injectedWorldCallback = FastNMS.INSTANCE.createInjectedEntityCallbacks(worldCallback, entityLookup);
            if (worldCallback.equals(injectedWorldCallback)) return;
            PaperReflections.methodHandle$EntityLookup$worldCallbackSetter.invokeExact(entityLookup, injectedWorldCallback);
        } catch (Throwable e) {
            this.plugin.logger().warn( "Failed to inject world callback", e);
        }
    }

    @Override
    public CEWorld createWorld(net.momirealms.craftengine.core.world.World world, WorldDataStorage storage) {
        return new BukkitCEWorld(world, storage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        unloadWorld(wrap(event.getWorld()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldSave(WorldSaveEvent event) {
        for (CEWorld world : this.worldArray) {
            world.save();
        }
    }

    @Override
    public void unloadWorld(net.momirealms.craftengine.core.world.World world) {
        UUID uuid = world.uuid();
        CEWorld ceWorld = this.worlds.remove(uuid);
        if (ceWorld == null) {
            return;
        }
        this.resetWorldArray();
        ceWorld.setTicking(false);
        for (Chunk chunk : ((World) world.platformWorld()).getLoadedChunks()) {
            handleChunkUnload(ceWorld, chunk);
        }
        if (uuid.equals(this.lastWorldUUID)) {
            this.lastWorld = null;
            this.lastWorldUUID = null;
        }
        try {
            ceWorld.worldDataStorage().close();
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to close world storage", e);
        }
    }

    public boolean initialized() {
        return initialized;
    }

    @Override
    public <T> BukkitWorld wrap(T world) {
        if (world instanceof World w) {
            return BukkitAdaptors.adapt(w);
        } else {
            throw new IllegalArgumentException(world.getClass() + " is not a Bukkit World");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        CEWorld world = worlds.get(event.getWorld().getUID());
        if (world == null) {
            return;
        }
        handleChunkLoad(world, event.getChunk(), event.isNewChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        CEWorld world = worlds.get(event.getWorld().getUID());
        if (world == null) {
            return;
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
            synchronized (sections) {
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
                                        if (!customState.isEmpty()) {
                                            BlockStateWrapper wrapper = customState.restoreBlockState();
                                            if (wrapper != null) {
                                                FastNMS.INSTANCE.method$LevelChunkSection$setBlockState(section, x, y, z, wrapper.literalObject(), false);
                                                unsaved = true;
                                            }
                                        }
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
            ceChunk.deactivateAllBlockEntities();
        }
    }

    public void handleChunkGenerate(CEWorld ceWorld, ChunkPos chunkPos, Object chunkAccess) {
        if (ceWorld.isChunkLoaded(chunkPos.longKey)) return;
        Object[] sections = FastNMS.INSTANCE.method$ChunkAccess$getSections(chunkAccess);
        CEChunk ceChunk;
        try {
            ceChunk = ceWorld.worldDataStorage().readNewChunkAt(ceWorld, chunkPos);
            CESection[] ceSections = ceChunk.sections();
            synchronized (sections) {
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    int finalI = i;
                    WorldStorageInjector.injectLevelChunkSection(section, ceSection, ceChunk, new SectionPos(chunkPos.x, ceChunk.sectionY(i), chunkPos.z),
                            (injected) -> sections[finalI] = injected);
                }
            }
            ceChunk.load();
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to read new chunk at " + chunkPos.x + " " + chunkPos.z, e);
        }
    }

    public void addCraftEngineDecorations(CEWorld ceWorld, Object chunkGenerator, Object worldGenLevel, Object chunkAccess, Object structureManager) {

    }

    private void handleChunkLoad(CEWorld ceWorld, Chunk chunk, boolean isNew) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        CEChunk chunkAtIfLoaded = ceWorld.getChunkAtIfLoaded(chunkPos.longKey);
        if (chunkAtIfLoaded != null) {
            if (isNew) {
                chunkAtIfLoaded.activateAllBlockEntities();
            }
            return;
        }
        CEChunk ceChunk;
        try {
            ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld, chunkPos);
            try {
                CESection[] ceSections = ceChunk.sections();
                Object worldServer = FastNMS.INSTANCE.field$CraftChunk$worldServer(chunk);
                Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(worldServer);
                Object lightEngine = FastNMS.INSTANCE.method$ChunkSource$getLightEngine(chunkSource);
                Object levelChunk = FastNMS.INSTANCE.method$ServerChunkCache$getChunkAtIfLoadedMainThread(chunkSource, chunkX, chunkZ);
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
                            boolean isEmptyBefore = FastNMS.INSTANCE.method$LevelSection$hasOnlyAir(section);
                            int sectionY = ceSection.sectionY;
                            // 有自定义方块
                            PalettedContainer<ImmutableBlockState> palettedContainer = ceSection.statesContainer();
                            if (!palettedContainer.isEmpty()) {
                                if (isEmptyBefore) {
                                    FastNMS.INSTANCE.method$LightEventListener$updateSectionStatus(lightEngine, FastNMS.INSTANCE.method$SectionPos$of(chunkX, sectionY, chunkZ), false);
                                }
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int y = 0; y < 16; y++) {
                                            ImmutableBlockState customState = palettedContainer.get(x, y, z);
                                            if (!customState.isEmpty() && customState.customBlockState() != null) {
                                                Object newState = customState.customBlockState().literalObject();
                                                Object previous = FastNMS.INSTANCE.method$LevelChunkSection$setBlockState(section, x, y, z, newState, false);
                                                if (newState != previous && FastNMS.INSTANCE.method$LightEngine$hasDifferentLightProperties(newState, previous)) {
                                                    FastNMS.INSTANCE.method$ThreadedLevelLightEngine$checkBlock(lightEngine, LocationUtils.toBlockPos(chunkX * 16 + x, sectionY * 16 + y, chunkZ * 16 + z));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        int finalI = i;
                        WorldStorageInjector.injectLevelChunkSection(section, ceSection, ceChunk, new SectionPos(chunkPos.x, ceChunk.sectionY(i), chunkPos.z),
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
        ceChunk.activateAllBlockEntities();
    }
}
