package net.momirealms.craftengine.bukkit.world;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BiomeFilterGenerator;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitCEChunk;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitChunkAccess;
import net.momirealms.craftengine.bukkit.world.gen.ConditionalFeature;
import net.momirealms.craftengine.bukkit.world.gen.CraftEngineFeatures;
import net.momirealms.craftengine.bukkit.world.gen.InjectedChunkGenerator;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.ChunkGenerationStage;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftChunkProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.lithium.chunk.LithiumHashPaletteProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.SectionPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ChunkMapProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ThreadedLevelLightEngineProxy;
import net.momirealms.craftengine.proxy.minecraft.util.CrudeIncrementalIntIdentityHashBiMapProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ChunkPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.status.WorldGenContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.ConfiguredFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacedFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementModifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LightEventListenerProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.EntityLookupProxy;
import net.momirealms.craftengine.proxy.universeprojects.util.palette.CompactHashPaletteProxy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.*;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class BukkitWorldManager implements WorldManager, Listener {
    private static BukkitWorldManager instance;
    private final BukkitCraftEngine plugin;
    private boolean initialized = false;
    // loaded worlds
    private final ConcurrentChainedUUID2ReferenceHashTable<BukkitWorld> loadedWorlds;
    private final Cache<UUID, BukkitWorld> unloadedWorlds = Caffeine.newBuilder()
            .expireAfterWrite(Duration.of(15, ChronoUnit.SECONDS))
            .executor(CraftEngine.instance().scheduler().async())
            .scheduler(Scheduler.systemScheduler())
            .removalListener((UUID uuid, BukkitWorld world, RemovalCause cause) -> {
                World bukkitWorld = world.bukkitWorld();
                if (bukkitWorld != null) {
                    // clear ce world
                    CraftWorldProxy.INSTANCE.setWorldBorder(bukkitWorld, null);
                }
                if (Config.fixWorldMemoryLeak()) {
                    if (bukkitWorld != null) {
                        CraftWorldProxy.INSTANCE.setWorld(bukkitWorld, null);
                    }
                }
                Debugger.CHUNK.debug(() -> "World '" + world.name() + "' has been closed");
            })
            .initialCapacity(16)
            .build();
    private StorageAdaptor storageAdaptor;
    // parsers
    private final ConfigParser configuredFeatureParser = new ConfiguredFeatureParser();
    private final ConfigParser placedFeatureParser = new PlacedFeatureParser();
    // features
    private final Map<Key, Object> configuredFeatures  = new ConcurrentHashMap<>();
    private final Map<Key, Object> placedFeatures = new ConcurrentHashMap<>();
    private List<ConditionalFeature> customPlacedFeatures = List.of();
    private List<Suggestion> cachedConfiguredFeaturesSuggestion = List.of();
    public long lastReloadFeatureTime;
    private volatile boolean disabled;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        this.plugin = plugin;
        this.loadedWorlds = ConcurrentChainedUUID2ReferenceHashTable.createWithCapacity(16);
        this.storageAdaptor = new BukkitStorageAdaptor();
        instance = this;
    }

    public static BukkitWorldManager instance() {
        return instance;
    }

    public boolean initialized() {
        return this.initialized;
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.configuredFeatureParser, this.placedFeatureParser};
    }

    @Override
    public void load() {
        this.cachedConfiguredFeaturesSuggestion = RegistryProxy.INSTANCE.keySet(RegistryUtils.lookupOrThrow(RegistriesProxy.CONFIGURED_FEATURE))
                .stream()
                .map(it -> Suggestion.suggestion(it.toString()))
                .toList();
    }

    @Override
    public void unload() {
        this.configuredFeatures.clear();
        this.placedFeatures.clear();
    }

    @Override
    public void delayedInit() {
        // 此时大概率为空，暂且保留代码
        for (World world : Bukkit.getWorlds()) {
            try {
                BukkitWorld bukkitWorld = ensureStorageWorld(world);
                handleWorldLoad(bukkitWorld);
            } catch (Throwable e) {
                this.plugin.logger().warn("Failed to load world " + world.getName(), e);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        this.initialized = true;
    }

    /**
     * 区块数据预热回调，由织入 SerializableChunkData#read 的 advice 在区块加载工作线程上调用。
     * 此时原版区块数据（含 PDC）已读取完毕且严格早于 ChunkLoadEvent，
     * 串行完成 CE 区块数据的读取与缓存，MCA 与 PDC 两种存储格式均可被预热。
     * 启动期的区块加载因世界管理器尚未就绪而跳过。
     */
    public static void onChunkDataRead(Object[] args) {
        try {
            BukkitWorldManager manager = instance;
            if (manager == null) return;
            // 在初始化完成前不要进行任何预加载
            if (!manager.initialized) return;
            World bukkitWorld = LevelProxy.INSTANCE.getWorld(args[0]);
            if (bukkitWorld == null) return;
            CEWorld ceWorld = BukkitAdaptor.adapt(bukkitWorld).storageWorld();
            WorldDataStorage storage = ceWorld.worldDataStorage();
            Object nmsChunkPos = args[1];
            ChunkPos pos = new ChunkPos(ChunkPosProxy.INSTANCE.getX(nmsChunkPos), ChunkPosProxy.INSTANCE.getZ(nmsChunkPos));
            Object chunk = args[2];
            // 满状态区块会被包成 ImposterProtoChunk，PDC 在被包裹的 LevelChunk 上，必须解包
            if (ImposterProtoChunkProxy.CLASS.isInstance(chunk)) {
                chunk = ImposterProtoChunkProxy.INSTANCE.getWrapped(chunk);
            }
            storage.preloadChunkAt(ceWorld, pos, new BukkitChunkAccess(chunk));
        } catch (Throwable ignored) {
            // 预热失败由事件里的主线程读取兜底
        }
    }

    @Override
    public void disable() {
        if (this.disabled) return;
        this.disabled = true;
        HandlerList.unregisterAll(this);
        if (this.storageAdaptor instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
        for (World world : Bukkit.getWorlds()) {
            try {
                handleWorldUnload(BukkitAdaptor.adapt(world));
            } catch (Throwable e) {
                this.plugin.logger().warn("Failed to unload world " + world.getName(), e);
            }
        }
        this.loadedWorlds.clear();
    }

    public BukkitWorld ensureStorageWorld(org.bukkit.World world) {
        BukkitWorld bukkitWorld = this.injectCraftWorld(world);
        CEWorld storageWorld = bukkitWorld.storageWorld();
        if (storageWorld == null) {
            installStorageWorld(bukkitWorld, createStorageWorld(bukkitWorld));
        }
        return bukkitWorld;
    }

    /*

    Features, for FastNMS

     */

    public boolean hasCustomFeatures() {
        return !this.customPlacedFeatures.isEmpty();
    }

    public synchronized CraftEngineFeatures fetchFeatures(Object serverLevel) {
        World world = LevelProxy.INSTANCE.getWorld(serverLevel);
        String name = world.getName();
        Key dimension = KeyUtils.identifierToKey(ResourceKeyProxy.INSTANCE.getIdentifier(LevelProxy.INSTANCE.getDimension(serverLevel)));
        Object holder = LevelProxy.INSTANCE.getDimensionTypeRegistration(serverLevel);
        Key dimensionType = HolderProxy.ReferenceProxy.CLASS.isInstance(holder)
                ? KeyUtils.unwrapHolder(holder)
                : null;
        List<ConditionalFeature> features = new ArrayList<>();
        for (ConditionalFeature feature : this.customPlacedFeatures) {
            if (feature.isAllowedWorld(name) && feature.isAllowedDimension(dimension) && feature.isAllowedDimensionType(dimensionType)) {
                features.add(feature);
            }
        }
        return new CraftEngineFeatures(this.customPlacedFeatures, features);
    }

    public long lastReloadFeatureTime() {
        return this.lastReloadFeatureTime;
    }

    @Nullable
    public Object structureHolderById(Key id) {
        Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.STRUCTURE);
        return RegistryUtils.getHolder(registry, ResourceKeyProxy.INSTANCE.create(RegistriesProxy.STRUCTURE, KeyUtils.toIdentifier(id)));
    }

    @Nullable
    public Object configuredFeatureHolderById(Key id) {
        Object holder = this.configuredFeatures.get(id);
        if (holder == null) {
            Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.CONFIGURED_FEATURE);
            holder = RegistryUtils.getHolder(registry, FeatureUtils.createConfiguredFeatureKey(id));
        }
        return holder;
    }

    @Nullable
    public Object placedFeatureHolderById(Key id) {
        Object holder = this.placedFeatures.get(id);
        if (holder == null) {
            Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.PLACED_FEATURE);
            holder = RegistryUtils.getHolder(registry, FeatureUtils.createPlacedFeatureKey(id));
        }
        return holder;
    }

    public Collection<Suggestion> cachedConfiguredFeaturesSuggestion() {
        return this.cachedConfiguredFeaturesSuggestion;
    }

    /*

    worlds

     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        Debugger.CHUNK.debug(() -> "WorldUnloadEvent -> " + world.getName());
        handleWorldUnload(BukkitAdaptor.adapt(world));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        World world = event.getWorld();
        Debugger.CHUNK.debug(() -> "WorldSaveEvent -> " + world.getName());
        CEWorld ceWorld = BukkitAdaptor.adapt(world).storageWorld();
        // Unload后还会触发 save，避免无效区块遍历。
        if (ceWorld.isTicking()) {
            ceWorld.saveChunks();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        Debugger.CHUNK.debug(() -> "WorldInitEvent -> " + world.getName());
        ensureStorageWorld(event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        Debugger.CHUNK.debug(() -> "WorldLoadEvent -> " + world.getName());
        BukkitWorld bukkitWorld = ensureStorageWorld(event.getWorld());
        handleWorldLoad(bukkitWorld);
    }

    public void handleWorldLoad(BukkitWorld world) {
        CEWorld ceWorld = world.storageWorld();
        for (Chunk chunk : world.bukkitWorld().getLoadedChunks()) {
            if (VersionHelper.hasFoliaPatch) {
                this.plugin.scheduler().platform().run(() -> {
                    if (chunk.isLoaded()) {
                        handleChunkLoad(ceWorld, chunk);
                        CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getX(), chunk.getZ());
                        if (loadedChunk != null) {
                            loadedChunk.setEntitiesLoaded(true);
                        }
                    }
                }, world, chunk.getX(), chunk.getZ());
            } else {
                handleChunkLoad(ceWorld, chunk);
                CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getX(), chunk.getZ());
                if (loadedChunk != null) {
                    loadedChunk.setEntitiesLoaded(true);
                }
            }
        }
        ceWorld.setTicking(true);
        this.loadedWorlds.put(world.uuid(), world);
    }

    public void handleWorldUnload(BukkitWorld world) {
        this.unloadedWorlds.put(world.uuid(), world);
        this.loadedWorlds.remove(world.uuid());
        CEWorld ceWorld = world.storageWorld();
        ceWorld.setTicking(false);
        for (Chunk chunk : world.bukkitWorld().getLoadedChunks()) {
            try {
                handleChunkUnload(ceWorld, chunk);
            } catch (Throwable t) {
                this.plugin.logger().warn("Failed to unload chunk " + chunk.getX() + "," + chunk.getZ(), t);
            }
        }
        try {
            ceWorld.worldDataStorage().close();
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to close world storage", e);
        }
    }

    public BukkitWorld injectCraftWorld(World world) {
        Object worldBorder = CraftWorldProxy.INSTANCE.getWorldBorder(world);
        if (worldBorder instanceof BukkitWorld bukkitWorld) {
            return bukkitWorld;
        }
        BukkitWorld injectedWorld = FastNMS.INSTANCE.createInjectedWorld(world);
        CraftWorldProxy.INSTANCE.setWorldBorder(world, injectedWorld);
        if (VersionHelper.hasPaperPatch) {
            injectWorldGeneration(injectedWorld);
            if (!VersionHelper.hasFoliaPatch) {
                injectWorldCallback(injectedWorld.minecraftWorld());
            }
        }
        return injectedWorld;
    }

    public void installStorageWorld(BukkitWorld injectedWorld, CEWorld ceWorld) {
        CEWorld previous = injectedWorld.storageWorld();
        if (previous == ceWorld) {
            return;
        }
        if (previous != null) {
            previous.saveChunks();
            try {
                previous.worldDataStorage().close();
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to close world storage", e);
            }
        }
        ((WorldHolder) injectedWorld).setStorageWorld(ceWorld);
    }

    public CEWorld createStorageWorld(BukkitWorld injectedWorld) {
        return VersionHelper.hasFoliaPatch ? new FoliaCEWorld(injectedWorld, this.storageAdaptor) : new BukkitCEWorld(injectedWorld, this.storageAdaptor);
    }

    public CEWorld createStorageWorld(BukkitWorld injectedWorld, WorldDataStorage storage) {
        return VersionHelper.hasFoliaPatch ? new FoliaCEWorld(injectedWorld, storage) : new BukkitCEWorld(injectedWorld, storage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        BukkitWorld bukkitWorld = BukkitAdaptor.adapt(event.getWorld());
        handleChunkLoad(bukkitWorld.storageWorld(), event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        BukkitWorld bukkitWorld = BukkitAdaptor.adapt(event.getWorld());
        handleChunkUnload(bukkitWorld.storageWorld(), event.getChunk());
    }

    @Override
    public void setStorageAdaptor(@NotNull StorageAdaptor storageAdaptor) {
        this.storageAdaptor = Objects.requireNonNull(storageAdaptor);
    }

    @Override
    public @NotNull StorageAdaptor getStorageAdaptor() {
        return this.storageAdaptor;
    }

    @Override
    public BukkitWorld getWorldOffMainThread(UUID uuid) {
        BukkitWorld bukkitWorld = this.loadedWorlds.get(uuid);
        if (bukkitWorld == null) {
            bukkitWorld = this.unloadedWorlds.getIfPresent(uuid);
        }
        return bukkitWorld;
    }

    @Override
    public BukkitWorld getWorld(UUID uuid) {
        return this.loadedWorlds.get(uuid);
    }

    private void injectWorldGeneration(BukkitWorld world) {
        Object serverLevel = world.minecraftWorld();
        Object serverChunkCache = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        Object chunkMap = ServerChunkCacheProxy.INSTANCE.getChunkMap(serverChunkCache);
        if (VersionHelper.isOrAbove1_21_2) {
            Object worldGenContext = ChunkMapProxy.INSTANCE.getWorldGenContext(chunkMap);
            Object previousGenerator = WorldGenContextProxy.INSTANCE.getGenerator(worldGenContext);
            if (!(previousGenerator instanceof InjectedChunkGenerator)) {
                InjectedChunkGenerator customGenerator = FastNMS.INSTANCE.createInjectedChunkGenerator(world, previousGenerator);
                worldGenContext = WorldGenContextProxy.INSTANCE.newInstance(
                        WorldGenContextProxy.INSTANCE.getLevel(worldGenContext),
                        customGenerator,
                        WorldGenContextProxy.INSTANCE.getStructureManager(worldGenContext),
                        WorldGenContextProxy.INSTANCE.getLightEngine(worldGenContext),
                        WorldGenContextProxy.INSTANCE.getMainThreadExecutor(worldGenContext),
                        WorldGenContextProxy.INSTANCE.getUnsavedListener(worldGenContext)
                );
                ChunkMapProxy.INSTANCE.setWorldGenContext(chunkMap, worldGenContext);
            }
        } else if (VersionHelper.isOrAbove1_21) {
            Object worldGenContext = ChunkMapProxy.INSTANCE.getWorldGenContext(chunkMap);
            Object previousGenerator = WorldGenContextProxy.INSTANCE.getGenerator(worldGenContext);
            if (!(previousGenerator instanceof InjectedChunkGenerator)) {
                InjectedChunkGenerator customGenerator = FastNMS.INSTANCE.createInjectedChunkGenerator(world, previousGenerator);
                worldGenContext = WorldGenContextProxy.INSTANCE.newInstance(
                        WorldGenContextProxy.INSTANCE.getLevel(worldGenContext),
                        customGenerator,
                        WorldGenContextProxy.INSTANCE.getStructureManager(worldGenContext),
                        WorldGenContextProxy.INSTANCE.getLightEngine(worldGenContext),
                        WorldGenContextProxy.INSTANCE.getMainThreadMailBox(worldGenContext)
                );
                ChunkMapProxy.INSTANCE.setWorldGenContext(chunkMap, worldGenContext);
            }
        } else {
            Object generator = ChunkMapProxy.INSTANCE.getGenerator(chunkMap);
            if (!(generator instanceof InjectedChunkGenerator)) {
                InjectedChunkGenerator customGenerator = FastNMS.INSTANCE.createInjectedChunkGenerator(world, generator);
                ChunkMapProxy.INSTANCE.setGenerator(chunkMap, customGenerator);
            }
        }
    }

    // 用于从实体tick列表中移除家具实体以降低遍历开销
    private void injectWorldCallback(Object serverLevel) {
        Object entityLookup = LevelUtils.getEntityLookup(serverLevel);
        Object worldCallback = EntityLookupProxy.INSTANCE.getWorldCallback(entityLookup);
        if (!(worldCallback instanceof InjectedWorldCallback)) {
            Object injectedWorldCallback = FastNMS.INSTANCE.createInjectedWorldCallbacks(worldCallback, entityLookup);
            EntityLookupProxy.INSTANCE.setWorldCallback(entityLookup, injectedWorldCallback);
        }
    }

    private void handleChunkUnload(CEWorld world, Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        CEChunk ceChunk = world.getChunkAtIfLoaded(chunk.getX(), chunk.getZ());
        if (ceChunk != null) {
            if (ceChunk.isUnsaved()) {
                try {
                    world.worldDataStorage().writeChunkAt(pos, ceChunk);
                    ceChunk.setUnsaved(false);
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write chunk tag at " + chunk.getX() + " " + chunk.getZ(), e);
                }
            }

            boolean unsaved = false;

            Object levelChunk = null;
            if (ceChunk instanceof BukkitCEChunk bukkitCEChunk) {
                net.momirealms.craftengine.core.world.chunk.Chunk chunkAccess = bukkitCEChunk.chunkAccess();
                if (chunkAccess != null) {
                    levelChunk = chunkAccess.minecraftChunk();
                    bukkitCEChunk.setChunkAccess(null);
                }
            }
            if (levelChunk == null) {
                Object worldServer = CraftChunkProxy.INSTANCE.getWorld(chunk);
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(worldServer);
                levelChunk = LevelUtils.getChunkAtIfLoaded(chunkSource, chunk.getX(), chunk.getZ());
            }

            CESection[] ceSections = ceChunk.sections();
            Object[] sections = ChunkAccessProxy.INSTANCE.getSections(levelChunk);
            boolean restore = world.settings.restoreVanillaBlocksOnChunkLoad;
            synchronized (sections) {
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    WorldStorageInjector.uninject(section);
                    if (restore && !ceSection.isEmpty()) {
                        PalettedContainer<ImmutableBlockState> statesContainer = ceSection.statesContainer;
                        for (int index = 0; index < 4096; index++) {
                            ImmutableBlockState customState = statesContainer.get(index);
                            if (!customState.isEmpty()) {
                                BlockStateWrapper wrapper = customState.restoreBlockState();
                                if (wrapper != null) {
                                    LevelChunkSectionProxy.INSTANCE.setBlockState(section, index & 15, index >> 8, (index >> 4) & 15, wrapper.minecraftState(), false);
                                    unsaved = true;
                                }
                            }
                        }
                    }
                }
            }

            if (unsaved /*&& !ChunkAccessProxy.INSTANCE.isUnsaved(levelChunk)*/) {
                if (VersionHelper.isOrAbove1_21_2) {
                    LevelChunkProxy.INSTANCE.markUnsaved(levelChunk);
                } else {
                    ChunkAccessProxy.INSTANCE.setUnsaved(levelChunk, true);
                }
            }

            ceChunk.unload();
            ceChunk.deactivateAllBlockEntities();
        }
    }

    // for FastNMS chunk generator
    public Runnable handleChunkGenerate(BukkitWorld world, ChunkPos chunkPos, Object chunkAccess, ChunkGenerationStage stage) {
        if (!stage.enabled()) return null;
        if (this.disabled) return null;
        CEWorld ceWorld = world.storageWorld();
        Object[] sections = ChunkAccessProxy.INSTANCE.getSections(chunkAccess);
        CEChunk ceChunk;
        try {
            BukkitChunkAccess access = new BukkitChunkAccess(chunkAccess);
            ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld, chunkPos, access);
            ceChunk.setChunkAccess(access);
            if (!WorldStorageInjector.isSectionInjected(sections[0])) {
                CESection[] ceSections = ceChunk.sections();
                synchronized (sections) {
                    for (int i = 0; i < ceSections.length; i++) {
                        CESection ceSection = ceSections[i];
                        Object section = sections[i];
                        int finalI = i;
                        WorldStorageInjector.inject(section, ceSection, ceChunk, new SectionPos(chunkPos.x, ceChunk.sectionY(i), chunkPos.z),
                                (injected) -> sections[finalI] = injected);
                    }
                }
            }
            return () -> {
                try {
                    if (ceChunk.isUnsaved()) {
                        ceWorld.worldDataStorage().writeChunkAt(chunkPos, ceChunk);
                        ceChunk.setUnsaved(false);
                    }
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write chunk [world=" + ceWorld.name() +
                            ", chunk=(" + chunkPos.x + ", " + chunkPos.z + "), stage=" + stage.name() + "]", e);
                }
            };
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to read chunk [world=" + ceWorld.name() +
                    ", chunk=(" + chunkPos.x + ", " + chunkPos.z + ")]", e);
            return null;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void handleChunkLoad(CEWorld ceWorld, Chunk chunk) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        Object worldServer = CraftChunkProxy.INSTANCE.getWorld(chunk);
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(worldServer);
        Object levelChunk = LevelUtils.getChunkAtIfLoaded(chunkSource, chunkX, chunkZ);

        if (levelChunk == null) {
            this.plugin.logger().warn("Cannot get LevelChunk at ( world: " + ceWorld.name() + " | chunk: " + chunkX + "," + chunkZ + " )");
            return;
        }

        BukkitChunkAccess bukkitChunkAccess = new BukkitChunkAccess(levelChunk);
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        CEChunk ceChunk;
        try {
            ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld, chunkPos, bukkitChunkAccess);
            ceChunk.setChunkAccess(bukkitChunkAccess);
            CESection[] ceSections = ceChunk.sections();
            Object lightEngine = ChunkSourceProxy.INSTANCE.getLightEngine(chunkSource);
            Object[] sections = ChunkAccessProxy.INSTANCE.getSections(levelChunk);
            // 注入 ChunkAccess 的 BlockEntities 字段.
            if (Config.recipeInjectBlockEntities()) {
                injectBlockEntities(levelChunk);
            }
            synchronized (sections) {
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    if (Config.syncCustomBlocks()) {
                        Object statesContainer = LevelChunkSectionProxy.INSTANCE.getStates(section);
                        Object data = PalettedContainerProxy.INSTANCE.getData(statesContainer);
                        Object palette = PalettedContainerProxy.DataProxy.INSTANCE.getPalette(data);
                        boolean requiresSync = false;
                        if (SingleValuePaletteProxy.CLASS.isInstance(palette)) {
                            Object onlyBlockState = SingleValuePaletteProxy.INSTANCE.getValue(palette);
                            if (BlockStateUtils.isCustomBlock(onlyBlockState)) {
                                requiresSync = true;
                            }
                        } else if (LinearPaletteProxy.CLASS.isInstance(palette)) {
                            Object[] blockStates = LinearPaletteProxy.INSTANCE.getValues(palette);
                            for (Object blockState : blockStates) {
                                if (blockState != null) {
                                    if (BlockStateUtils.isCustomBlock(blockState)) {
                                        requiresSync = true;
                                        break;
                                    }
                                }
                            }
                        } else if (HashMapPaletteProxy.CLASS.isInstance(palette)) {
                            if (VersionHelper.hasLithiumPatch && LithiumHashPaletteProxy.CLASS.isInstance(palette)) {
                                Reference2IntOpenHashMap<Object> table = LithiumHashPaletteProxy.INSTANCE.getTable(palette);
                                for (Object blockState : table.keySet()) {
                                    if (blockState != null) {
                                        if (BlockStateUtils.isCustomBlock(blockState)) {
                                            requiresSync = true;
                                            break;
                                        }
                                    }
                                }
                            } else if (VersionHelper.hasUniverseSpigotPatch && CompactHashPaletteProxy.CLASS.isInstance(palette)) {
                                Object[] blockStates = CompactHashPaletteProxy.INSTANCE.getEntries(palette);
                                for (Object blockState : blockStates) {
                                    if (blockState != null) {
                                        if (BlockStateUtils.isCustomBlock(blockState)) {
                                            requiresSync = true;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Object biMap = HashMapPaletteProxy.INSTANCE.getValues(palette);
                                Object[] blockStates = CrudeIncrementalIntIdentityHashBiMapProxy.INSTANCE.getKeys(biMap);
                                for (Object blockState : blockStates) {
                                    if (blockState != null) {
                                        if (BlockStateUtils.isCustomBlock(blockState)) {
                                            requiresSync = true;
                                            break;
                                        }
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
                                        Object mcState = LevelChunkSectionProxy.INSTANCE.getBlockState(section, x, y, z);
                                        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(mcState);
                                        if (optionalCustomState.isPresent()) {
                                            ceSection.setBlockState(x, y, z, optionalCustomState.get());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 可能是新区块，已经被注入过了，那么在此取消注入
                    // 防止二次写入，造成不必要的保存
                    WorldStorageInjector.uninject(section);
                    if (ceWorld.settings.restoreCustomBlocksOnChunkLoad) {
                        boolean isEmptyBefore = LevelChunkSectionProxy.INSTANCE.hasOnlyAir(section);
                        int sectionY = ceSection.sectionY;
                        // 有自定义方块
                        if (!ceSection.isEmpty()) {
                            if (isEmptyBefore) {
                                LightEventListenerProxy.INSTANCE.updateSectionStatus(lightEngine, SectionPosProxy.INSTANCE.newInstance(chunkX, sectionY, chunkZ), false);
                            }
                            PalettedContainer<ImmutableBlockState> statesContainer = ceSection.statesContainer;
                            for (int index = 0; index < 4096; index++) {
                                ImmutableBlockState customState = statesContainer.get(index);
                                if (!customState.isEmpty() && customState.customBlockState() != null) {
                                    int x = index & 0xF;
                                    int z = (index >> 4) & 0xF;
                                    int y = (index >> 8) & 0xF;
                                    Object newState = customState.customBlockState().minecraftState();
                                    Object previous = LevelChunkSectionProxy.INSTANCE.setBlockState(section, x, y, z, newState, false);
                                    if (newState != previous && LightUtils.hasDifferentLightProperties(newState, previous)) {
                                        ThreadedLevelLightEngineProxy.INSTANCE.checkBlock(lightEngine, BlockPosProxy.INSTANCE.newInstance(chunkX * 16 + x, sectionY * 16 + y, chunkZ * 16 + z));
                                    }
                                    if (customState.hasConstantBlockEntityRenderer()) {
                                        BlockPos blockPos = new BlockPos(chunkX * 16 + x, sectionY * 16 + y, chunkZ * 16 + z);
                                        if (!ceChunk.hasConstantBlockEntityRenderer(blockPos)) {
                                            ceChunk.addConstantBlockEntityRenderer(blockPos, customState);
                                            ceChunk.setUnsaved(true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    int finalI = i;
                    WorldStorageInjector.inject(section, ceSection, ceChunk, new SectionPos(chunkPos.x, ceChunk.sectionY(i), chunkPos.z),
                            (injected) -> sections[finalI] = injected);
                }
            }

            ceChunk.load();
            ceChunk.activateAllBlockEntities();
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to read chunk tag at " + chunk.getX() + " " + chunk.getZ(), e);
        }
    }

    private static void injectBlockEntities(Object levelChunk) {
        if (VersionHelper.isOrAbove1_21_11 && VersionHelper.hasCanvasPatch) {
            Object[] blockEntities = ChunkAccessProxy.INSTANCE.canvas$getAllBlockEntities(levelChunk);
            for (Object blockEntity : blockEntities) {
                BukkitRecipeManager.injectFurnaceBlockEntity(blockEntity);
            }
        } else {
            Map<Object, Object> blockEntities = ChunkAccessProxy.INSTANCE.getBlockEntities(levelChunk);
            if (!(blockEntities instanceof InjectedBlockEntityMap<?,?>)) {
                // <BlockPos, BlockEntity>
                InjectedBlockEntityMap<Object, Object> mapListener = new InjectedBlockEntityMap<>(blockEntities, BukkitRecipeManager::injectFurnaceBlockEntity);
                ChunkAccessProxy.INSTANCE.setBlockEntities(levelChunk, mapListener);
                // 修改当前区块存在的
                for (Object blockEntity : blockEntities.values()) {
                    BukkitRecipeManager.injectFurnaceBlockEntity(blockEntity);
                }
            }
        }
    }

    private final class ConfiguredFeatureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("configured_feature(s)");

        @Override
        public Key type() {
            return Key.ce("configured_feature");
        }

        @Override
        public void postProcess() {
            List<Suggestion> suggestions = new ArrayList<>(BukkitWorldManager.this.cachedConfiguredFeaturesSuggestion);
            for (Key id : BukkitWorldManager.this.configuredFeatures.keySet()) {
                suggestions.add(Suggestion.suggestion(id.asString()));
            }
            BukkitWorldManager.this.cachedConfiguredFeaturesSuggestion = Collections.unmodifiableList(suggestions);
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection rawSection) {
            ConfigSection section = rawSection.withSamePath(processFeatureSection(rawSection));
            Object feature;
            JsonElement json = GsonHelper.get().toJsonTree(section.values());
            if (VersionHelper.isOrAbove1_20_5) {
                feature = ConfiguredFeatureProxy.CODEC.parse(RegistryOps.JSON, json)
                        .resultOrPartial(error -> {
                            throw new KnownResourceException("resource.configured_feature.invalid_feature", section.path(), json.toString(), error);
                        })
                        .orElse(null);
            } else {
                feature = LegacyDFUUtils.parse(ConfiguredFeatureProxy.CODEC, RegistryOps.JSON, json, (error) -> {
                    throw new KnownResourceException("resource.configured_feature.invalid_feature", section.path(), json.toString(), error);
                });
            }
            if (feature != null) {
                BukkitWorldManager.this.configuredFeatures.put(id, feature);
            }
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.CONFIGURED_FEATURE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.BLOCK);
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return BukkitWorldManager.this.configuredFeatures.size();
        }
    }

    private final class PlacedFeatureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("placed_feature(s)");
        private final AtomicInteger id = new AtomicInteger();
        private List<ConditionalFeature> tempFeatures = null;

        @Override
        public Key type() {
            return Key.ce("placed_feature");
        }

        @Override
        public void preProcess() {
            this.tempFeatures = Collections.synchronizedList(new ArrayList<>());
            this.id.set(0);
        }

        @Override
        public void postProcess() {
            BukkitWorldManager.this.customPlacedFeatures = this.tempFeatures;
            BukkitWorldManager.this.lastReloadFeatureTime = System.currentTimeMillis();
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.PLACED_FEATURE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.CONFIGURED_FEATURE);
        }

        private static final String[] BIOME = ConfigKeys.of("biome(s)");
        private static final String[] WORLD = ConfigKeys.of("world(s)");
        private static final String[] DIMENSION = ConfigKeys.of("dimension(s)");
        private static final String[] ENVIRONMENT = ConfigKeys.of("environment(s)|dimension_type(s)");

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection rawSection) {
            ConfigSection section = rawSection.withSamePath(processFeatureSection(rawSection));

            // 自定义筛选条件
            Predicate<Key> biomeFilter = parseFilter(section.getStringList(BIOME).stream(), Key::of);
            Predicate<String> worldFilter = parseFilter(section.getStringList(WORLD).stream(), Function.identity());
            Predicate<Key> dimensionFilter = parseFilter(section.getStringList(DIMENSION).stream(), Key::of);
            Predicate<Key> dimensionTypeFilter = parseFilter(section.getStringList(ENVIRONMENT).stream(), Key::of);

            // 解析feature
            Object rawFeature = section.get("feature");
            Object configuredFeature = null;
            if (rawFeature instanceof String name) {
                configuredFeature = BukkitWorldManager.this.configuredFeatures.get(Key.of(name));
            }
            if (configuredFeature == null) {
                JsonElement json = GsonHelper.get().toJsonTree(rawFeature);
                if (VersionHelper.isOrAbove1_20_5) {
                    configuredFeature = ConfiguredFeatureProxy.CODEC.parse(RegistryOps.JSON, json)
                            .resultOrPartial(error -> {
                                throw new KnownResourceException("resource.configured_feature.invalid_feature", section.assemblePath("feature"), json.toString(), error);
                            })
                            .orElse(null);
                } else {
                    configuredFeature = LegacyDFUUtils.parse(ConfiguredFeatureProxy.CODEC, RegistryOps.JSON, json, (error) -> {
                        throw new KnownResourceException("resource.configured_feature.invalid_feature", section.assemblePath("feature"), json.toString(), error);
                    });
                }
            }
            if (configuredFeature == null) {
                throw new KnownResourceException("resource.missing_argument", section.path(), "feature", TranslationManager.instance().plainTranslation(ConfigConstants.ARGUMENT_SECTION));
            }

            // 解析 placements
            List<Object> placements = section.getSectionList("placement", (s -> {
                String type = s.getString("type");
                if ("biome".equals(type) || "minecraft:biome".equals(type)) {
                    return BiomeFilterGenerator.createBiomePlacementFilter(biomeFilter);
                }
                JsonElement json = GsonHelper.get().toJsonTree(s.values());
                if (VersionHelper.isOrAbove1_20_5) {
                    return PlacementModifierProxy.CODEC.parse(RegistryOps.JSON, json)
                            .resultOrPartial(error -> {
                                throw new KnownResourceException("resource.placed_feature.invalid_placement", s.path(), json.toString(), error);
                            })
                            .orElse(null);
                } else {
                    return LegacyDFUUtils.parse(PlacementModifierProxy.CODEC, RegistryOps.JSON, json, (error) -> {
                        throw new KnownResourceException("resource.placed_feature.invalid_placement", s.path(), json.toString(), error);
                    });
                }
            }));
            if (placements.isEmpty()) {
                throw new KnownResourceException("resource.missing_argument", section.path(), "placement", TranslationManager.instance().plainTranslation(ConfigConstants.ARGUMENT_SECTION));
            }

            // 构造 placed feature 实例
            Object placedFeature = PlacedFeatureProxy.INSTANCE.newInstance(configuredFeature, placements);
            BukkitWorldManager.this.placedFeatures.put(id, HolderProxy.INSTANCE.direct(placedFeature));
            this.tempFeatures.add(new ConditionalFeature(this.id.getAndIncrement(), placedFeature, biomeFilter, worldFilter, dimensionFilter, dimensionTypeFilter));
        }

        @Override
        public int count() {
            return this.tempFeatures.size();
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        private <T> Predicate<T> parseFilter(Stream<String> stream, Function<String, T> mapper) {
            List<T> items = stream.map(mapper).toList();
            if (items.isEmpty()) {
                return k -> true;
            } else if (items.size() == 1) {
                T first = items.getFirst();
                return k -> k.equals(first);
            } else if (items.size() == 2) {
                T first = items.getFirst();
                T last = items.getLast();
                return k -> k.equals(first) || k.equals(last);
            } else if (items.size() <= 4) {
                return k -> {
                    for (T item : items) {
                        if (item.equals(k)) {
                            return true;
                        }
                    }
                    return false;
                };
            } else {
                Set<T> itemSet = new HashSet<>(items);
                return itemSet::contains;
            }
        }
    }

    private static final String BLOCK_ID = VersionHelper.isOrAbove26_3 ? "id" : "Name";
    private static final String BLOCK_PROPERTIES = VersionHelper.isOrAbove26_3 ? "properties" : "Properties";

    //简单地处理一下，将feature转换
    @SuppressWarnings({"DuplicatedCode"})
    private Map<String, Object> processFeatureSection(ConfigSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 使用 snake 命名法
        for (String key : section.keySet()) {
            ConfigValue value = section.getValue(key);
            if (value == null) continue;
            result.put(key.replace('-', '_'), processFeatureValue(value));
        }
        // 处理方块状态
        Object rawName = result.get(BLOCK_ID);
        if (rawName instanceof String blockName) {
            Optional<BlockDefinition> customBlock = this.plugin.blockManager().blockById(Key.of(blockName));
            // 如果是自定义方块名
            if (customBlock.isPresent()) {
                BlockDefinition block = customBlock.get();
                ImmutableBlockState blockState = block.defaultState();
                // 移除 properties 否则无法解析
                Object properties = result.remove(BLOCK_PROPERTIES);
                if (properties instanceof Map<?,?> propertiesMap && !propertiesMap.isEmpty()) {
                    for (Map.Entry<?, ?> entry : propertiesMap.entrySet()) {
                        String propertyValue = entry.getValue().toString();
                        Property<?> property = block.getProperty(entry.getKey().toString());
                        if (property != null) {
                            Optional<?> optionalValue = property.optional(propertyValue);
                            if (optionalValue.isPresent()) {
                                blockState = ImmutableBlockState.with(blockState, property, optionalValue.get());
                            }
                        }
                    }
                }
                result.put(BLOCK_ID, BlockStateUtils.getBlockOwnerIdFromState(blockState.customBlockState().minecraftState()).asString());
            }
        }
        // 处理 block predicate 等功能
        Object rawBlocks = result.get("blocks");
        if (rawBlocks != null) {
            if (rawBlocks instanceof String blockName && blockName.charAt(0) != '#') {
                Optional<BlockDefinition> customBlock = this.plugin.blockManager().blockById(Key.of(blockName));
                if (customBlock.isPresent()) {
                    BlockDefinition block = customBlock.get();
                    ImmutableBlockState blockState = block.defaultState();
                    result.put("blocks", BlockStateUtils.getBlockOwnerIdFromState(blockState.customBlockState().minecraftState()).asString());
                }
            } else if (rawBlocks instanceof List<?> list) {
                // list 的情况下，不能使用 tag
                List<String> newBlockList = new ArrayList<>(list.size());
                for (Object rawBlockName : list) {
                    if (rawBlockName instanceof String blockName) {
                        Optional<BlockDefinition> customBlock = this.plugin.blockManager().blockById(Key.of(blockName));
                        if (customBlock.isPresent()) {
                            BlockDefinition block = customBlock.get();
                            ImmutableBlockState blockState = block.defaultState();
                            newBlockList.add(BlockStateUtils.getBlockOwnerIdFromState(blockState.customBlockState().minecraftState()).asString());
                        } else {
                            newBlockList.add(blockName);
                        }
                    } else {
                        // 理论不会不是 string
                        newBlockList.add(rawBlockName.toString());
                    }
                }
                result.put("blocks", newBlockList);
            }
        }
        return result;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private Object processFeatureValue(@NotNull ConfigValue value) {
        if (value.is(Map.class)) {
            return processFeatureSection(value.getAsSection());
        } else if (value.is(List.class)) {
            return value.getAsList(this::processFeatureValue);
        }
        return value.value();
    }
}
