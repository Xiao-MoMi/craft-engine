package net.momirealms.craftengine.bukkit.world;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitCEChunk;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitChunkAccess;
import net.momirealms.craftengine.bukkit.world.gen.ConditionalFeature;
import net.momirealms.craftengine.bukkit.world.gen.CraftEngineFeatures;
import net.momirealms.craftengine.bukkit.world.gen.InjectedChunkGenerator;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.EmptyBlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.storage.StorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftChunkProxy;
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
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.status.WorldGenContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.ConfiguredFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacedFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementModifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LightEventListenerProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.EntityLookupProxy;
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
    private final ConcurrentUUID2ReferenceChainedHashTable<CEWorld> worlds;
    private CEWorld[] worldArray;
    private StorageAdaptor storageAdaptor;
    // for faster getter
    private UUID lastWorldUUID = null;
    private CEWorld lastWorld = null;
    // parsers
    private final ConfigParser configuredFeatureParser = new ConfiguredFeatureParser();
    private final ConfigParser placedFeatureParser = new PlacedFeatureParser();
    // features
    private final Map<Key, Object> configuredFeatures  = new ConcurrentHashMap<>();
    private final Map<Key, Object> placedFeatures = new ConcurrentHashMap<>();
    private List<ConditionalFeature> customPlacedFeatures = List.of();
    private List<Suggestion> cachedConfiguredFeaturesSuggestion = List.of();
    public long lastReloadFeatureTime;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        this.plugin = plugin;
        this.worlds = ConcurrentUUID2ReferenceChainedHashTable.createWithCapacity(10, 0.5f);
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
            BukkitWorld wrappedWorld = wrap(world);
            try {
                CEWorld ceWorld = this.worlds.computeIfAbsent(world.getUID(), k -> VersionHelper.isFolia ? new FoliaCEWorld(wrappedWorld, this.storageAdaptor) : new BukkitCEWorld(wrappedWorld, this.storageAdaptor));
                injectWorld(ceWorld);
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (VersionHelper.isFolia) {
                        this.plugin.scheduler().platform().run(() -> {
                            if (chunk.isLoaded()) {
                                handleChunkLoad(ceWorld, chunk, false);
                                CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getChunkKey());
                                if (loadedChunk != null) {
                                    loadedChunk.setEntitiesLoaded(true);
                                }
                            }
                        }, world, chunk.getX(), chunk.getZ());
                    } else {
                        handleChunkLoad(ceWorld, chunk, false);
                        CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getChunkKey());
                        if (loadedChunk != null) {
                            loadedChunk.setEntitiesLoaded(true);
                        }
                    }
                }
                ceWorld.setTicking(true);
            } catch (Throwable t) {
                this.plugin.logger().warn("Error loading world: " + world.getName(), t);
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
            //避免触发load world
            if (this.worlds.containsKey(world.getUID())) {
                CEWorld ceWorld = getWorld(world.getUID());
                ceWorld.setTicking(false);
                for (Chunk chunk : world.getLoadedChunks()) {
                    try {
                        handleChunkUnload(ceWorld, chunk);
                    } catch (Throwable t) {
                        this.plugin.logger().warn("Failed to unload chunk " + chunk.getX() + "," + chunk.getZ(), t);
                    }
                }
                try {
                    ceWorld.worldDataStorage().close();
                } catch (IOException e) {
                    this.plugin.logger().warn("Error unloading world: " + world.getName(), e);
                }
            }
        }
        this.worlds.clear();
        this.lastWorld = null;
        this.lastWorldUUID = null;
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
                ? KeyUtils.identifierToKey(ResourceKeyProxy.INSTANCE.getIdentifier(HolderProxy.ReferenceProxy.INSTANCE.getKey(holder)))
                : null;
        List<ConditionalFeature> features = new ArrayList<>();
        for (ConditionalFeature feature : this.customPlacedFeatures) {
            if (feature.isAllowedWorld(name) && feature.isAllowedEnvironment(dimension) && feature.isAllowedDimensionType(dimensionType)) {
                features.add(feature);
            }
        }
        return new CraftEngineFeatures(this.customPlacedFeatures, features);
    }

    public long lastReloadFeatureTime() {
        return this.lastReloadFeatureTime;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        UUID uuid = world.getUID();
        if (this.worlds.containsKey(uuid)) return;
        CEWorld ceWorld = VersionHelper.isFolia ? new FoliaCEWorld(wrap(world), this.storageAdaptor) : new BukkitCEWorld(wrap(world), this.storageAdaptor);
        this.worlds.put(uuid, ceWorld);
        this.resetWorldArray();
        this.injectWorld(ceWorld);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        CEWorld world = this.worlds.get(event.getWorld().getUID());
        if (world == null) {
            return;
        }
        handleChunkLoad(world, event.getChunk(), event.isNewChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        CEWorld world = this.worlds.get(event.getWorld().getUID());
        if (world == null) {
            return;
        }
        handleChunkUnload(world, event.getChunk());
    }

    @Override
    public void setStorageAdaptor(@NotNull StorageAdaptor storageAdaptor) {
        this.storageAdaptor = storageAdaptor;
    }

    public StorageAdaptor getStorageAdaptor() {
        return this.storageAdaptor;
    }

    public CEWorld getWorld(World world) {
        return getWorld(world.getUID());
    }

    @Override
    public CEWorld getWorld(UUID uuid) {
        if (!VersionHelper.isFolia && (uuid == this.lastWorldUUID || uuid.equals(this.lastWorldUUID))) {
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

    @Override
    public CEWorld loadWorld(net.momirealms.craftengine.core.world.World world) {
        UUID uuid = world.uuid();
        if (this.worlds.containsKey(uuid)) {
            return this.worlds.get(uuid);
        }
        CEWorld ceWorld = VersionHelper.isFolia ? new FoliaCEWorld(world, this.storageAdaptor) : new BukkitCEWorld(world, this.storageAdaptor);
        this.worlds.put(uuid, ceWorld);
        this.resetWorldArray();
        this.injectWorld(ceWorld);
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
        this.injectWorld(world);
        for (Chunk chunk : ((World) world.world().platformWorld()).getLoadedChunks()) {
            handleChunkLoad(world, chunk, false);
        }
        world.setTicking(true);
    }

    private void injectWorld(CEWorld world) {
        Object serverLevel = world.world.minecraftWorld();
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
        if (!VersionHelper.isFolia) {
            this.injectWorldCallback(serverLevel);
        }
    }

    // 用于从实体tick列表中移除家具实体以降低遍历开销
    private void injectWorldCallback(Object serverLevel) {
        Object entityLookup;
        if (VersionHelper.isOrAbove1_21) {
            entityLookup = LevelProxy.INSTANCE.moonrise$getEntityLookup(serverLevel);
        } else {
            entityLookup = ServerLevelProxy.INSTANCE.getEntityLookup(serverLevel);
        }
        Object worldCallback = EntityLookupProxy.INSTANCE.getWorldCallback(entityLookup);
        if (!(worldCallback instanceof InjectedWorldCallback)) {
            Object injectedWorldCallback = FastNMS.INSTANCE.createInjectedWorldCallbacks(worldCallback, entityLookup);
            EntityLookupProxy.INSTANCE.setWorldCallback(entityLookup, injectedWorldCallback);
        }
    }

    @Override
    public CEWorld createWorld(net.momirealms.craftengine.core.world.World world, WorldDataStorage storage) {
        return VersionHelper.isFolia ? new FoliaCEWorld(world, storage) : new BukkitCEWorld(world, storage);
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
            try {
                handleChunkUnload(ceWorld, chunk);
            } catch (Throwable t) {
                this.plugin.logger().warn("Failed to unload chunk " + chunk.getX() + "," + chunk.getZ(), t);
            }
        }
        if (uuid.equals(this.lastWorldUUID)) {
            this.lastWorld = null;
            this.lastWorldUUID = null;
        }
        try {
            ceWorld.worldDataStorage().close();
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to close world storage", e);
        }
    }

    @Override
    public <T> BukkitWorld wrap(T world) {
        if (world instanceof World w) {
            return new BukkitWorld(w);
        } else {
            throw new IllegalArgumentException(world.getClass() + " is not a Bukkit World");
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
                if (VersionHelper.isOrAbove1_21) {
                    levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedImmediately(chunkSource, chunk.getX(), chunk.getZ());
                } else {
                    levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedMainThread(chunkSource, chunk.getX(), chunk.getZ());
                }
            }

            CESection[] ceSections = ceChunk.sections();
            Object[] sections = ChunkAccessProxy.INSTANCE.getSections(levelChunk);
            synchronized (sections) {
                for (int i = 0; i < ceSections.length; i++) {
                    CESection ceSection = ceSections[i];
                    Object section = sections[i];
                    WorldStorageInjector.uninject(section);
                    if (Config.restoreVanillaBlocks()) {
                        if (!ceSection.statesContainer().isEmpty()) {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    for (int y = 0; y < 16; y++) {
                                        ImmutableBlockState customState = ceSection.getBlockState(x, y, z);
                                        if (!customState.isEmpty()) {
                                            BlockStateWrapper wrapper = customState.restoreBlockState();
                                            if (wrapper != null) {
                                                LevelChunkSectionProxy.INSTANCE.setBlockState(section, x, y, z, wrapper.minecraftState(), false);
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
    public void handleChunkGenerate(CEWorld ceWorld, ChunkPos chunkPos, Object chunkAccess) {
        if (ceWorld.isChunkLoaded(chunkPos.longKey)) return;
        Object[] sections = ChunkAccessProxy.INSTANCE.getSections(chunkAccess);
        CEChunk ceChunk;
        try {
            ceChunk = ceWorld.worldDataStorage().readNewChunkAt(ceWorld, chunkPos);
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
            ceChunk.load();
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to read new chunk at " + chunkPos.x + " " + chunkPos.z, e);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void handleChunkLoad(CEWorld ceWorld, Chunk chunk, boolean isNew) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        Object worldServer = CraftChunkProxy.INSTANCE.getWorld(chunk);
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(worldServer);
        Object levelChunk;
        if (VersionHelper.isOrAbove1_21) {
            levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedImmediately(chunkSource, chunkX, chunkZ);
        } else {
            levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedMainThread(chunkSource, chunkX, chunkZ);
        }
        BukkitChunkAccess bukkitChunkAccess = new BukkitChunkAccess(levelChunk);
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        CEChunk chunkAtIfLoaded = ceWorld.getChunkAtIfLoaded(chunkPos.longKey);
        if (chunkAtIfLoaded != null) {
            chunkAtIfLoaded.setChunkAccess(bukkitChunkAccess);
            if (isNew) {
                chunkAtIfLoaded.activateAllBlockEntities();
            }
            if (Config.recipeInjectBlockEntities()) {
                injectBlockEntities(levelChunk);
            }
            return;
        }

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
                    if (Config.restoreCustomBlocks()) {
                        boolean isEmptyBefore = LevelChunkSectionProxy.INSTANCE.hasOnlyAir(section);
                        int sectionY = ceSection.sectionY;
                        // 有自定义方块
                        PalettedContainer<ImmutableBlockState> palettedContainer = ceSection.statesContainer;
                        if (!palettedContainer.isEmpty()) {
                            if (isEmptyBefore) {
                                LightEventListenerProxy.INSTANCE.updateSectionStatus(lightEngine, SectionPosProxy.INSTANCE.newInstance(chunkX, sectionY, chunkZ), false);
                            }
                            for (int index = 0; index < 4096; index++) {
                                ImmutableBlockState customState = palettedContainer.get(index);
                                if (customState != EmptyBlockDefinition.STATE && customState.customBlockState() != null) {
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
        if (VersionHelper.isOrAbove1_21_11 && VersionHelper.isCanvas) {
            Object[] blockEntities = ChunkAccessProxy.INSTANCE.canvas$getAllBlockEntities(levelChunk);
            for (Object blockEntity : blockEntities) {
                BukkitRecipeManager.injectFurnaceBlockEntity(blockEntity);
            }
        } else {
            Map<Object, Object> blockEntities = ChunkAccessProxy.INSTANCE.getBlockEntities(levelChunk);
            if (!(blockEntities instanceof MapListener<?,?>)) {
                // <BlockPos, BlockEntity>
                MapListener<Object, Object> mapListener = new MapListener<>(blockEntities, BukkitRecipeManager::injectFurnaceBlockEntity);
                ChunkAccessProxy.INSTANCE.setBlockEntities(levelChunk, mapListener);
                // 修改当前区块存在的
                for (Object blockEntity : blockEntities.values()) {
                    BukkitRecipeManager.injectFurnaceBlockEntity(blockEntity);
                }
            }
        }
    }

    private final class ConfiguredFeatureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"configured-feature", "configured-features", "configured_feature", "configured_features"};

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
        public static final String[] CONFIG_SECTION_NAME = new String[] {"placed-feature", "placed-features", "placed_feature", "placed_features"};
        private final AtomicInteger id = new AtomicInteger();
        private List<ConditionalFeature> tempFeatures = null;
        private List<ConditionalFeature> backendFeatures = null;

        @Override
        public void preProcess() {
            this.backendFeatures = new ArrayList<>();
            this.tempFeatures = Collections.synchronizedList(this.backendFeatures);
            this.id.set(0);
        }

        @Override
        public void postProcess() {
            BukkitWorldManager.this.customPlacedFeatures = this.backendFeatures;
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

        private static final String[] BIOME = new String[] {"biome", "biomes"};
        private static final String[] WORLD = new String[] {"world", "worlds"};
        private static final String[] DIMENSION = new String[] {"dimension", "dimensions"};
        private static final String[] ENVIRONMENT = new String[] {"environment", "environments", "dimension-type", "dimension-types", "dimension_type", "dimension_types"};

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection rawSection) {
            ConfigSection section = rawSection.withSamePath(processFeatureSection(rawSection));

            // 自定义筛选条件
            Predicate<Key> biomeFilter = parseFilter(section.getStringList(BIOME).stream(), Key::of);
            Predicate<String> worldFilter = parseFilter(section.getStringList(WORLD).stream(), Function.identity());
            Predicate<Key> environmentFilter = parseFilter(section.getStringList(DIMENSION).stream(), Key::of);
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
                    return FastNMS.INSTANCE.createBiomePlacementFilter(biomeFilter);
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
            this.tempFeatures.add(new ConditionalFeature(this.id.getAndIncrement(), placedFeature, biomeFilter, worldFilter, environmentFilter, dimensionTypeFilter));
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
        Object rawName = result.get("Name");
        if (rawName instanceof String blockName) {
            Optional<BlockDefinition> customBlock = this.plugin.blockManager().blockById(Key.of(blockName));
            // 如果是自定义方块名
            if (customBlock.isPresent()) {
                BlockDefinition block = customBlock.get();
                ImmutableBlockState blockState = block.defaultState();
                // 移除 properties 否则无法解析
                Object properties = result.remove("Properties");
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
                result.put("Name", BlockStateUtils.getBlockOwnerIdFromState(blockState.customBlockState().minecraftState()).asString());
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
