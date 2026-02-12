package net.momirealms.craftengine.bukkit.world;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.plugin.reflection.paper.PaperReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LegacyDFUUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.gen.ConditionalFeature;
import net.momirealms.craftengine.bukkit.world.gen.CraftEngineFeatures;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
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
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BukkitWorldManager implements WorldManager, Listener {
    private static BukkitWorldManager instance;
    private final BukkitCraftEngine plugin;
    private final ConcurrentUUID2ReferenceChainedHashTable<CEWorld> worlds;
    private CEWorld[] worldArray;
    private StorageAdaptor storageAdaptor;
    private boolean initialized = false;
    private UUID lastWorldUUID = null;
    private CEWorld lastWorld = null;
    private final ConfiguredFeatureParser configuredFeatureParser;
    private final PlacedFeatureParser placedFeatureParser;
    private final Map<Key, Object> configuredFeatures;
    private List<ConditionalFeature> placedFeatures;
    public long lastReloadFeatureTime;

    public BukkitWorldManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.worlds = ConcurrentUUID2ReferenceChainedHashTable.createWithCapacity(10, 0.5f);
        this.storageAdaptor = new DefaultStorageAdaptor();
        this.configuredFeatureParser = new ConfiguredFeatureParser();
        this.placedFeatureParser = new PlacedFeatureParser();
        this.configuredFeatures = new HashMap<>();
        this.placedFeatures = List.of();
    }

    @Override
    public void unload() {
        this.configuredFeatures.clear();
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

    public boolean hasCustomFeatures() {
        return !this.placedFeatures.isEmpty();
    }

    public synchronized CraftEngineFeatures fetchFeatures(Object serverLevel) {
        World world = FastNMS.INSTANCE.method$Level$getCraftWorld(serverLevel);
        String name = world.getName();
        Key dimension = KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.field$ResourceKey$location(FastNMS.INSTANCE.method$Level$dimension(serverLevel)));
        List<ConditionalFeature> features = new ArrayList<>();
        for (ConditionalFeature feature : this.placedFeatures) {
            if (feature.isAllowedWorld(name) && feature.isAllowedEnvironment(dimension)) {
                features.add(feature);
            }
        }
        return new CraftEngineFeatures(this.placedFeatures, features);
    }

    public long lastReloadFeatureTime() {
        return this.lastReloadFeatureTime;
    }

    public Object configuredFeatureById(Key id) {
        return this.configuredFeatures.get(id);
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.configuredFeatureParser, this.placedFeatureParser};
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
                injectWorld(ceWorld);
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (VersionHelper.isFolia()) {
                        this.plugin.scheduler().executeSync(() -> {
                            handleChunkLoad(ceWorld, chunk, false);
                            CEChunk loadedChunk = ceWorld.getChunkAtIfLoaded(chunk.getChunkKey());
                            if (loadedChunk != null) {
                                loadedChunk.setEntitiesLoaded(true);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        UUID uuid = world.getUID();
        if (this.worlds.containsKey(uuid)) return;
        CEWorld ceWorld = new BukkitCEWorld(wrap(world), this.storageAdaptor);
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

    @Override
    public CEWorld loadWorld(net.momirealms.craftengine.core.world.World world) {
        UUID uuid = world.uuid();
        if (this.worlds.containsKey(uuid)) {
            return this.worlds.get(uuid);
        }
        CEWorld ceWorld = new BukkitCEWorld(world, this.storageAdaptor);
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
        Object serverLevel = world.world.serverWorld();
        Object serverChunkCache = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
        Object chunkMap = FastNMS.INSTANCE.field$ServerChunkCache$chunkMap(serverChunkCache);
        FastNMS.INSTANCE.injectedWorldGen(world, chunkMap);
        if (!VersionHelper.isFolia()) {
            this.injectWorldCallback(serverLevel);
        }
    }

    // 用于从实体tick列表中移除家具实体以降低遍历开销
    private void injectWorldCallback(Object serverLevel) {
        try {
            Object entityLookup = FastNMS.INSTANCE.method$ServerLevel$getEntityLookup(serverLevel);
            Object worldCallback = PaperReflections.methodHandle$EntityLookup$worldCallbackGetter.invokeExact(entityLookup);
            Object injectedWorldCallback = FastNMS.INSTANCE.createInjectedEntityCallbacks(worldCallback, entityLookup);
            if (worldCallback == injectedWorldCallback) return;
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

    public class ConfiguredFeatureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"configured-feature", "configured-features"};

        @Override
        protected void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) throws LocalizedException {
            Map<String, Object> processedSection = replaceDashToUnderscore(section);
            Object feature;
            if (VersionHelper.isOrAbove1_20_5()) {
                feature = CoreReflections.instance$ConfiguredFeature$CODEC.parse(MRegistryOps.JSON, GsonHelper.get().toJsonTree(processedSection))
                        .resultOrPartial(error -> {
                            throw new LocalizedResourceConfigException("warning.config.configured_feature.invalid_feature", error);
                        })
                        .orElse(null);
            } else {
                feature = LegacyDFUUtils.parse(CoreReflections.instance$ConfiguredFeature$CODEC, MRegistryOps.JSON, GsonHelper.get().toJsonTree(processedSection), (error) -> {
                    throw new LocalizedResourceConfigException("warning.config.configured_feature.invalid_feature", error);
                });
            }
            if (feature != null) {
                BukkitWorldManager.this.configuredFeatures.put(id, feature);
            }
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.CONFIGURED_FEATURE;
        }

        @Override
        public int count() {
            return BukkitWorldManager.this.configuredFeatures.size();
        }
    }

    public class PlacedFeatureParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"placed-feature", "placed-features"};
        private List<ConditionalFeature> tempFeatures = null;
        private int id;

        @Override
        public void preProcess() {
            this.tempFeatures = new ArrayList<>();
            this.id = 0;
        }

        @Override
        public void postProcess() {
            BukkitWorldManager.this.placedFeatures = this.tempFeatures;
            BukkitWorldManager.this.lastReloadFeatureTime = System.currentTimeMillis();
        }

        @Override
        protected void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) throws LocalizedException {
            Map<String, Object> processedSection = replaceDashToUnderscore(section);
            Predicate<Key> biomeFilter = parseFilter(ResourceConfigUtils.get(processedSection, "biome", "biomes"), Key::of);
            Predicate<String> worldFilter = parseFilter(ResourceConfigUtils.get(processedSection, "world", "worlds"), Function.identity());
            Predicate<Key> environmentFilter = parseFilter(ResourceConfigUtils.get(processedSection, "dimension", "dimensions", "environment", "environments"), Key::of);

            Object rawFeature = processedSection.get("feature");
            Object configuredFeature = null;
            if (rawFeature instanceof String name) {
                configuredFeature = BukkitWorldManager.this.configuredFeatures.get(Key.of(name));
            }
            if (configuredFeature == null) {
                if (VersionHelper.isOrAbove1_20_5()) {
                    configuredFeature = CoreReflections.instance$ConfiguredFeature$CODEC.parse(MRegistryOps.JSON, GsonHelper.get().toJsonTree(rawFeature))
                            .resultOrPartial(error -> {
                                throw new LocalizedResourceConfigException("warning.config.placed_feature.invalid_feature", error);
                            })
                            .orElse(null);
                } else {
                    configuredFeature = LegacyDFUUtils.parse(CoreReflections.instance$ConfiguredFeature$CODEC, MRegistryOps.JSON, GsonHelper.get().toJsonTree(rawFeature), (error) -> {
                        throw new LocalizedResourceConfigException("warning.config.placed_feature.invalid_feature", error);
                    });
                }
            }
            if (configuredFeature == null) {
                throw new LocalizedResourceConfigException("warning.config.placed_feature.missing_feature");
            }
            Object rawPlacement = ResourceConfigUtils.get(processedSection, "placement");
            List<Object> placements = ResourceConfigUtils.parseConfigAsList(rawPlacement, map -> {
                if (map.get("type") instanceof String type) {
                    if (type.equals("biome") || type.equals("minecraft:biome")) {
                        return FastNMS.INSTANCE.createBiomePlacementFilter(biomeFilter);
                    }
                }
                JsonElement json = GsonHelper.get().toJsonTree(map);
                if (VersionHelper.isOrAbove1_20_5()) {
                    return CoreReflections.instance$PlacementModifier$CODEC.parse(MRegistryOps.JSON, json)
                            .resultOrPartial(error -> {
                                throw new LocalizedResourceConfigException("warning.config.placed_feature.invalid_placement", json.toString(), error);
                            })
                            .orElse(null);
                } else {
                    return LegacyDFUUtils.parse(CoreReflections.instance$PlacementModifier$CODEC, MRegistryOps.JSON, json, (error) -> {
                        throw new LocalizedResourceConfigException("warning.config.placed_feature.invalid_placement", json.toString(), error);
                    });
                }
            });
            if (placements.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.placed_feature.missing_placement");
            }
            try {
                Object placedFeature = CoreReflections.constructor$PlacedFeature.newInstance(configuredFeature, placements);
                this.tempFeatures.add(new ConditionalFeature(this.id++, placedFeature, biomeFilter, worldFilter, environmentFilter));
            } catch (ReflectiveOperationException e) {
                BukkitWorldManager.this.plugin.logger().warn("Failed to create placed feature '" + id + "': " + e, e);
            }
        }

        @Override
        public int count() {
            return this.tempFeatures.size();
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.PLACED_FEATURE;
        }

        private <T> Predicate<T> parseFilter(Object config, Function<String, T> mapper) {
            List<T> items = MiscUtils.getAsStringList(config).stream()
                    .map(mapper)
                    .toList();
            if (items.isEmpty()) {
                return k -> true;
            } else if (items.size() <= 3) {
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

    /**
     * 递归地将Map中所有键的短横线(-)替换为下划线(_)
     */
    protected Map<String, Object> replaceDashToUnderscore(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String originalKey = entry.getKey();
            Object value = entry.getValue();
            String newKey = originalKey.replace("-", "_");
            Object processedValue = processValue(value);
            result.put(newKey, processedValue);
        }
        Object rawName = result.get("Name");
        if (rawName instanceof String blockName) {
            Optional<CustomBlock> customBlock = this.plugin.blockManager().blockById(Key.of(blockName));
            if (customBlock.isPresent()) {
                CustomBlock block = customBlock.get();
                ImmutableBlockState blockState = block.defaultState();
                Object properties = result.remove("Properties");
                if (properties instanceof Map<?,?> propertiesMap && !propertiesMap.isEmpty()) {
                    for (Map.Entry<?, ?> entry : propertiesMap.entrySet()) {
                        String propertyValue = entry.getValue().toString();
                        Property<?> property = block.getProperty(entry.getKey().toString());
                        if (property != null) {
                            Optional<?> optionalValue = property.optional(propertyValue);
                            if (optionalValue.isEmpty()) {
                                return null;
                            } else {
                                blockState = ImmutableBlockState.with(blockState, property, optionalValue.get());
                            }
                        }
                    }
                }
                result.put("Name", BlockStateUtils.getBlockOwnerIdFromState(blockState.customBlockState().literalObject()).asString());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Object processValue(Object value) {
        if (value == null) return null;
        if (value instanceof Map) {
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            return replaceDashToUnderscore(nestedMap);
        }
        if (value instanceof List) {
            List<Object> originalList = (List<Object>) value;
            List<Object> processedList = new ArrayList<>();
            for (Object item : originalList) {
                processedList.add(processValue(item));
            }
            return processedList;
        }
        return value;
    }
}
