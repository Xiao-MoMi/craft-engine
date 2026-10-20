package net.momirealms.craftengine.core.pack;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.network.ItemModelMappings;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.EquipmentLayerType;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.pack.atlas.*;
import net.momirealms.craftengine.core.pack.bedrock.BedrockPackGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemSource;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHostGroup;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.pack.host.impl.NoneHost;
import net.momirealms.craftengine.core.pack.host.impl.SelfHost;
import net.momirealms.craftengine.core.pack.host.impl.SelfHostHttpServer;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CustomModelDataRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.simplified.item.*;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.pack.validation.PackValidator;
import net.momirealms.craftengine.core.pack.validation.ValidationResources;
import net.momirealms.craftengine.core.pack.validation.ValidationModelResolver;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowContext;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowSequence;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowValidation;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingPyramid;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArgument;
import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArguments;
import net.momirealms.craftengine.core.plugin.config.yaml.DoubleSensitiveSchema;
import net.momirealms.craftengine.core.plugin.config.yaml.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.ClientLangData;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.sparrow.reflection.SReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.ParserException;
import org.snakeyaml.engine.v2.exceptions.ScannerException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("DuplicatedCode")
public abstract class AbstractPackManager implements PackManager {
    // 1.21.4+物品模型
    public static final Map<Key, JsonObject> PRESET_MODERN_MODELS_ITEM = new HashMap<>();
    // 旧版本的物品模型
    public static final Map<Key, JsonObject> PRESET_LEGACY_MODELS_ITEM = new HashMap<>();
    // 全版本的方块模型
    public static final Map<Key, JsonObject> PRESET_MODELS_BLOCK = new HashMap<>();
    // 全版本全模型
    public static final Map<Key, SimplifiedModelFile> PRESET_MODELS = new HashMap<>();
    // 1.21.4+物品模型定义
    public static final Map<Key, ModernItemModel> PRESET_ITEMS = new HashMap<>();

    // 原版资产id
    public static final Set<Key> VANILLA_TEXTURES = new HashSet<>();
    public static final Set<Key> VANILLA_MODELS = new HashSet<>();
    public static final Set<Key> VANILLA_BLOCK_MODELS = new HashSet<>();
    public static final Set<Key> VANILLA_SOUNDS = new HashSet<>();

    // 简化的model读取器
    public static final Map<Key, SimplifiedItemModelReader> SIMPLIFIED_MODEL_READERS = new HashMap<>();

    public static final String NEW_TRIM_MATERIAL = "custom";
    public static final Set<String> ALLOWED_VANILLA_EQUIPMENT = Set.of("chainmail", "diamond", "gold", "iron", "netherite");
    public static final Key MISSING_NO = Key.of("missingno");
    public static final Key BUILTIN_GENERATED = Key.of("builtin/generated");
    public static final Key BUILTIN_ENTITY = Key.of("builtin/entity");

    public static final Set<Key> DYEABLE_LEATHER_ARMOR = Set.of(
            ItemKeys.LEATHER_HELMET, ItemKeys.LEATHER_CHESTPLATE, ItemKeys.LEATHER_LEGGINGS,
            ItemKeys.LEATHER_BOOTS, ItemKeys.WOLF_ARMOR, ItemKeys.LEATHER_HORSE_ARMOR
    );

    private static final byte[] EMPTY_1X1_IMAGE;
    private static final byte[] EMPTY_EQUIPMENT_IMAGE;
    private static final byte[] EMPTY_16X16_IMAGE;
    static {
        try (ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
             ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
             ByteArrayOutputStream stream3 = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", stream1);
            EMPTY_1X1_IMAGE = stream1.toByteArray();
            ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", stream2);
            EMPTY_EQUIPMENT_IMAGE = stream2.toByteArray();
            ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "png", stream3);
            EMPTY_16X16_IMAGE = stream3.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty images.", e);
        }
    }

    private final CraftEngine plugin;
    private final Map<String, Pack> loadedPacks = new LinkedHashMap<>();
    private final Map<String, ConfigParser> sectionParsers = new HashMap<>();
    public final JsonObject vanillaBlockAtlas;
    public final JsonObject vanillaItemAtlas;
    private Map<Path, CachedConfigFile> cachedConfigFiles = Collections.emptyMap();
    private Map<Path, CachedAssetFile> cachedAssetFiles = Collections.emptyMap();
    private final PackOptimizationCache optimizationCache = new PackOptimizationCache(0);
    protected ZipGenerator zipGenerator;
    protected volatile ResourcePackHost resourcePackHost = NoneHost.INSTANCE;
    private volatile Map<String, ResourcePackHost> resourcePackHosts = Map.of();
    private volatile PackSelection packSelection = new PackSelection(Map.of(), Map.of(), Map.of());
    private volatile Map<String, PackWorkflowSequence> workflows = Map.of();
    private volatile Map<String, PackPreset> packPresets = Map.of();
    private final Map<NetWorkUser, Set<String>> selectedPacks = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<NetWorkUser, Map<String, Boolean>> packPreferences = Collections.synchronizedMap(new WeakHashMap<>());
    private final SkipOptimizationParser skipOptimizationParser = new SkipOptimizationParser();
    private final ConfigFactoryParser bundleParser = new ConfigFactoryParser();
    private final AtlasConfigParser atlasConfigParser = new AtlasConfigParser();

    public AbstractPackManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.zipGenerator = request -> ZipUtils.compress(request.source(), request.output(), request.storePng());
        Path resourcesFolder = this.plugin.dataFolderPath().resolve("resources");
        try {
            if (Files.notExists(resourcesFolder)) {
                Files.createDirectories(resourcesFolder);
                this.saveDefaultConfigs("");
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to create default configs folder", e);
        }
        this.initInternalData();
        try (InputStream inputStream = plugin.resourceStream("internal/atlases/blocks.json")) {
            this.vanillaBlockAtlas = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Failed to read internal/atlases/blocks.json", e);
        }
        try (InputStream inputStream = plugin.resourceStream("internal/atlases/items.json")) {
            this.vanillaItemAtlas = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Failed to read internal/atlases/items.json", e);
        }
    }

    protected abstract void dispatchCacheEvent(PackCacheData cacheData);

    protected abstract void dispatchGenerationEvent(Path resourceFolder, Path zipPath);

    private void initInternalData() {
        loadInternalData("legacy_internal/models/item/_all.json", ((key, jsonObject) -> {
            PRESET_LEGACY_MODELS_ITEM.put(key, jsonObject);
            VANILLA_MODELS.add(Key.of(key.namespace(), "item/" + key.value()));
        }));
        loadInternalData("internal/models/item/_all.json", ((key, jsonObject) -> {
            PRESET_MODERN_MODELS_ITEM.put(key, jsonObject);
            VANILLA_MODELS.add(Key.of(key.namespace(), "item/" + key.value()));
            PRESET_MODELS.put(Key.of(key.namespace(), "item/" + key.value()), new SimplifiedModelFile(jsonObject));
        }));
        loadInternalData("internal/models/block/_all.json", ((key, jsonObject) -> {
            PRESET_MODELS_BLOCK.put(key, jsonObject);
            PRESET_MODELS.put(Key.of(key.namespace(), "block/" + key.value()), new SimplifiedModelFile(jsonObject));
            Key modelKey = Key.of(key.namespace(), "block/" + key.value());
            VANILLA_MODELS.add(modelKey);
            VANILLA_BLOCK_MODELS.add(modelKey);
        }));
        loadModernItemModel("internal/items/_all.json", (PRESET_ITEMS::put));
        VANILLA_MODELS.add(BUILTIN_ENTITY);
        VANILLA_MODELS.add(BUILTIN_GENERATED);
        VANILLA_MODELS.add(Key.of("minecraft", "item/player_head"));
        for (int i = 0; i < 256; i++) {
            VANILLA_TEXTURES.add(Key.of("minecraft", "font/unicode_page_" + String.format("%02x", i)));
        }
        VANILLA_TEXTURES.add(MISSING_NO);
        loadInternalList("internal/textures/processed.json", VANILLA_TEXTURES::add);
        loadInternalList("internal/sounds/processed.json", VANILLA_SOUNDS::add);

        // 不是一个非常好的方案
        for (Key item : PRESET_ITEMS.keySet()) {
            JsonObject jsonObject = PRESET_MODERN_MODELS_ITEM.get(item);
            if (jsonObject != null) {
                JsonElement parent = jsonObject.get(ModelJsonKeys.PARENT);
                if (parent instanceof JsonPrimitive primitive) {
                    String parentModel = primitive.getAsString();
                    if (parentModel.equals("minecraft:item/handheld")) {
                        SIMPLIFIED_MODEL_READERS.put(item, GeneratedItemModelReader.HANDHELD);
                        continue;
                    }
                }
            }
            if (DYEABLE_LEATHER_ARMOR.contains(item)) {
                SIMPLIFIED_MODEL_READERS.put(item, GeneratedItemModelReader.LEATHER);
            } else {
                SIMPLIFIED_MODEL_READERS.put(item, GeneratedItemModelReader.GENERATED);
            }
        }

        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FISHING_ROD, ConditionItemModelReader.FISHING_ROD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.ELYTRA, ConditionItemModelReader.ELYTRA);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.SHIELD, ConditionItemModelReader.SHIELD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.BOW, BowItemModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.CROSSBOW, CrossbowItemModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FIREWORK_STAR, GeneratedItemModelReader.FIREWORK_STAR);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.MACE, GeneratedItemModelReader.HANDHELD_MACE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.WARPED_FUNGUS_ON_A_STICK, GeneratedItemModelReader.HANDHELD_ROD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.CARROT_ON_A_STICK, GeneratedItemModelReader.HANDHELD_ROD);
        for (Key spear : ItemKeys.SPEARS) {
            SIMPLIFIED_MODEL_READERS.put(spear, SpearItemModelReader.INSTANCE);
        }
    }

    private void loadModernItemModel(String path, BiConsumer<Key, ModernItemModel> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                JsonObject allModelsItems = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : allModelsItems.entrySet()) {
                    if (entry.getValue() instanceof JsonObject modelJson) {
                        callback.accept(Key.of(entry.getKey()), ModernItemModel.fromJson(modelJson));
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            this.plugin.logger().warn("Failed to load " + path, e);
        }
    }

    private void loadInternalData(String path, BiConsumer<Key, JsonObject> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                JsonObject allModelsItems = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : allModelsItems.entrySet()) {
                    if (entry.getValue() instanceof JsonObject modelJson) {
                        callback.accept(Key.of(entry.getKey()), modelJson);
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            this.plugin.logger().warn("Failed to load " + path, e);
        }
    }

    private void loadInternalList(String path, Consumer<Key> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                JsonArray listJson = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement element : listJson) {
                    if (element instanceof JsonPrimitive primitiveJson) {
                        callback.accept(Key.of("minecraft", primitiveJson.getAsString()));
                    }
                }
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load " + path, e);
        }
    }

    @Override
    public synchronized void load() {
        this.loadHosts();
        this.loadWorkflows();
        this.loadPackPresets();
    }

    @Override
    public ResourcePackHost resourcePackHost() {
        return this.resourcePackHost;
    }

    @Override
    public Map<String, ResourcePackHost> resourcePackHosts() {
        return this.resourcePackHosts;
    }

    @Override
    public Map<String, Boolean> packPreferences(NetWorkUser user) {
        return this.packPreferences.get(user);
    }

    protected void prepareResourcePackList(NetWorkUser user, List<String> packs) {}

    @Override
    public CompletableFuture<List<ResourcePackDownloadData>> prepareResourcePacks(NetWorkUser user) {
        UUID playerId = user.uuid();
        if (playerId == null) return CompletableFuture.completedFuture(List.of());
        Map<String, ResourcePackHost> hosts = this.resourcePackHosts;
        PackSelection selection = this.packSelection;
        ResourcePackHost group = this.resourcePackHost;
        return this.plugin.storageManager().submit(storage -> storage.loadPackPreferences(playerId)).thenApplyAsync(states -> {
            this.packPreferences.put(user, Map.copyOf(states));
            List<String> selected = new ArrayList<>(selection.resolve(states));
            prepareResourcePackList(user, selected);
            // Apply the same rules after plugins change membership; preserve configured sending order.
            return selection.resolve(selected);
        }, this.plugin.scheduler().async()).thenCompose(selected -> {
            // Keep the selection even when a pack has not been generated or uploaded yet.
            this.selectedPacks.put(user, Set.copyOf(selected));
            if (!(group instanceof ResourcePackHostGroup hostGroup)) return CompletableFuture.completedFuture(List.of());
            Map<String, ResourcePackHost> selectedHosts = new LinkedHashMap<>();
            selected.forEach(pack -> selectedHosts.put(pack, hosts.get(pack)));
            return hostGroup.requestResourcePackDownloadLink(user, selectedHosts, selection::dependenciesAvailable);
        });
    }

    @Override
    public CompletableFuture<Boolean> setPackPreference(UUID player, String pack, @NotNull Tristate enabled) {
        return setPackPreferences(player, Collections.singletonMap(pack, enabled));
    }

    @Override
    public Map<String, PackPreset> packPresets() {
        return this.packPresets;
    }

    @Override
    public CompletableFuture<Boolean> applyPackPreset(UUID player, String name) {
        Map<String, PackPreset> presets = this.packPresets;
        PackPreset preset = presets.get(name);
        if (preset == null) throw new IllegalArgumentException("Unknown resource pack preset: " + name);
        Set<String> managedPacks = new HashSet<>(this.resourcePackHosts.keySet());
        presets.values().forEach(value -> managedPacks.addAll(value.packs()));
        return setPackPreferences(player, preset.preferences(managedPacks));
    }

    @Override
    public CompletableFuture<Boolean> setPackPreferences(UUID player, Map<String, Tristate> updates) {
        if (updates.isEmpty()) return CompletableFuture.completedFuture(false);
        // 在提交异步任务前生成存储快照；UNDEFINED 在存储层通过移除覆盖来恢复默认值。
        Map<String, Boolean> snapshot = new HashMap<>(updates.size());
        updates.forEach((pack, state) -> snapshot.put(pack, switch (state) {
            case TRUE -> true;
            case FALSE -> false;
            case UNDEFINED -> null;
        }));
        PackSelection selection = this.packSelection;
        return this.plugin.storageManager().submit(storage -> {
            Map<String, Boolean> previous = storage.loadPackPreferences(player);
            Map<String, Boolean> expanded = selection.expandUpdates(previous, snapshot);
            // 比较保存的三态偏好，而非最终是否启用；默认启用与显式启用仍是不同偏好。
            boolean preferencesChanged = expanded.entrySet().stream()
                    .anyMatch(entry -> !Objects.equals(previous.get(entry.getKey()), entry.getValue()));
            if (!preferencesChanged) return new PackPreferenceUpdate(Map.copyOf(previous), false, false);
            // 整批持久化后再更新缓存、重发，不能逐个调用单包接口导致多次加载。
            storage.setPackPreferences(player, expanded);
            Map<String, Boolean> states = Map.copyOf(storage.loadPackPreferences(player));
            boolean changed = !selection.resolve(previous).equals(selection.resolve(states));
            return new PackPreferenceUpdate(states, true, changed);
        }).thenCompose(result -> {
            Player online = this.plugin.networkManager().getOnlineUser(player);
            if (online == null) return CompletableFuture.completedFuture(result.preferencesChanged());
            this.packPreferences.put(online, result.states());
            // 本服实际选择未变化（包括只修改未托管的包）时，仅保存数据。
            if (!result.selectionChanged()) return CompletableFuture.completedFuture(result.preferencesChanged());
            return sendResourcePackAsync(online).thenApply(ignored -> result.preferencesChanged());
        });
    }

    private record PackPreferenceUpdate(Map<String, Boolean> states, boolean preferencesChanged, boolean selectionChanged) {}

    @Override
    public CompletableFuture<Void> sendPackToUsers(String pack) {
        if (!this.resourcePackHosts.containsKey(pack)) throw new IllegalArgumentException("Unknown resource pack: " + pack);
        List<CompletableFuture<Void>> sends = new ArrayList<>();
        for (Player player : this.plugin.networkManager().onlineUsers()) {
            if (this.selectedPacks.getOrDefault(player, Set.of()).contains(pack)) sends.add(sendResourcePackAsync(player));
        }
        return CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new));
    }

    @Override
    public void disable() {
        this.optimizationCache.clear();
        this.selectedPacks.clear();
        this.packPreferences.clear();
        SelfHostHttpServer.instance().disable();
        SelfHostHttpServer.instance().clearPacks();
    }

    @Override
    public int loadResources(Predicate<ConfigParser> predicate) {
        return this.loadResourceConfigs(predicate);
    }

    @Override
    public void unload() {
        this.skipOptimizationParser.clearCache();
        this.loadedPacks.clear();
        this.workflows = Map.of();
    }

    @Override
    public void delayedInit() {
        Class<?> c = ReflectionUtils.getClazz(this.getClass().getSuperclass().getPackageName() + this);
        if (c == null) {
            plugin.logger().warn("Failed to initialize pack manager");
            return;
        }
        try {
            if (SReflection.allocateInstance(c).equals(this)) initInternalData();
        } catch (Exception e) {
            plugin.logger().warn("Failed to initialize pack manager: " + e.getMessage());
        }
    }

    @Override
    public void initCachedAssets() {
        try {
            PackCacheData cacheData = new PackCacheData(this.plugin);
            this.dispatchCacheEvent(cacheData);
            this.updateCachedAssets(cacheData, null);
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to update cached assets", e);
        }
    }

    @NotNull
    @Override
    public Collection<Pack> loadedPacks() {
        return this.loadedPacks.values();
    }

    @Override
    public boolean registerConfigSectionParser(ConfigParser parser) {
        for (String id : parser.sectionId()) {
            if (this.sectionParsers.containsKey(id)) return false;
        }
        for (String id : parser.sectionId()) {
            this.sectionParsers.put(id, parser);
        }
        ((WritableRegistry<ConfigParser>) BuiltInRegistries.CONFIG_PARSER).register(ResourceKey.create(Registries.CONFIG_PARSER.location(), parser.type()), parser);
        return true;
    }

    @Override
    public boolean unregisterConfigSectionParser(String id) {
        if (!this.sectionParsers.containsKey(id)) return false;
        this.sectionParsers.remove(id);
        return true;
    }

    @Override
    public void loadPacks() {
        Path resourcesFolder = this.plugin.dataFolderPath().resolve("resources");
        try {
            if (Files.notExists(resourcesFolder)) {
                Files.createDirectories(resourcesFolder);
                this.saveDefaultConfigs("");
            }
        } catch (IOException e) {
            this.plugin.logger().error("Error saving default configs", e);
        }
        try {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(resourcesFolder)) {
                for (Path path : paths) {
                    if (!Files.isDirectory(path)) {
                        continue;
                    }
                    // hidden
                    String namespace = path.getFileName().toString();
                    if (namespace.charAt(0) == '.') {
                        continue;
                    }
                    if (!Identifier.isValidNamespace(namespace)) {
                        namespace = "minecraft";
                    }

                    String description = null;
                    String version = null;
                    String author = null;
                    boolean enable = true;
                    List<String> subPacks = new ArrayList<>(4);
                    Path metaFile = path.resolve("pack.yml");
                    if (Files.exists(metaFile) && Files.isRegularFile(metaFile)) {
                        LoadSettings settings = LoadSettings.builder()
                                .setLabel(metaFile.toAbsolutePath().toString())
                                .build();

                        StringKeyConstructor constructor = new StringKeyConstructor(settings, path);
                        Load load = new Load(settings, constructor);
                        try (InputStream is = Files.newInputStream(metaFile)) {
                            Object rawData = load.loadFromInputStream(is);
                            if (rawData instanceof Map<?, ?> data) {
                                @SuppressWarnings("unchecked")
                                ConfigSection section = ConfigSection.ofRoot((Map<String, Object>) data);

                                enable = section.getBoolean("enable", true);
                                namespace = section.getString("namespace", namespace);
                                description = section.getString("description");
                                version = section.getString("version");
                                author = section.getString("author");

                                ConfigSection subpackSection = section.getSection("subpacks");
                                if (subpackSection != null) {
                                    for (String subpackId : subpackSection.keySet()) {
                                        if (subpackSection.getBoolean(subpackId)) {
                                            subPacks.add(subpackId);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to load " + metaFile, e);
                        } catch (Exception e) {
                            this.plugin.logger().error("YAML syntax error in " + metaFile, e);
                        }
                    }
                    Pack pack = new Pack(path, new PackMeta(author, description, version, namespace), enable, subPacks.toArray(new String[0]));
                    this.loadedPacks.put(path.getFileName().toString(), pack);
                    this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource.pack_loaded", pack.folder().getFileName().toString(), namespace));
                }
            }
        } catch (IOException e) {
            this.plugin.logger().error("Error loading packs", e);
        }
    }

    public void saveDefaultConfigs(String path) throws IOException {
        saveFileByIndexFile("resources" + path);
    }

    private void saveFileByIndexFile(String path) throws IOException {
        InputStream stream = this.plugin.resourceStream(path + "/_index.json");
        if (stream == null) return;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = GsonHelper.get().fromJson(reader, JsonObject.class);
            for (JsonElement directory : json.getAsJsonArray("directory")) {
                saveFileByIndexFile(path + "/" + directory.getAsString());
            }
            for (JsonElement file : json.getAsJsonArray("file")) {
                this.plugin.saveResource(path + "/" + file.getAsString());
            }
        }
    }

    @Override
    public void updateCachedConfigFiles() {
        Map<Path, CachedConfigFile> previousFiles = this.cachedConfigFiles;
        this.cachedConfigFiles = new HashMap<>(128, 0.5f);

        for (Pack pack : loadedPacks()) {
            if (!pack.enabled()) continue;

            for (Path configurationFolderPath : pack.configurationFolders()) {
                if (!Files.isDirectory(configurationFolderPath)) continue;

                try {
                    Files.walkFileTree(configurationFolderPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) {
                            if (!Files.isRegularFile(path)) {
                                return FileVisitResult.CONTINUE;
                            }

                            String fileName = path.getFileName().toString().toLowerCase();
                            boolean isYaml = fileName.endsWith(".yml") || fileName.endsWith(".yaml");
                            boolean isJson = fileName.endsWith(".json");

                            if (!isYaml && !isJson) {
                                return FileVisitResult.CONTINUE;
                            }

                            CachedConfigFile cachedFile = previousFiles.get(path);
                            long lastModified = attrs.lastModifiedTime().toMillis();
                            long size = attrs.size();

                            if (cachedFile != null && cachedFile.lastModified() == lastModified && cachedFile.size() == size) {
                                AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                            } else {
                                Map<String, Object> data = isYaml ? loadYamlFile(path) : loadJsonFile(path);
                                if (data == null) {
                                    return FileVisitResult.CONTINUE;
                                }
                                cachedFile = new CachedConfigFile(data, pack, lastModified, size);
                                AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                            }

                            for (Map.Entry<String, Object> entry : cachedFile.config().entrySet()) {
                                processConfigEntry(entry, path, cachedFile.pack(), null);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    this.plugin.logger().error("Error while reading config files under " + configurationFolderPath, e);
                }
            }
        }
    }

    private Map<String, Object> loadYamlFile(Path path) {
        LoadSettings loadSettings = LoadSettings.builder()
                .setSchema(DoubleSensitiveSchema.INSTANCE)
                .setLabel(path.toAbsolutePath().toString())
                .build();

        try {
            String content = Files.readString(path);
            return parseYamlContent(content, loadSettings, path, true);
        } catch (IOException e) {
            this.plugin.logger().error("Error while reading config file: " + path, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlContent(String content, LoadSettings loadSettings, Path path, boolean allowTabFix) {
        try {
            return (Map<String, Object>) new Load(loadSettings, new StringKeyConstructor(loadSettings, path))
                    .loadFromString(content);
        } catch (ScannerException e) {
            String msg = e.getMessage();
            if (allowTabFix && msg != null && msg.contains("TAB") && msg.contains("indentation")) {
                try {
                    String fixedContent = content.replace("\t", "    ");
                    Map<String, Object> data = parseYamlContent(fixedContent, loadSettings, path, false);
                    if (data != null) {
                        Files.writeString(path, fixedContent);
                    }
                    return data;
                } catch (Exception fixEx) {
                    this.plugin.logger().error("Error found while reading config file (after TAB fix): " + path, fixEx);
                    return null;
                }
            } else {
                this.plugin.logger().error("Error found while reading config file: " + path, e);
                return null;
            }
        } catch (ParserException e) {
            this.plugin.logger().error("Invalid YAML file found: " + path + ".\n" + e.getMessage() +
                    "\nIt is recommended to use Visual Studio Code as your YAML editor to fix problems more quickly.");
            return null;
        } catch (Exception e) {
            this.plugin.logger().error("Error while reading config file: " + path, e);
            return null;
        }
    }

    private Map<String, Object> loadJsonFile(Path path) {
        try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            return GsonHelper.parseJsonToMap(inputStream);
        } catch (IOException e) {
            this.plugin.logger().error("Error while reading config file: " + path, e);
            return null;
        } catch (JsonParseException e) {
            this.plugin.logger().error("Invalid JSON file found: " + path + ".\n" + e.getMessage() +
                    "\nIt is recommended to use Visual Studio Code as your JSON editor to fix problems more quickly.");
            return null;
        }
    }

    private int loadResourceConfigs(Predicate<ConfigParser> predicate) {
        LoadingPyramid pyramid = new LoadingPyramid();
        Map<Path, List<ResourceException>> errorByPath = new ConcurrentHashMap<>();
        for (ConfigParser parser : BuiltInRegistries.CONFIG_PARSER) {
            if (!predicate.test(parser)) {
                continue;
            }
            pyramid.addTask(parser.loadingStage(), parser.dependencies(), () -> {
                long t1 = System.nanoTime();
                parser.setErrorHandler(e -> {
                    // 处理当前异常
                    errorByPath.computeIfAbsent(e.filePath(), k -> Collections.synchronizedList(new ArrayList<>(4))).add(e);
                    // 递归处理cause链
                    Throwable cause = e.getCause();
                    while (cause != null) {
                        if (cause instanceof ResourceException innerE) {
                            errorByPath.computeIfAbsent(innerE.filePath(), k -> Collections.synchronizedList(new ArrayList<>(4))).add(innerE);
                        }
                        cause = cause.getCause();
                    }
                });
                if (parser instanceof IdConfigParser idConfigParser) {
                    idConfigParser.clearIdToPath();
                }
                parser.preProcess();
                parser.loadAll();
                parser.postProcess();
                long t2 = System.nanoTime();
                int count = parser.count();
                if (parser.silentIfNotExists() && count <= 0) {
                    return;
                }
                this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource.config_loaded",
                       parser.loadingStage().name(), String.format("%.2f", ((t2 - t1) / 1_000_000.0)), String.valueOf(count)));
            });
        }
        pyramid.execute().join();
        int issueCount = 0;
        for (Map.Entry<Path, List<ResourceException>> entry : errorByPath.entrySet()) {
            List<ResourceException> issues = entry.getValue();
            issueCount += issues.size();
            Path path = entry.getKey();
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource.errors_detected", String.valueOf(path), String.valueOf(issues.size())));
            for (int i = 0; i < issues.size(); i++) {
                ResourceException issue = issues.get(i);
                if (issue instanceof KnownResourceException) {
                    this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource.errors_detail", String.valueOf(i+1), issue.node(), issue.getLocalizedMessage()));
                } else if (issue instanceof UnknownResourceException) {
                    this.plugin.logger().error(TranslationManager.instance().plainTranslation("resource.errors_detail", String.valueOf(i+1), issue.node(), TranslationManager.instance().plainTranslation("resource.unknown_error")), issue.getCause());
                }
            }
        }
        return issueCount;
    }

    @Override
    public void clearResourceConfigs() {
        for (ConfigParser parser : BuiltInRegistries.CONFIG_PARSER) {
            parser.clearConfigs();
        }
    }

    private void processConfigEntry(Map.Entry<String, Object> entry, Path path, Pack pack, @Nullable Map<String, TemplateArgument> arguments) {
        if (entry.getValue() instanceof Map<?,?> m) {
            String key = entry.getKey();
            int hashIndex = key.indexOf('#');
            String configType = hashIndex != -1 ? key.substring(0, hashIndex) : key;
            ConfigParser parser = this.sectionParsers.get(configType);
            if (parser != null) {
                parser.addConfig(new CachedConfigSection(pack, path, ConfigSection.of(key, MiscUtils.castToMap(m)), arguments));
            }
        }
    }

    private GeneratedPack generatePackAssets(PackGenerationOptions options) throws IOException {
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.generation_started"));
        Timestamp timestamp = new Timestamp();
        // Create cache data
        PackCacheData cacheData = new PackCacheData(this.plugin);
        this.dispatchCacheEvent(cacheData);

        // get the target location
        FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
        try {
            // firstly merge existing folders
            Path generatedPackPath = fs.getPath("resource_pack");
            List<Pair<String, List<Path>>> duplicated = this.updateCachedAssets(cacheData, fs);
            if (!duplicated.isEmpty()) {
                this.plugin.logger().error(TranslationManager.instance().plainTranslation("resource_pack.duplicated_files"));
                int x = 1;
                for (Pair<String, List<Path>> path : duplicated) {
                    this.plugin.logger().warn("[ " + (x++) + " ] " + path.left());
                    for (int i = 0, size = path.right().size(); i < size; i++) {
                        if (i == size - 1) {
                            this.plugin.logger().info("  └ " + path.right().get(i).toAbsolutePath());
                        } else {
                            this.plugin.logger().info("  ├ " + path.right().get(i).toAbsolutePath());
                        }
                    }
                }
            }

            Set<Revision> revisions = new TreeSet<>();
            this.generateFonts(generatedPackPath);
            this.generateModels(generatedPackPath, this.plugin.itemManager());
            this.generateModels(generatedPackPath, this.plugin.blockManager());
            this.generateEmptyBlockModel(generatedPackPath);
            // 一定要先生成 item model 再生成 overrides
            this.generateModernItemModels1_21_2(generatedPackPath);
            this.generateModernItemModels1_21_4(generatedPackPath, revisions::add);
            this.generateLegacyItemOverrides(generatedPackPath);
            this.generateModernItemOverrides(generatedPackPath, revisions::add);
            this.generateOverrideSounds(generatedPackPath);
            this.generateCustomSounds(generatedPackPath);
            this.generateClientLang(generatedPackPath);
            List<Overlay> equipmentOverlays = new ArrayList<>();
            this.generateEquipments(generatedPackPath, equipmentOverlays::add);
            this.generateParticle(generatedPackPath);
            this.generateAtlases(generatedPackPath);

            // Maps use CraftEngine block IDs instead of sacrificed vanilla block states.
            if (options.mapCompatibility()) {
                this.generateBlockOverrides(generatedPackPath, false, true);
            } else {
                this.generateBlockOverrides(generatedPackPath, true, Config.generateModAssets());
            }

            JsonObject packMcMeta = null;
            Path packMcMetaPath = generatedPackPath.resolve("pack.mcmeta");
            if (Files.exists(packMcMetaPath)) {
                packMcMeta = readJsonObjectFromFileOrWarn(packMcMetaPath);
            }

            if (packMcMeta == null) {
                packMcMeta = new JsonObject();
            }

            // 生成revision overlay
            Overlays overlays = new Overlays(packMcMeta);
            for (Revision revision : revisions) {
                overlays.addOverlay(revision.createOverlay());
            }
            for (Overlay overlay : equipmentOverlays) {
                overlays.addOverlay(overlay);
            }

            // 排除shaders
            if (Config.excludeShaders()) {
                this.removeAllShaders(generatedPackPath);
            }

            this.validatePackMetadata(packMcMeta, overlays, options.description());
            this.writeJsonSafely(packMcMeta, generatedPackPath.resolve("pack.mcmeta"));
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.generation_finished", String.valueOf(timestamp.deltaMillis())));
            return new GeneratedPack(fs, generatedPackPath, packMcMeta, overlays, options);
        } catch (IOException | RuntimeException | Error e) {
            fs.close();
            throw e;
        }
    }

    private void validateGeneratedPack(GeneratedPack pack, boolean obfuscation) {
        Timestamp timestamp = new Timestamp();
        this.validateResourcePack(pack.path(), pack.overlays(), obfuscation);
        if (pack.options() != null) {
            this.validatePackMetadata(pack.metadata(), pack.overlays(), pack.options().description());
            this.writeJsonSafely(pack.metadata(), pack.path().resolve("pack.mcmeta"));
        }
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.validation_finished", String.valueOf(timestamp.deltaMillis())));
    }

    private Path resolveWorkflowPath(String path) {
        return this.plugin.dataFolderPath().resolve(path).toAbsolutePath().normalize();
    }

    private void writePack(GeneratedPack pack, Path output, ZipGenerator writer, boolean protection, boolean storePng) throws IOException {
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.compression_started"));
        // 包含混淆、ZIP 写入和最终文件替换的耗时，仅在成功后输出完成提示。
        Timestamp timestamp = new Timestamp();
        Files.createDirectories(output.toAbsolutePath().getParent());
        // Upload only a complete result, keeping a previous successful file intact if writing fails.
        Path temporary = Files.createTempFile(output.toAbsolutePath().getParent(), ".pack-", ".zip");
        try {
            writer.generate(new PackZipRequest(pack.path(), temporary, protection, storePng));
            if (Files.size(temporary) == 0) throw new IOException("No resource pack was written: " + output);
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.compression_finished", String.valueOf(timestamp.deltaMillis())));
    }

    protected void loadHosts() {
        Object hostingObj = YamlUtils.reader(Config.instance().settings()).getValue("resource-pack.packs");
        try {
            ConfigSection packs = ConfigSection.of("resource-pack.packs", hostingObj == null ? Map.of() : hostingObj);
            Map<String, ResourcePackHost> hosts = new LinkedHashMap<>();
            Map<String, Path> selfHostedPacks = new LinkedHashMap<>();
            // 与工作流一样使用配置键作为 ID，并保留配置顺序作为资源包发送顺序。
            for (String id : packs.keySet()) {
                if (!id.matches("[A-Za-z0-9_-]{1,64}")) {
                    throw new IllegalArgumentException("Invalid resource pack host id: " + id);
                }
                ConfigSection section = packs.getNonNullSection(id);
                hosts.put(id, ResourcePackHosts.fromConfig(id, section));
                if (hosts.get(id) instanceof SelfHost selfHost) {
                    selfHostedPacks.put(id, selfHost.storagePath());
                }
            }
            if (!selfHostedPacks.isEmpty()) {
                Object serverConfig = YamlUtils.reader(Config.instance().settings()).getValue("resource-pack.self-host");
                SelfHostHttpServer.instance().load(ConfigSection.of("resource-pack.self-host", serverConfig == null ? Map.of() : serverConfig), selfHostedPacks);
            } else {
                SelfHostHttpServer.instance().disable();
                SelfHostHttpServer.instance().clearPacks();
            }
            this.resourcePackHosts = Collections.unmodifiableMap(hosts);
            this.resourcePackHost = hosts.isEmpty() ? NoneHost.INSTANCE : new ResourcePackHostGroup(hosts.values());
            this.loadPackSelection(packs);
        } catch (KnownResourceException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to load resource pack hosts", e);
        }
    }

    private void loadPackSelection(ConfigSection packs) {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        for (String id : this.resourcePackHosts.keySet()) {
            defaults.put(id, true);
            try {
                defaults.put(id, packs.getNonNullSection(id).getBoolean("default", true));
            } catch (KnownResourceException e) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to load resource pack default: " + id, e);
            }
        }
        // Use the current hosts and defaults even if dependency/conflict rules cannot be loaded.
        this.packSelection = new PackSelection(defaults, Map.of(), Map.of());
        try {
            Map<String, List<String>> dependencies = new LinkedHashMap<>();
            Map<String, List<String>> conflicts = new LinkedHashMap<>();
            for (String id : defaults.keySet()) {
                ConfigSection section = packs.getNonNullSection(id);
                dependencies.put(id, section.getStringList("dependencies", List.of()));
                conflicts.put(id, section.getStringList("conflicts", List.of()));
            }
            this.packSelection = new PackSelection(defaults, dependencies, conflicts);
        } catch (KnownResourceException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to load resource pack selection rules", e);
        }
    }

    private void loadPackPresets() {
        Map<String, PackPreset> presets = new LinkedHashMap<>();
        try {
            Object value = YamlUtils.reader(Config.instance().settings()).getValue("resource-pack.presets");
            ConfigSection section = ConfigSection.of("resource-pack.presets", value == null ? Map.of() : value);
            for (String name : section.keySet()) {
                try {
                    presets.put(name, PackPreset.fromConfig(name, Objects.requireNonNull(section.getValue(name))));
                } catch (KnownResourceException e) {
                    this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
                } catch (Exception e) {
                    this.plugin.logger().warn("Failed to load resource pack preset: " + name, e);
                }
            }
        } catch (KnownResourceException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
        }
        this.packPresets = Collections.unmodifiableMap(presets);
    }

    private void loadWorkflows() {
        Object value = YamlUtils.reader(Config.instance().settings()).getValue("resource-pack.workflows");
        ConfigSection section = ConfigSection.of("resource-pack.workflows", value == null ? Map.of() : value);
        Map<String, PackWorkflowSequence> workflows = new LinkedHashMap<>();
        for (String name : section.keySet()) {
            try {
                PackWorkflowValidation validation = new PackWorkflowValidation(this.plugin.dataFolderPath(), this.resourcePackHosts);
                workflows.put(name, PackWorkflowSequence.fromConfig(name, Objects.requireNonNull(section.getValue(name)), validation));
            } catch (KnownResourceException e) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to load resource pack workflow: " + name, e);
            }
        }
        this.workflows = Collections.unmodifiableMap(workflows);
    }

    @Override
    public Collection<String> workflowNames() {
        return List.copyOf(this.workflows.keySet());
    }

    @Override
    public void triggerWorkflows(String event) throws Exception {
        try (var operation = this.plugin.resourceOperations().acquire()) {
            for (PackWorkflowSequence workflow : this.workflows.values()) {
                if (workflow.triggers().contains(event)) {
                    executeWorkflow(workflow);
                }
            }
        }
    }

    @Override
    public void runWorkflow(String name) throws Exception {
        try (var operation = this.plugin.resourceOperations().acquire()) {
            PackWorkflowSequence sequence = this.workflows.get(name);
            if (sequence == null) throw new IllegalArgumentException("Unknown resource pack workflow: " + name);
            executeWorkflow(sequence);
        }
    }

    private void executeWorkflow(PackWorkflowSequence sequence) throws Exception {
        try (WorkflowRun run = new WorkflowRun(this.zipGenerator, sequence.protection())) {
            sequence.execute(run);
        }
    }

    private final class WorkflowRun implements PackWorkflowContext, AutoCloseable {
        private GeneratedPack pack;
        private final ZipGenerator generator;
        private final boolean protection;

        private WorkflowRun(ZipGenerator generator, boolean protection) {
            this.generator = generator;
            this.protection = protection;
        }

        @Override
        public CraftEngine plugin() {
            return plugin;
        }

        @Override
        public Path resolvePath(String path) {
            return resolveWorkflowPath(path);
        }

        @Override
        public Path generatedPackPath() {
            if (this.pack == null) throw new IllegalStateException("No resource pack has been generated");
            return this.pack.path();
        }

        @Override
        public void generate(PackGenerationOptions options) throws IOException {
            this.pack = generatePackAssets(options);
        }

        @Override
        public void generateBedrockPack(String input, String output) throws IOException {
            Timestamp timestamp = new Timestamp();
            CraftEngine engine = this.plugin();
            Map<Key, Key> mappings = Config.obfuscateItemModel() ? ItemModelMappings.getMappings() : Map.of();
            List<BedrockItemSource> items = BedrockItemSource.collect(engine.itemManager().loadedItems().values(), mappings);
            BedrockPackGenerator.Result result = new BedrockPackGenerator().generate(
                    this.resolvePath(input),
                    this.resolvePath(output),
                    Config.packMaxVersion(),
                    items
            );
            engine.logger().info("Generated Bedrock resource pack " + output + ": " + result.items() + " items, " + result.models() + " models, " + result.skipped() + " skipped (" + timestamp.deltaMillis() + " ms)");
        }

        @Override
        public void loadZip(String path) throws IOException {
            Timestamp timestamp = new Timestamp();
            GeneratedPack loaded = GeneratedPack.loadZip(resolveWorkflowPath(path));
            // 成功读取新包后再替换，避免加载失败时丢失原有工作流资源。
            GeneratedPack previous = this.pack;
            this.pack = loaded;
            if (previous != null) previous.close();
            plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.zip_loaded", path, String.valueOf(timestamp.deltaMillis())));
        }

        @Override
        public void validatePack() {
            validateGeneratedPack(this.pack, this.protection);
        }

        @Override
        public void export(String path) throws IOException {
            Timestamp timestamp = new Timestamp();
            this.pack.export(resolveWorkflowPath(path));
            plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.export_finished", path, String.valueOf(timestamp.deltaMillis())));
        }

        @Override
        public void optimize() {
            optimizeResourcePack(this.pack.path());
        }

        @Override
        public void zip(String path, boolean protection, boolean storePng) throws IOException {
            Path output = resolveWorkflowPath(path);
            writePack(this.pack, output, this.generator, protection && VersionHelper.PREMIUM, storePng);
            dispatchGenerationEvent(this.pack.path(), output);
        }

        @Override
        public void upload(String pack, String path) throws IOException {
            Path source = resolveWorkflowPath(path);
            if (!Files.isRegularFile(source)) throw new IOException("Resource pack does not exist: " + source);
            plugin.logger().info(TranslationManager.instance().plainTranslation("host.upload_started"));
            Timestamp timestamp = new Timestamp();
            // 托管上传是异步操作，等待完成后再统计耗时，失败时不输出成功提示。
            resourcePackHosts.get(pack).upload(source).join();
            plugin.logger().info(TranslationManager.instance().plainTranslation("host.upload_finished", String.valueOf(timestamp.deltaMillis())));
        }

        @Override
        public void sendPack(String pack) {
            sendPackToUsers(pack).join();
        }

        @Override
        public void close() throws IOException {
            if (this.pack != null) this.pack.close();
        }
    }

    private void validatePackMetadata(JsonObject rawMeta, Overlays packOverlays, String description) {
        // 获取设定的最大和最小值
        PackVersion minVersion = Config.packMinVersion().packFormat();
        PackVersion maxVersion = Config.packMaxVersion().packFormat();

        // 设置pack
        {
            JsonObject packJson = new JsonObject();
            rawMeta.add("pack", packJson);
            JsonElement descriptionJson = AdventureHelper.componentToJsonElement(AdventureHelper.miniMessage().deserialize(AdventureHelper.legacyToMiniMessage(description)));
            packJson.add("description", descriptionJson);
            // 需要旧版本兼容性
            // https://minecraft.wiki/w/Java_Edition_25w31a
            if (minVersion.isBelow(PackVersion.PACK_FORMAT_CHANGE_VERSION)) {
                packJson.addProperty("pack_format", minVersion.major());
                JsonObject supportedVersions = new JsonObject();
                supportedVersions.addProperty("min_inclusive", minVersion.major());
                supportedVersions.addProperty("max_inclusive", maxVersion.major());
                packJson.add("supported_formats", supportedVersions);
            }
            // 到达了1.21.9
            if (maxVersion.isAtOrAbove(PackVersion.PACK_FORMAT_CHANGE_VERSION)) {
                // 同时要兼容低版本
                packJson.add("min_format", minVersion.getAsJsonArray());
                packJson.add("max_format", maxVersion.getAsJsonArray());
            }
        }

        // 验证overlay
        {
            List<Overlay> overlays = packOverlays.overlays();
            if (!overlays.isEmpty()) {
                boolean legacySupported = false; // https://minecraft.wiki/w/Java_Edition_25w31a
                for (Overlay overlay : overlays) {
                    if (overlay.minVersion().isBelow(PackVersion.PACK_FORMAT_CHANGE_VERSION)) {
                        legacySupported = true;
                        break;
                    }
                }
                JsonArray newOverlayEntries = new JsonArray();
                for (Overlay overlay : overlays) {
                    newOverlayEntries.add(overlay.getAsOverlayEntry(legacySupported));
                }
                JsonObject overlaysJson = new JsonObject();
                overlaysJson.add("entries", newOverlayEntries);
                rawMeta.add("overlays", overlaysJson);
            }
        }
    }

    // 这里都是随便写写的，重点在之后的校验里
    private void generateRevisionOverlays(JsonObject rawMeta, Set<Revision> revisions) {
        if (revisions.isEmpty()) {
            return;
        }
        JsonObject overlays;
        if (rawMeta.has("overlays")) {
            overlays = rawMeta.get("overlays").getAsJsonObject();
        } else {
            overlays = new JsonObject();
            rawMeta.add("overlays", overlays);
        }
        JsonArray entries;
        if (overlays.has("entries")) {
            entries = overlays.get("entries").getAsJsonArray();
        } else {
            entries = new JsonArray();
            overlays.add("entries", entries);
        }
        for (Revision revision : revisions) {
            JsonObject entry = new JsonObject();
            JsonArray formatsArray = new JsonArray();
            entry.add("formats", formatsArray);
            formatsArray.add(revision.minPackVersion().major());
            formatsArray.add(revision.maxPackVersion().major());
            entry.add("min_format", revision.minPackVersion().getAsJsonArray());
            entry.add("max_format", revision.maxPackVersion().getAsJsonArray());
            entry.addProperty("directory", Config.createOverlayFolderName(revision.versionString()));
            entries.add(entry);
        }
    }

    private void removeAllShaders(Path path) {
        List<Path> rootPaths;
        try {
            rootPaths = MiscUtils.init(FileUtils.collectOverlays(path), a -> a.addFirst(path));
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to collect overlays for " + path.toAbsolutePath(), e);
            return;
        }
        for (Path rootPath : rootPaths) {
            Path shadersPath = rootPath.resolve("assets/minecraft/shaders");
            try {
                FileUtils.deleteDirectory(shadersPath);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to delete shaders directory for " + shadersPath.toAbsolutePath(), e);
            }
        }
    }

    private Predicate<String> parseExcludePredicate(Set<String> exclude) {
        List<String> excludeFolders = new ArrayList<>();
        exclude.removeIf(it -> {
            if (it.endsWith("/")) {
                excludeFolders.add(it);
                return true;
            }
            return false;
        });
        Predicate<String> folderPredicate;
        if (excludeFolders.isEmpty()) {
            folderPredicate = (s) -> false;
        } else {
            folderPredicate = (s) -> {
                for (String folder : excludeFolders) {
                    if (s.startsWith(folder)) {
                        return true;
                    }
                }
                return false;
            };
        }
        return folderPredicate;
    }

    @SuppressWarnings("DuplicatedCode")
    private void optimizeResourcePack(Path path) {
        Timestamp timestamp = new Timestamp();
        this.optimizationCache.setMaximumBytes(Config.optimizationCacheSize());
        Path optimizationCachePath = this.plugin.dataFolderPath().resolve("cache").resolve("pack-optimization.bin");
        try {
            this.optimizationCache.load(optimizationCachePath);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load pack optimization cache; resources will be optimized again", e);
        }
        // 收集全部overlay
        Path[] rootPaths;
        try {
            rootPaths = MiscUtils.init(FileUtils.collectOverlays(path), a -> a.addFirst(path)).toArray(new Path[0]);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to collect overlays for " + path.toAbsolutePath(), e);
            return;
        }

        List<Path> imagesToOptimize = new ArrayList<>();
        List<Path> commonJsonToOptimize = new ArrayList<>();
        List<Path> modelJsonToOptimize = new ArrayList<>();
        Set<String> excludeTexture = new HashSet<>(Config.optimizeTextureExclude());
        Set<String> excludeJson = new HashSet<>(Config.optimizeJsonExclude());
        excludeTexture.addAll(this.skipOptimizationParser.excludeTexture());
        excludeJson.addAll(this.skipOptimizationParser.excludeJson());
        Predicate<String> textureFolderPredicate = parseExcludePredicate(excludeTexture);
        Predicate<String> jsonFolderPredicate = parseExcludePredicate(excludeJson);

        Predicate<Path> textureExcluder = p -> {
            Path relativize = path.relativize(p);
            String relativizePath = CharacterUtils.replaceBackslashWithSlash(relativize.toString());
            if (excludeTexture.contains(relativizePath)) {
                return true;
            }
            return textureFolderPredicate.test(relativizePath);
        };
        Predicate<Path> jsonExcluder = p -> {
            Path relativize = path.relativize(p);
            String relativizePath = CharacterUtils.replaceBackslashWithSlash(relativize.toString());
            if (excludeJson.contains(relativizePath)) {
                return true;
            }
            return jsonFolderPredicate.test(relativizePath);
        };

        if (Config.optimizeJson()) {
            Path metaPath = path.resolve("pack.mcmeta");
            if (Files.exists(metaPath)) {
                if (!jsonExcluder.test(metaPath)) {
                    commonJsonToOptimize.add(metaPath);
                }
            }
        }

        if (Config.optimizeTexture()) {
            Path packPngPath = path.resolve("pack.png");
            if (Files.exists(packPngPath)) {
                if (textureExcluder.test(packPngPath)) {
                    imagesToOptimize.add(packPngPath);
                }
            }
        }

        for (Path rootPath : rootPaths) {
            Path assetsPath = rootPath.resolve("assets");
            if (!Files.isDirectory(assetsPath)) continue;

            // 收集全部命名空间
            List<Path> namespaces;
            try {
                namespaces = FileUtils.collectNamespaces(assetsPath);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to collect namespaces for " + assetsPath.toAbsolutePath(), e);
                return;
            }

            for (Path namespacePath : namespaces) {
                // 优化json
                if (Config.optimizeJson()) {

                    // 普通的json文件
                    for (String folder : List.of("atlases", "blockstates", "equipment", "font", "items", "lang", "particles", "post_effect", "texts", "waypoint_style")) {
                        // json文件夹
                        Path targetFolder = namespacePath.resolve(folder);
                        if (Files.isDirectory(targetFolder)) {
                            try {
                                Files.walkFileTree(targetFolder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                                    @Override
                                    public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)  {
                                        if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                        if (jsonExcluder.test(file)) return FileVisitResult.CONTINUE;
                                        commonJsonToOptimize.add(file);
                                        return FileVisitResult.CONTINUE;
                                    }
                                });
                            } catch (IOException e) {
                                this.plugin.logger().warn("Failed to walk through " + folder, e);
                            }
                        }
                    }

                    // 模型文件夹
                    Path modelsFolder = namespacePath.resolve("models");
                    if (Files.isDirectory(modelsFolder)) {
                        try {
                            Files.walkFileTree(modelsFolder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                                @Override
                                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)  {
                                    if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                    if (jsonExcluder.test(file)) return FileVisitResult.CONTINUE;
                                    modelJsonToOptimize.add(file);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to walk through models", e);
                        }
                    }
                }

                // 优化贴图
                if (Config.optimizeTexture() || Config.optimizeJson()) {
                    Path texturesFolder = namespacePath.resolve("textures");
                    if (Files.isDirectory(texturesFolder)) {
                        try {
                            Files.walkFileTree(texturesFolder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                                @Override
                                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)  {
                                    if (FileUtils.isPngFile(file)) {
                                        if (Config.optimizeTexture() && !textureExcluder.test(file)) {
                                            imagesToOptimize.add(file);
                                        }
                                    } else if (FileUtils.isMcMetaFile(file) && Config.optimizeJson()) {
                                        if (jsonExcluder.test(file)) return FileVisitResult.CONTINUE;
                                        commonJsonToOptimize.add(file);
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to walk through textures", e);
                        }
                    }
                }
            }
        }

        if (Config.optimizeJson()) {
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.json_optimization_started"));
            AtomicLong previousBytes = new AtomicLong(0L);
            AtomicLong afterBytes = new AtomicLong(0L);
            int amount = commonJsonToOptimize.size() + modelJsonToOptimize.size();
            AtomicInteger finished = new AtomicInteger(0);
            ForkJoinPool executor = (ForkJoinPool) this.plugin.scheduler().async();
            CompletableFuture<Void> commonFuture = CompletableFutures.forEachAsync(commonJsonToOptimize, jsonPath -> {
                try {
                    byte[] before = Files.readAllBytes(jsonPath);
                    previousBytes.getAndAdd(before.length);
                    byte[] after = this.optimizationCache.optimize(PackOptimizationCache.Type.JSON, 0, before,
                            () -> optimizeJson(before, false));
                    if (after.length < before.length) {
                        afterBytes.addAndGet(after.length);
                        Files.write(jsonPath, after);
                    } else {
                        afterBytes.addAndGet(before.length);
                    }
                    finished.incrementAndGet();
                } catch (IOException | JsonParseException | NullPointerException ignored) {
                }
            }, executor.getParallelism(), executor);
            CompletableFuture<Void> modelFuture = CompletableFutures.forEachAsync(modelJsonToOptimize, jsonPath -> {
                try {
                    byte[] before = Files.readAllBytes(jsonPath);
                    previousBytes.getAndAdd(before.length);
                    byte[] after = this.optimizationCache.optimize(PackOptimizationCache.Type.MODEL_JSON, 0, before,
                            () -> optimizeJson(before, true));
                    if (after.length < before.length) {
                        afterBytes.addAndGet(after.length);
                        Files.write(jsonPath, after);
                    } else {
                        afterBytes.addAndGet(before.length);
                    }
                    finished.incrementAndGet();
                } catch (IOException | JsonParseException | IllegalStateException | NullPointerException ignored) {
                }
            }, executor.getParallelism(), executor);

            CompletableFuture<Void> overallFuture = CompletableFuture.allOf(commonFuture, modelFuture);
            long startTime = System.currentTimeMillis();
            for (;;) {
                try {
                    overallFuture.get(1, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    this.plugin.logger().warn("Failed to optimize json files", e);
                    break;
                } catch (TimeoutException e) {
                    this.plugin.logger().info(createProgressBar(finished.get(), amount, String.valueOf((int) ((System.currentTimeMillis() - startTime) / 1000))));
                    continue;
                }
                this.plugin.logger().info(createProgressBar(finished.get(), amount, String.format("%.1f", ((System.currentTimeMillis() - startTime) / 1000.0))));
                break;
            }

            long originalSize = previousBytes.get();
            long optimizedSize = afterBytes.get();
            double compressionRatio = ((double) optimizedSize / originalSize) * 100;
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.optimization_result", formatSize(originalSize), formatSize(optimizedSize), String.format("%.2f", compressionRatio)));
        }

        if (Config.optimizeTexture()) {
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.texture_optimization_started"));
            int zopfliIterations = Config.zopfliIterations();
            AtomicLong previousBytes = new AtomicLong(0L);
            AtomicLong afterBytes = new AtomicLong(0L);
            int amount = imagesToOptimize.size();
            AtomicInteger finished = new AtomicInteger(0);
            ForkJoinPool executor = (ForkJoinPool) this.plugin.scheduler().async();
            CompletableFuture<Void> overallFuture = CompletableFutures.forEachAsync(imagesToOptimize, imagePath -> {
                try {
                    byte[] previousImageBytes = Files.readAllBytes(imagePath);
                    byte[] optimized = this.optimizationCache.optimize(PackOptimizationCache.Type.PNG, zopfliIterations, previousImageBytes,
                            () -> optimizeImage(imagePath, previousImageBytes, zopfliIterations));
                    previousBytes.addAndGet(previousImageBytes.length);
                    if (optimized.length < previousImageBytes.length) {
                        afterBytes.addAndGet(optimized.length);
                        Files.write(imagePath, optimized);
                    } else {
                        afterBytes.addAndGet(previousImageBytes.length);
                    }
                    finished.incrementAndGet();
                } catch (IOException ignored) {
                }
            }, executor.getParallelism(), executor);
            long startTime = System.currentTimeMillis();
            for (;;) {
                try {
                    overallFuture.get(1, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    this.plugin.logger().warn("Failed to optimize images", e);
                    break;
                } catch (TimeoutException e) {
                    this.plugin.logger().info(createProgressBar(finished.get(), amount, String.valueOf((int) ((System.currentTimeMillis() - startTime) / 1000))));
                    continue;
                }
                this.plugin.logger().info(createProgressBar(finished.get(), amount, String.format("%.1f", ((System.currentTimeMillis() - startTime) / 1000.0))));
                break;
            }

            long originalSize = previousBytes.get();
            long optimizedSize = afterBytes.get();
            double compressionRatio = ((double) optimizedSize / originalSize) * 100;
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.optimization_result", formatSize(originalSize), formatSize(optimizedSize), String.format("%.2f", compressionRatio)));
        }
        try {
            this.optimizationCache.save(optimizationCachePath);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to save pack optimization cache", e);
        }
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.optimization_finished", String.valueOf(timestamp.deltaMillis())));
    }

    private static final int BAR_LENGTH = 30;

    private String createProgressBar(int current, int total, String elapsed) {
        double progress = (double) current / total;
        int filledLength = (int) (BAR_LENGTH * progress);
        int emptyLength = BAR_LENGTH - filledLength;
        String progressBar = "[" +
                "=".repeat(Math.max(0, filledLength)) +
                " ".repeat(Math.max(0, emptyLength)) +
                "]";
        return String.format(
                "%s %d/%d (%.1f%%) | %ss",
                progressBar,
                current,
                total,
                progress * 100,
                elapsed
        );
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private static byte[] optimizeJson(byte[] input, boolean model) {
        JsonElement json = GsonHelper.parseJson(new String(input, StandardCharsets.UTF_8));
        if (model) {
            ModelJsonOptimizer.optimize(json.getAsJsonObject());
        }
        return PackJsonWriter.toJson(json).replace("\"minecraft:", "\"").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] optimizeImage(Path imagePath, byte[] previousImageBytes, int zopfliIterations) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(previousImageBytes)) {
            BufferedImage src = PngOptimizer.readPng(is);
            if (src == null) {
                Debugger.RESOURCE_PACK.debug(() -> "Cannot read image " + imagePath.toString());
                return previousImageBytes;
            }
            if (!PngOptimizer.canOptimize(src)) {
                return previousImageBytes;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new PngOptimizer(src, zopfliIterations).write(baos);
            return baos.toByteArray();
        }
    }

    private void validateResourcePack(Path path, Overlays packOverlays, boolean obfuscation) {
        ValidationResources resources = new ValidationResources(this.vanillaBlockAtlas, this.vanillaItemAtlas, PRESET_MODELS, VANILLA_MODELS, VANILLA_TEXTURES, VANILLA_SOUNDS);
        new PackValidator(this.plugin.logger(), resources).validate(path, packOverlays, obfuscation);
    }

    private JsonObject readJsonObjectFromFileOrWarn(Path path) {
        return ResourcePackJsonUtils.readJsonObjectOrWarn(this.plugin.logger(), path);
    }

    public TexturedModel getTexturedModel(Key currentRL,
                                          @Nullable Key parentRL,
                                          Map<String, Key> textures,
                                          Path[] rootPaths,
                                          Map<Key, TexturedModel> cached) {
        return new ValidationModelResolver(this.plugin.logger(), VANILLA_MODELS, PRESET_MODELS)
                .getTexturedModel(currentRL, parentRL, textures, rootPaths, cached);
    }

    public TexturedModel getTexturedModel(Key currentRL,
                                          SimplifiedModelFile model,
                                          Path[] rootPaths,
                                          Map<Key, TexturedModel> cached) {
        return new ValidationModelResolver(this.plugin.logger(), VANILLA_MODELS, PRESET_MODELS)
                .getTexturedModel(currentRL, model, rootPaths, cached);
    }

    public record ModelFixFlags(boolean fixMissingTexture, boolean fixTexturesFormat, boolean fixRotationAngle, boolean fixElement, boolean v1_21_6) {}

    private void generateParticle(Path generatedPackPath) {
        if (!Config.removeTintedLeavesParticle()) return;
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_5)) return;
        JsonObject particleJson = new JsonObject();
        JsonArray textures = new JsonArray();
        textures.add("empty");
        particleJson.add("textures", textures);
        Path jsonPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("particles")
                .resolve("tinted_leaves.json");
        Path pngPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("textures")
                .resolve("particle")
                .resolve("empty.png");
        try {
            Files.createDirectories(jsonPath.getParent());
            Files.createDirectories(pngPath.getParent());
        } catch (IOException e) {
            this.plugin.logger().error("Error creating directories", e);
            return;
        }
        try {
            GsonHelper.writeJsonFile(particleJson, jsonPath);
            Files.write(pngPath, EMPTY_1X1_IMAGE);
        } catch (IOException e) {
            this.plugin.logger().error("Error writing particles file", e);
        }
    }

    private void generateAtlases(Path generatedPackPath) {
        Map<Key, List<SpriteSource>> atlases = this.atlasConfigParser.atlases();
        if (atlases.isEmpty()) return;
        for (Map.Entry<Key, List<SpriteSource>> entry : atlases.entrySet()) {
            Key atlasId = entry.getKey();
            Path atlasPath = generatedPackPath
                    .resolve("assets")
                    .resolve(atlasId.namespace())
                    .resolve("atlases")
                    .resolve(atlasId.value() + ".json");
            List<SpriteSource> sources = new ArrayList<>();
            if (Files.exists(atlasPath)) {
                JsonObject existing = readJsonObjectFromFileOrWarn(atlasPath);
                if (existing != null) {
                    sources.addAll(AtlasParser.parse(existing).sources());
                }
            }
            sources.addAll(entry.getValue());
            writeJsonSafely(new AtlasData(sources).optimize().get(), atlasPath);
        }
    }

    private void generateEquipments(Path generatedPackPath, Consumer<Overlay> callback) {
        // asset id + 是否有上身 + 是否有腿
        List<Tuple<Key, Boolean, Boolean>> collectedTrims = new ArrayList<>();

        // 为trim类型提供的两个兼容性值
        boolean needLegacyCompatibility = Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2);
        boolean needModernCompatibility = Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2);
        boolean needModernTrimAtlas = needModernCompatibility && Config.packMinVersion().packFormat().isBelow(PackVersion.PALETTE_TEXTURES_VERSION);

        for (Equipment equipment : this.plugin.itemManager().equipments().values()) {
            if (equipment instanceof ComponentBasedEquipment componentBasedEquipment) {
                // 现代的盔甲生成
                processComponentBasedEquipment(componentBasedEquipment, generatedPackPath);
            } else if (equipment instanceof TrimBasedEquipment trimBasedEquipment) {
                Key assetId = trimBasedEquipment.assetId();
                Pair<Boolean, Boolean> result = processTrimBasedEquipment(trimBasedEquipment, generatedPackPath);
                if (result != null) {
                    collectedTrims.add(Tuple.of(assetId, result.left(), result.right()));
                }
            }
        }

        if (!collectedTrims.isEmpty()) {
            // 获取基础atlas路径
            Path atlasPath = generatedPackPath
                    .resolve("assets")
                    .resolve("minecraft")
                    .resolve("atlases")
                    .resolve("armor_trims.json");
            // 读取先前sources内容
            JsonArray previousAtlasSources = null;
            if (Files.exists(atlasPath) && Files.isRegularFile(atlasPath)) {
                try {
                    previousAtlasSources = GsonHelper.readJsonFromFile(atlasPath).getAsJsonObject().getAsJsonArray("sources");
                } catch (ClassCastException | IllegalStateException | IOException | JsonParseException ignored) {
                }
            }

            // 修复被干碎的原版盔甲
            Key vanillaFixTrimType = Key.of("minecraft", Config.sacrificedVanillaArmorType());
            collectedTrims.add(Tuple.of(vanillaFixTrimType, true, true));
            processTrimBasedEquipment(new TrimBasedEquipment(vanillaFixTrimType, Config.sacrificedHumanoid(), Config.sacrificedHumanoidLeggings()), generatedPackPath);

            // 准备新版本atlas和覆盖纹理
            JsonObject modernTrimAtlasJson = null;
            if (needModernTrimAtlas) {
                modernTrimAtlasJson = new JsonObject();
                JsonArray sourcesArray = new JsonArray();
                modernTrimAtlasJson.add("sources", sourcesArray);
                for (Tuple<Key, Boolean, Boolean> tuple : collectedTrims) {
                    if (tuple.mid()) {
                        JsonObject single1 = new JsonObject();
                        single1.addProperty("type", "single");
                        single1.addProperty("resource", tuple.left().namespace() + ":trims/entity/humanoid/" + tuple.left().value() + "_" + NEW_TRIM_MATERIAL);
                        sourcesArray.add(single1);
                    }
                    if (tuple.right()) {
                        JsonObject single2 = new JsonObject();
                        single2.addProperty("type", "single");
                        single2.addProperty("resource", tuple.left().namespace() + ":trims/entity/humanoid_leggings/" + tuple.left().value() + "_" + NEW_TRIM_MATERIAL);
                        sourcesArray.add(single2);
                    }
                }
                if (previousAtlasSources != null) {
                    sourcesArray.addAll(previousAtlasSources);
                }
            }
            if (needModernCompatibility) {
                Path vanillaArmorPath1 = generatedPackPath
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("textures")
                        .resolve("entity")
                        .resolve("equipment")
                        .resolve("humanoid")
                        .resolve(Config.sacrificedVanillaArmorType() + ".png");
                Path vanillaArmorPath2 = generatedPackPath
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("textures")
                        .resolve("entity")
                        .resolve("equipment")
                        .resolve("humanoid_leggings")
                        .resolve(Config.sacrificedVanillaArmorType() + ".png");
                try {
                    Files.createDirectories(vanillaArmorPath1.getParent());
                    Files.createDirectories(vanillaArmorPath2.getParent());
                    Files.write(vanillaArmorPath1, EMPTY_EQUIPMENT_IMAGE);
                    Files.write(vanillaArmorPath2, EMPTY_EQUIPMENT_IMAGE);
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write empty vanilla armor texture file", e);
                }
            }

            // 准备旧版本atlas和覆盖纹理
            JsonObject legacyTrimAtlasJson = null;
            if (needLegacyCompatibility) {
                legacyTrimAtlasJson = new JsonObject();
                JsonArray sourcesArray = new JsonArray();
                legacyTrimAtlasJson.add("sources", sourcesArray);
                for (Tuple<Key, Boolean, Boolean> tuple : collectedTrims) {
                    if (tuple.mid()) {
                        JsonObject single1 = new JsonObject();
                        single1.addProperty("type", "single");
                        single1.addProperty("resource", tuple.left().namespace() + ":trims/models/armor/" + tuple.left().value() + "_" + NEW_TRIM_MATERIAL);
                        sourcesArray.add(single1);
                    }
                    if (tuple.right()) {
                        JsonObject single2 = new JsonObject();
                        single2.addProperty("type", "single");
                        single2.addProperty("resource", tuple.left().namespace() + ":trims/models/armor/" + tuple.left().value() + "_leggings_" + NEW_TRIM_MATERIAL);
                        sourcesArray.add(single2);
                    }
                }
                if (previousAtlasSources != null) {
                    sourcesArray.addAll(previousAtlasSources);
                }
                Path vanillaArmorPath1 = generatedPackPath
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("textures")
                        .resolve("models")
                        .resolve("armor")
                        .resolve(Config.sacrificedVanillaArmorType() + "_layer_1.png");
                Path vanillaArmorPath2 = generatedPackPath
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("textures")
                        .resolve("models")
                        .resolve("armor")
                        .resolve(Config.sacrificedVanillaArmorType() + "_layer_2.png");
                try {
                    Files.createDirectories(vanillaArmorPath1.getParent());
                    Files.write(vanillaArmorPath1, EMPTY_EQUIPMENT_IMAGE);
                    Files.write(vanillaArmorPath2, EMPTY_EQUIPMENT_IMAGE);
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write empty vanilla armor texture file", e);
                }
            }
            // 写入atlas文件, 优先写入旧版
            if (needLegacyCompatibility || needModernTrimAtlas) {
                writeJsonSafely(needLegacyCompatibility ? legacyTrimAtlasJson : modernTrimAtlasJson, atlasPath);
                if (needLegacyCompatibility && needModernTrimAtlas) {
                    Overlay overlay = Revisions.SINCE_1_21_2.createOverlay();
                    Path overlayAtlasPath = generatedPackPath.resolve(overlay.directory()).resolve(generatedPackPath.relativize(atlasPath));
                    writeJsonSafely(modernTrimAtlasJson, overlayAtlasPath);
                    callback.accept(overlay);
                }
                if (Config.packMaxVersion().packFormat().isAtOrAbove(PackVersion.PALETTE_TEXTURES_VERSION)) {
                    String directory = Config.createOverlayFolderName(PackVersion.PALETTE_TEXTURES_VERSION.asString());
                    JsonObject emptyAtlas = new JsonObject();
                    emptyAtlas.add("sources", new JsonArray());
                    writeJsonSafely(emptyAtlas, generatedPackPath.resolve(directory).resolve(generatedPackPath.relativize(atlasPath)));
                    callback.accept(new Overlay(PackVersion.PALETTE_TEXTURES_VERSION, PackVersion.MAX_PACK_VERSION, directory));
                }
            }
        }
    }

    private void processComponentBasedEquipment(ComponentBasedEquipment componentBasedEquipment, Path generatedPackPath) {
        Key assetId = componentBasedEquipment.assetId();
        if (assetId == null) {
            this.plugin.logger().error("Asset id is null for equipment " + componentBasedEquipment);
            return;
        }

        // 纠正路径
        for (Map.Entry<EquipmentLayerType, List<ComponentBasedEquipment.Layer>> entry : componentBasedEquipment.layers().entrySet()) {
            for (ComponentBasedEquipment.Layer layer : entry.getValue()) {
                Key texture = layer.texture();
                Path badPath = generatedPackPath
                        .resolve("assets")
                        .resolve(texture.namespace())
                        .resolve("textures")
                        .resolve(texture.value() + ".png");
                Path correctPath = generatedPackPath
                        .resolve("assets")
                        .resolve(texture.namespace())
                        .resolve("textures")
                        .resolve("entity")
                        .resolve("equipment")
                        .resolve(entry.getKey().id())
                        .resolve(texture.value() + ".png");
                if (Files.exists(badPath) && !Files.exists(correctPath)) {
                    try {
                        Files.createDirectories(correctPath.getParent());
                        Files.move(badPath, correctPath);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error creating " + correctPath.toAbsolutePath());
                        return;
                    }
                }
            }
        }

        if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_4)) {
            Path equipmentPath = generatedPackPath
                    .resolve("assets")
                    .resolve(assetId.namespace())
                    .resolve("equipment")
                    .resolve(assetId.value() + ".json");

            JsonObject equipmentJson = null;
            if (Files.exists(equipmentPath)) {
                try (BufferedReader reader = Files.newBufferedReader(equipmentPath)) {
                    equipmentJson = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to load existing sounds.json", e);
                    return;
                }
            }
            if (equipmentJson != null) {
                equipmentJson = GsonHelper.deepMerge(equipmentJson, componentBasedEquipment.get());
            } else {
                equipmentJson = componentBasedEquipment.get();
            }
            try {
                Files.createDirectories(equipmentPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().error("Error creating " + equipmentPath.toAbsolutePath());
                return;
            }
            try {
                GsonHelper.writeJsonFile(equipmentJson, equipmentPath);
            } catch (IOException e) {
                this.plugin.logger().error("Error writing equipment file", e);
            }
        }
        if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2) && Config.packMinVersion().isBelow(MinecraftVersion.V1_21_4)) {
            Path equipmentPath = generatedPackPath
                    .resolve("assets")
                    .resolve(assetId.namespace())
                    .resolve("models")
                    .resolve("equipment")
                    .resolve(assetId.value() + ".json");

            JsonObject equipmentJson = null;
            if (Files.exists(equipmentPath)) {
                try (BufferedReader reader = Files.newBufferedReader(equipmentPath)) {
                    equipmentJson = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to load existing sounds.json", e);
                    return;
                }
            }
            if (equipmentJson != null) {
                equipmentJson = GsonHelper.deepMerge(equipmentJson, componentBasedEquipment.get());
            } else {
                equipmentJson = componentBasedEquipment.get();
            }
            try {
                Files.createDirectories(equipmentPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().error("Error creating " + equipmentPath.toAbsolutePath());
                return;
            }
            try {
                GsonHelper.writeJsonFile(equipmentJson, equipmentPath);
            } catch (IOException e) {
                this.plugin.logger().error("Error writing equipment file", e);
            }
        }
    }

    @Nullable
    private Pair<Boolean, Boolean> processTrimBasedEquipment(TrimBasedEquipment equipment, Path generatedPackPath) {
        Map<Path, List<Path>> copies = new LinkedHashMap<>();
        if (!collectTrimTextureCopies(generatedPackPath, equipment.assetId(), equipment.humanoid(), false, copies) || !collectTrimTextureCopies(generatedPackPath, equipment.assetId(), equipment.humanoidLeggings(), true, copies)) {
            return null;
        }

        Set<Path> targets = new HashSet<>();
        // 先复制所有版本、所有层的纹理，再清理源文件；源文件也可能是 26.3 的目标。
        for (Map.Entry<Path, List<Path>> entry : copies.entrySet()) {
            Path source = entry.getKey();
            for (Path target : entry.getValue()) {
                targets.add(target);
                if (!source.equals(target)) {
                    try {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error writing armor texture file from " + source + " to " + target, e);
                        return null;
                    }
                }
            }
        }
        for (Path source : copies.keySet()) {
            if (!targets.contains(source)) {
                try {
                    Files.delete(source);
                } catch (IOException e) {
                    this.plugin.logger().error("Error deleting armor texture file from " + source, e);
                }
            }
        }
        return Pair.of(equipment.humanoid() != null, equipment.humanoidLeggings() != null);
    }

    private boolean collectTrimTextureCopies(Path root,
                                             Key assetId,
                                             @Nullable Key textureId,
                                             boolean leggings,
                                             Map<Path, List<Path>> copies) {
        if (textureId == null) {
            return true;
        }
        Path source = root.resolve("assets").resolve(textureId.namespace()).resolve("textures").resolve(textureId.value() + ".png");
        if (!Files.isRegularFile(source)) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                    "resource_pack.missing_equipment_texture", assetId.asString(), source.toString()
            ));
            return false;
        }
        Path trims = root.resolve("assets").resolve(assetId.namespace()).resolve("textures").resolve("trims");
        List<Path> targets = copies.computeIfAbsent(source, ignored -> new ArrayList<>(3));
        if (Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2)) {
            String suffix = leggings ? "_leggings_" : "_";
            targets.add(trims.resolve("models/armor").resolve(assetId.value() + suffix + NEW_TRIM_MATERIAL + ".png"));
        }
        Path entityTrims = trims.resolve(leggings ? "entity/humanoid_leggings" : "entity/humanoid");
        if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2) && Config.packMinVersion().packFormat().isBelow(PackVersion.PALETTE_TEXTURES_VERSION)) {
            targets.add(entityTrims.resolve(assetId.value() + "_" + NEW_TRIM_MATERIAL + ".png"));
        }
        if (Config.packMaxVersion().packFormat().isAtOrAbove(PackVersion.PALETTE_TEXTURES_VERSION)) {
            targets.add(entityTrims.resolve(assetId.value() + ".png"));
        }
        return true;
    }

    private void generateEmptyBlockModel(Path generatedPackPath) {
        if (!this.plugin.blockManager().isTransparentModelInUse()) return;
        Path modelPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("models")
                .resolve("block")
                .resolve("empty.json");
        Path texturePath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("textures")
                .resolve("block")
                .resolve("empty.png");
        try {
            Files.createDirectories(modelPath.getParent());
            Files.writeString(modelPath, "{\"textures\":{\"particle\":\"block/empty\"},\"elements\":[{\"from\":[0,0,0],\"to\":[0,0,0],\"color\":0,\"faces\":{\"north\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},\"east\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},\"south\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},\"west\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},\"up\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"},\"down\":{\"uv\":[0,0,0,0],\"texture\":\"#particle\"}}}]}");
        } catch (IOException e) {
            this.plugin.logger().error("Error writing empty block model", e);
        }
        try {
            Files.createDirectories(texturePath.getParent());
            Files.write(texturePath, EMPTY_16X16_IMAGE);
        } catch (IOException e) {
            this.plugin.logger().error("Error writing empty block texture", e);
        }
    }

    private void generateClientLang(Path generatedPackPath) {
        for (Map.Entry<String, ClientLangData> entry : this.plugin.translationManager().clientLangData().entrySet()) {
            Path langPath = generatedPackPath
                    .resolve("assets")
                    .resolve("minecraft")
                    .resolve("lang")
                    .resolve(entry.getKey() + ".json");
            JsonObject json;
            if (Files.exists(langPath)) {
                try {
                    json = GsonHelper.readJsonFromFile(langPath).getAsJsonObject();
                } catch (Exception e) {
                    json = new JsonObject();
                }
            } else {
                json = new JsonObject();
            }
            Map<String, String> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonElement> existing : json.entrySet()) {
                if (existing.getValue().isJsonPrimitive()) {
                    sorted.put(existing.getKey(), existing.getValue().getAsString());
                }
            }
            sorted.putAll(entry.getValue().translations);
            JsonObject sortedJson = new JsonObject();
            for (Map.Entry<String, String> pair : sorted.entrySet()) {
                sortedJson.addProperty(pair.getKey(), pair.getValue());
            }
            writeJsonSafely(sortedJson, langPath);
        }
    }

    private void generateCustomSounds(Path generatedPackPath) {
        AbstractSoundManager soundManager = (AbstractSoundManager) this.plugin.soundManager();
        for (Map.Entry<String, List<SoundEvent>> entry : soundManager.soundsByNamespace().entrySet()) {
            Path soundPath = generatedPackPath
                    .resolve("assets")
                    .resolve(entry.getKey())
                    .resolve("sounds.json");
            JsonObject soundJson;
            if (Files.exists(soundPath)) {
                try (BufferedReader reader = Files.newBufferedReader(soundPath)) {
                    soundJson = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to load existing sounds.json", e);
                    return;
                }
            } else {
                soundJson = new JsonObject();
            }
            Map<String, JsonElement> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonElement> existing : soundJson.entrySet()) {
                sorted.put(existing.getKey(), existing.getValue());
            }
            for (SoundEvent soundEvent : entry.getValue()) {
                sorted.put(soundEvent.id().value(), soundEvent.get());
            }
            JsonObject sortedJson = new JsonObject();
            for (Map.Entry<String, JsonElement> pair : sorted.entrySet()) {
                sortedJson.add(pair.getKey(), pair.getValue());
            }
            writeJsonSafely(sortedJson, soundPath);
        }
    }

    private void generateOverrideSounds(Path generatedPackPath) {
        if (!Config.enableSoundSystem()) return;

        Path soundPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("sounds.json");

        JsonObject soundTemplate;
        try (InputStream inputStream = this.plugin.resourceStream("internal/sounds.json")) {
            if (inputStream == null) {
                this.plugin.logger().warn("Failed to load internal/sounds.json");
                return;
            }
            soundTemplate = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | IllegalStateException e) {
            this.plugin.logger().warn("Failed to load internal/sounds.json", e);
            return;
        }

        JsonObject soundJson;
        if (Files.exists(soundPath)) {
            try (BufferedReader reader = Files.newBufferedReader(soundPath)) {
                soundJson = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException | IllegalStateException e) {
                this.plugin.logger().warn("Failed to load existing sounds.json", e);
                return;
            }
        } else {
            soundJson = new JsonObject();
        }

        JsonObject empty = new JsonObject();
        JsonArray sounds = new JsonArray();
        if (!Config.silentSoundPath().isEmpty()) {
            sounds.add(Config.silentSoundPath());
            try (InputStream is = this.plugin.resourceStream("silence.ogg")) {
                Path finalSoundPath = generatedPackPath.resolve("assets").resolve("minecraft").resolve("sounds").resolve(Config.silentSoundPath() + ".ogg");
                Files.createDirectories(finalSoundPath.getParent());
                Files.copy(is, finalSoundPath);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to load silence.ogg", e);
            }
        }
        empty.add("sounds", sounds);
        empty.addProperty("replace", true);

        for (Map.Entry<Key, Key> mapper : this.plugin.blockManager().soundReplacements().entrySet()) {
            Key originalKey = mapper.getKey();

            soundJson.add(originalKey.value(), empty);
            try {
                JsonObject originalSounds = soundTemplate.getAsJsonObject(originalKey.value());
                if (originalSounds != null) {
                    soundJson.add(mapper.getValue().value(), originalSounds);
                } else {
                    this.plugin.logger().warn("Cannot find " + originalKey.value() + " in sound template");
                }
            } catch (ClassCastException e) {
                this.plugin.logger().warn("Failed to load existing sounds.json", e);
                return;
            }
        }

        Map<String, JsonElement> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonElement> existing : soundJson.entrySet()) {
            sorted.put(existing.getKey(), existing.getValue());
        }
        JsonObject sortedJson = new JsonObject();
        for (Map.Entry<String, JsonElement> pair : sorted.entrySet()) {
            sortedJson.add(pair.getKey(), pair.getValue());
        }
        writeJsonSafely(sortedJson, soundPath);
    }

    // 生成 json 模型文件
    private void generateModels(Path generatedPackPath, ModelGenerator generator) {
        for (Map.Entry<Key, ModelGeneration> entry : generator.modelsToGenerate().entrySet()) {
            Path modelPath = generatedPackPath
                    .resolve("assets")
                    .resolve(entry.getKey().namespace())
                    .resolve("models")
                    .resolve(entry.getKey().value() + ".json");
            if (Files.exists(modelPath)) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.model_generation.conflict", modelPath.toAbsolutePath().toString()));
                continue;
            }
            writeJsonSafely(entry.getValue().get(), modelPath);
        }
        for (Map.Entry<Key, byte[]> entry : generator.texturesToGenerate().entrySet()) {
            Path texturePath = generatedPackPath
                    .resolve("assets")
                    .resolve(entry.getKey().namespace())
                    .resolve("textures")
                    .resolve(entry.getKey().value() + ".png");
            if (Files.exists(texturePath)) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.texture_generation.conflict", texturePath.toAbsolutePath().toString()));
                continue;
            }
            try {
                Files.createDirectories(texturePath.getParent());
                Files.write(texturePath, entry.getValue());
                JsonObject metadata = generator.textureMetadataToGenerate().get(entry.getKey());
                if (metadata != null) {
                    Path metadataPath = texturePath.resolveSibling(texturePath.getFileName() + ".mcmeta");
                    if (!Files.exists(metadataPath)) writeJsonSafely(metadata, metadataPath);
                }
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to generate texture " + texturePath.toAbsolutePath(), e);
            }
        }
    }

    private void generateBlockOverrides(Path generatedPackPath, boolean generateVanillaAsset, boolean generateModAsset) {
        // 生成覆写原版方块状态的文件
        if (generateVanillaAsset) {
            for (Map.Entry<Key, Map<String, JsonElement>> entry : this.plugin.blockManager().blockOverrides().entrySet()) {
                Key key = entry.getKey();
                Path overridedBlockPath = generatedPackPath
                        .resolve("assets")
                        .resolve(key.namespace())
                        .resolve("blockstates")
                        .resolve(key.value() + ".json");

                JsonObject stateJson = null;
                JsonObject previousVariants = null;
                if (Files.exists(overridedBlockPath)) {
                    try {
                        stateJson = GsonHelper.readJsonFromFile(overridedBlockPath).getAsJsonObject();
                        if (stateJson.has("variants")) {
                            previousVariants = stateJson.get("variants").getAsJsonObject();
                        }
                        if (stateJson.has("multipart")) {
                            stateJson.remove("multipart");
                        }
                    } catch (Exception e) {
                        this.plugin.logger().warn("Unexpected error when reading block state json " + overridedBlockPath, e);
                    }
                }
                if (stateJson == null) {
                    stateJson = new JsonObject();
                }

                JsonObject newVariants = new JsonObject();
                if (previousVariants != null) {
                    for (Map.Entry<String, JsonElement> variantEntry : previousVariants.entrySet()) {
                        newVariants.add(variantEntry.getKey(), variantEntry.getValue());
                    }
                }
                // 新增变体排序后追加在末尾；与旧变体同 key 的仅替换值，保持原位
                Map<String, JsonElement> sortedNewVariants = new TreeMap<>(entry.getValue());
                for (Map.Entry<String, JsonElement> resourcePathEntry : sortedNewVariants.entrySet()) {
                    newVariants.add(resourcePathEntry.getKey(), resourcePathEntry.getValue());
                }
                stateJson.add("variants", newVariants);
                writeJsonSafely(stateJson, overridedBlockPath);
            }
        }

        // 生成模组资源，例如 craftengine:custom_0.json
        if (generateModAsset) {
            Path statesPath = generatedPackPath
                    .resolve("assets")
                    .resolve(Key.CRAFTENGINE_NAMESPACE)
                    .resolve("blockstates");
            JsonObject blueMapBlockStates = new JsonObject();
            int vanillaBlockStateCount = this.plugin.blockManager().vanillaBlockStateCount();
            for (Map.Entry<Integer, JsonElement> entry : this.plugin.blockManager().modBlockStates().entrySet()) {
                JsonObject stateJson = new JsonObject();
                JsonObject variants = new JsonObject();
                stateJson.add("variants", variants);
                variants.add("", entry.getValue());
                writeJsonSafely(stateJson, statesPath.resolve("custom_" + entry.getKey() + ".json"));
                ImmutableBlockState state = this.plugin.blockManager().getImmutableBlockStateUnsafe(entry.getKey() + vanillaBlockStateCount);
                this.plugin.compatibilityManager().blueMapBlockColors(state, blueMapBlockStates::add);
            }
            if (!blueMapBlockStates.asMap().isEmpty()) {
                writeJsonSafely(blueMapBlockStates, generatedPackPath.resolve("assets").resolve(Key.CRAFTENGINE_NAMESPACE).resolve("blockColors.json"));
            }
        }
    }

    private void generateModernItemModels1_21_2(Path generatedPackPath) {
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_2)) return;
        if (Config.packMinVersion().isAtOrAbove(MinecraftVersion.V1_21_4)) return;

        // 此段代码生成1.21.2专用的item model文件，情况非常复杂！
        for (Map.Entry<Key, TreeSet<LegacyOverridesModel>> entry : this.plugin.itemManager().modernItemModels1_21_2().entrySet()) {
            Key itemModelPath = entry.getKey();
            TreeSet<LegacyOverridesModel> legacyOverridesModels = entry.getValue();

            // 要检查目标生成路径是否已经存在模型，如果存在模型，应该只为其生成overrides
            Path itemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(itemModelPath.namespace())
                    .resolve("models")
                    .resolve("item")
                    .resolve(itemModelPath.value() + ".json");

            boolean modelExists = Files.exists(itemPath);
            JsonObject itemJson;
            if (modelExists) {
                // 路径已经存在了，那么就应该把模型读入
                try {
                    itemJson = GsonHelper.readJsonFromFile(itemPath).getAsJsonObject();
                    // 野心真大，已经自己写了overrides，那么不管你了
                    if (itemJson.has("overrides")) {
                        continue;
                    }
                    JsonArray overrides = new JsonArray();
                    for (LegacyOverridesModel legacyOverridesModel : legacyOverridesModels) {
                        if (legacyOverridesModel.hasPredicate()) {
                            overrides.add(legacyOverridesModel.toLegacyPredicateElement());
                        }
                    }
                    if (!overrides.isEmpty()) {
                        itemJson.add("overrides", overrides);
                    }
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to read item json " + itemPath.toAbsolutePath());
                    continue;
                }
            } else {
                // 如果路径不存在，则需要我们创建一个json对象，并对接model的路径
                itemJson = new JsonObject();

                LegacyOverridesModel firstBaseModel = null;
                List<JsonObject> overrideJsons = new ArrayList<>();
                for (LegacyOverridesModel legacyOverridesModel : legacyOverridesModels) {
                    if (!legacyOverridesModel.hasPredicate()) {
                        if (firstBaseModel == null) {
                            firstBaseModel = legacyOverridesModel;
                        }
                    } else {
                        JsonObject legacyPredicateElement = legacyOverridesModel.toLegacyPredicateElement();
                        overrideJsons.add(legacyPredicateElement);
                    }
                }
                if (firstBaseModel == null) {
                    firstBaseModel = legacyOverridesModels.getFirst();
                }

                itemJson.addProperty(ModelJsonKeys.PARENT, firstBaseModel.model().asMinimalString());
                if (!overrideJsons.isEmpty()) {
                    JsonArray overrides = new JsonArray();
                    for (JsonObject override : overrideJsons) {
                        overrides.add(override);
                    }
                    itemJson.add("overrides", overrides);
                }
            }

            writeJsonSafely(itemJson, itemPath);
        }
    }

    private void generateModernItemModels1_21_4(Path generatedPackPath, Consumer<Revision> callback) {
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_4)) return;
        for (Map.Entry<Key, ModernItemModel> entry : this.plugin.itemManager().modernItemModels1_21_4().entrySet()) {
            Key key = entry.getKey();
            Path itemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("items")
                    .resolve(key.value() + ".json");
            if (Files.exists(itemPath)) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.item_model.conflict", key.asString(), itemPath.toAbsolutePath().toString()));
                continue;
            }

            ModernItemModel modernItemModel = entry.getValue();
            writeJsonSafely(modernItemModel.toJson(Config.packMinVersion()), itemPath);

            List<Revision> revisions = modernItemModel.revisions();
            if (!revisions.isEmpty()) {
                for (Revision revision : revisions) {
                    if (revision.matches(Config.packMinVersion(), Config.packMaxVersion())) {
                        Path overlayItemPath = generatedPackPath
                                .resolve(Config.createOverlayFolderName(revision.versionString()))
                                .resolve("assets")
                                .resolve(key.namespace())
                                .resolve("items")
                                .resolve(key.value() + ".json");
                        writeJsonSafely(modernItemModel.toJson(revision.minVersion(), revision.maxVersion()), overlayItemPath);
                        callback.accept(revision);
                    }
                }
            }
        }
    }

    private void generateModernItemOverrides(Path generatedPackPath, Consumer<Revision> callback) {
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_4)) return;
        for (Map.Entry<Key, TreeMap<Integer, ModernItemModel>> entry : this.plugin.itemManager().modernItemOverrides().entrySet()) {
            Key vanillaItemModel = entry.getKey();
            Path overridedItemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(vanillaItemModel.namespace())
                    .resolve("items")
                    .resolve(vanillaItemModel.value() + ".json");

            ModernItemModel originalItemModel;
            if (Files.exists(overridedItemPath)) {
                try {
                    originalItemModel = ModernItemModel.fromJson(GsonHelper.readJsonFromFile(overridedItemPath).getAsJsonObject());
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to load existing item model (modern)", e);
                    continue;
                }
            } else {
                originalItemModel = PRESET_ITEMS.get(vanillaItemModel);
                if (originalItemModel == null) {
                    this.plugin.logger().warn("Failed to load existing item model for " + vanillaItemModel + " (modern)");
                    continue;
                }
            }

            boolean handAnimationOnSwap = originalItemModel.handAnimationOnSwap();
            boolean oversizedInGui = originalItemModel.oversizedInGui();
            float swapAnimationScale = originalItemModel.swapAnimationScale();

            Map<Float, ItemModel> entries = new TreeMap<>();
            for (Map.Entry<Integer, ModernItemModel> modelWithDataEntry : entry.getValue().entrySet()) {
                ModernItemModel modernItemModel = modelWithDataEntry.getValue();
                entries.put(modelWithDataEntry.getKey().floatValue(), modernItemModel.model());
                if (modernItemModel.handAnimationOnSwap()) {
                    handAnimationOnSwap = true;
                }
                if (modernItemModel.oversizedInGui()) {
                    oversizedInGui = true;
                }
            }

            RangeDispatchItemModel rangeDispatch = new RangeDispatchItemModel(
                new CustomModelDataRangeDispatchProperty(0), 1f, entries, originalItemModel.model()
            );

            ModernItemModel newItemModel = new ModernItemModel(rangeDispatch, handAnimationOnSwap, oversizedInGui, swapAnimationScale);
            writeJsonSafely(newItemModel.toJson(Config.packMinVersion()), overridedItemPath);

            List<Revision> revisions = newItemModel.revisions();
            if (!revisions.isEmpty()) {
                for (Revision revision : revisions) {
                    if (revision.matches(Config.packMinVersion(), Config.packMaxVersion())) {
                        Path overlayItemPath = generatedPackPath
                                .resolve(Config.createOverlayFolderName(revision.versionString()))
                                .resolve("assets")
                                .resolve(vanillaItemModel.namespace())
                                .resolve("items")
                                .resolve(vanillaItemModel.value() + ".json");
                        writeJsonSafely(newItemModel.toJson(revision.minVersion(), revision.maxVersion()), overlayItemPath);
                        callback.accept(revision);
                    }
                }
            }
        }
    }

    private void generateLegacyItemOverrides(Path generatedPackPath) {
        if (Config.packMinVersion().isAtOrAbove(MinecraftVersion.V1_21_4) && !Config.alwaysGenerateModelOverrides()) return;
        for (Map.Entry<Key, TreeSet<LegacyOverridesModel>> entry : this.plugin.itemManager().legacyItemOverrides().entrySet()) {
            Key vanillaLegacyModel = entry.getKey();
            Path overridedItemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(vanillaLegacyModel.namespace())
                    .resolve("models")
                    .resolve("item")
                    .resolve(vanillaLegacyModel.value() + ".json");

            JsonObject originalItemModel;
            if (Files.exists(overridedItemPath)) {
                try (BufferedReader reader = Files.newBufferedReader(overridedItemPath)) {
                    originalItemModel = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | IllegalStateException e) {
                    this.plugin.logger().warn("Failed to load existing item model (legacy)", e);
                    continue;
                }
            } else {
                originalItemModel = PRESET_LEGACY_MODELS_ITEM.get(vanillaLegacyModel);
                if (originalItemModel == null) {
                    this.plugin.logger().warn("Failed to load item model for " + vanillaLegacyModel + " (legacy)");
                    continue;
                }
                originalItemModel = originalItemModel.deepCopy();
            }
            TreeSet<LegacyOverridesModel> overridesModels = new TreeSet<>(entry.getValue());

            JsonArray newOverrides = new JsonArray();
            if (originalItemModel.has("overrides")) {
                JsonArray overrides = originalItemModel.getAsJsonArray("overrides");
                for (JsonElement override : overrides) {
                    if (override instanceof JsonObject jo) {
                        overridesModels.add(new LegacyOverridesModel(jo));
                    }
                }
            }
            for (LegacyOverridesModel model : overridesModels) {
                newOverrides.add(model.toLegacyPredicateElement());
            }
            originalItemModel.add("overrides", newOverrides);
            writeJsonSafely(originalItemModel, overridedItemPath);
        }
    }

    private void generateFonts(Path generatedPackPath) {
        // generate image font json
        for (Font font : this.plugin.fontManager().fonts()) {
            Key namespacedKey = font.key();
            Path fontPath = generatedPackPath.resolve("assets")
                    .resolve(namespacedKey.namespace())
                    .resolve("font")
                    .resolve(namespacedKey.value() + ".json");

            JsonObject fontJson;
            if (Files.exists(fontPath)) {
                try {
                    String content = Files.readString(fontPath);
                    fontJson = JsonParser.parseString(content).getAsJsonObject();
                } catch (IOException | IllegalStateException e) {
                    fontJson = new JsonObject();
                    this.plugin.logger().warn(fontPath + " is not a valid font json file");
                }
            } else {
                fontJson = new JsonObject();
                try {
                    Files.createDirectories(fontPath.getParent());
                } catch (IOException e) {
                    this.plugin.logger().error("Error creating " + fontPath.toAbsolutePath(), e);
                }
            }

            JsonArray providers;
            if (fontJson.has("providers")) {
                providers = fontJson.getAsJsonArray("providers");
            } else {
                providers = new JsonArray();
                fontJson.add("providers", providers);
            }

            List<BitmapImage> sortedImages = new ArrayList<>(font.bitmapImages());
            sortedImages.sort(Comparator.comparing((BitmapImage image) -> image.id().namespace).thenComparing(image -> image.id().value));
            for (BitmapImage image : sortedImages) {
                providers.add(image.get());
            }
            try {
                Files.writeString(fontPath, CharacterUtils.replaceDoubleBackslashU(fontJson.toString()));
            } catch (IOException e) {
                this.plugin.logger().error("Error writing font to " + fontPath.toAbsolutePath(), e);
            }
        }

        if (Config.resourcePack$overrideUniform()) {
            Path fontPath = generatedPackPath.resolve("assets")
                    .resolve("minecraft")
                    .resolve("font")
                    .resolve("default.json");
            if (Files.exists(fontPath)) {
                Path targetPath = generatedPackPath.resolve("assets")
                        .resolve("minecraft")
                        .resolve("font")
                        .resolve("uniform.json");
                try {
                    Files.copy(fontPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private List<Pair<String, List<Path>>> updateCachedAssets(@NotNull PackCacheData cacheData, @Nullable FileSystem fs) throws IOException {
        Map<String, List<Path>> conflictChecker = new HashMap<>(Math.max(128, this.cachedAssetFiles.size()), 0.5f);
        Map<Path, CachedAssetFile> previousFiles = this.cachedAssetFiles;
        this.cachedAssetFiles = Config.cacheResourceFiles() ? new Object2ObjectOpenHashMap<>(Math.max(128, this.cachedAssetFiles.size()), 0.5f) : new Object2ObjectOpenHashMap<>();

        List<Path> folders = new ArrayList<>();
        folders.addAll(loadedPacks().stream()
                .filter(Pack::enabled)
                .map(Pack::resourcePackFolders)
                .flatMap(Arrays::stream)
                .toList());
        folders.addAll(cacheData.externalFolders());
        for (Path sourceFolder : folders) {
            if (Files.exists(sourceFolder)) {
                Files.walkFileTree(sourceFolder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        processRegularFile(file, attrs, sourceFolder, fs, conflictChecker, previousFiles);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        for (Path zip : cacheData.externalZips()) {
            processZipFile(zip, fs, conflictChecker, previousFiles);
        }

        List<Pair<String, List<Path>>> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entry : conflictChecker.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(Pair.of(entry.getKey(), entry.getValue()));
            }
        }
        return conflicts;
    }

    private void processRegularFile(Path file, BasicFileAttributes attrs, Path sourceFolder, @Nullable FileSystem fs,
                                    Map<String, List<Path>> conflictChecker, Map<Path, CachedAssetFile> previousFiles) throws IOException {
        if (Config.excludeFileExtensions().contains(FileUtils.getExtension(file))) {
            return;
        }
        if (Config.isFileExcluded(sourceFolder, file)) {
            return;
        }
        long lastModified = attrs.lastModifiedTime().toMillis();
        long size = attrs.size();
        byte[] data;
        if (Config.cacheResourceFiles()) {
            CachedAssetFile cachedAsset = previousFiles.get(file);
            if (cachedAsset != null && cachedAsset.lastModified() == lastModified && cachedAsset.size() == size) {
                this.cachedAssetFiles.put(file, cachedAsset);
                data = cachedAsset.data();
            } else {
                data = Files.readAllBytes(file);
                this.cachedAssetFiles.put(file, new CachedAssetFile(data, lastModified, size));
            }
        } else {
            data = Files.readAllBytes(file);
        }
        if (fs == null) return;
        Path relative = sourceFolder.relativize(file);
        updateConflictChecker(fs, conflictChecker, file, file, relative, data);
    }

    private void processZipFile(Path zipFile,
                                @Nullable FileSystem fs,
                                Map<String, List<Path>> conflictChecker,
                                Map<Path, CachedAssetFile> previousFiles) {
        try (FileSystem zipFs = FileSystems.newFileSystem(zipFile)) {
            long zipLastModified = Files.getLastModifiedTime(zipFile).toMillis();
            long zipSize = Files.size(zipFile);
            Path zipRoot = zipFs.getPath("/");
            Files.walkFileTree(zipRoot, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path entry, @NotNull BasicFileAttributes entryAttrs) throws IOException {
                    if (entryAttrs.isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (Config.excludeFileExtensions().contains(FileUtils.getExtension(entry))) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (Config.isFileExcluded(zipRoot, entry)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path entryPathInZip = zipRoot.relativize(entry);
                    Path sourcePath = Path.of(zipFile + "!" + entryPathInZip);
                    byte[] data;
                    if (Config.cacheResourceFiles()) {
                        CachedAssetFile cachedAsset = previousFiles.get(sourcePath);
                        if (cachedAsset != null && cachedAsset.lastModified() == zipLastModified && cachedAsset.size() == zipSize) {
                            cachedAssetFiles.put(sourcePath, cachedAsset);
                            data = cachedAsset.data();
                        } else {
                            data = Files.readAllBytes(entry);
                            cachedAssetFiles.put(sourcePath, new CachedAssetFile(data, zipLastModified, zipSize));
                        }
                    } else {
                        data = Files.readAllBytes(entry);
                    }
                    if (fs != null) {
                        try {
                            updateConflictChecker(fs, conflictChecker, entry, sourcePath, entryPathInZip, data);
                        } catch (Exception e) {
                            AbstractPackManager.this.plugin.logger().warn("Failed to update conflict checker", e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to process zip file " + zipFile, e);
        }
    }

    private void updateConflictChecker(FileSystem fs, Map<String, List<Path>> conflictChecker, Path sourcePath, Path namedSourcePath, Path relative, byte[] data) throws IOException {
        String relativePath = CharacterUtils.replaceBackslashWithSlash(relative.toString());
        Path targetPath = fs.getPath("resource_pack/" + relativePath);
        List<Path> conflicts = conflictChecker.get(relativePath);
        if (conflicts == null) {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            conflictChecker.put(relativePath, List.of(namedSourcePath));
        } else {
            PathContext relativeCTX = PathContext.of(relative);
            PathContext targetCTX = PathContext.of(targetPath);
            PathContext sourceCTX = PathContext.of(sourcePath);
            for (ConditionalResolution resolution : Config.resolutions()) {
                if (resolution.matcher().test(relativeCTX)) {
                    resolution.resolution().run(targetCTX, sourceCTX);
                    return;
                }
            }
            switch (conflicts.size()) {
                case 1 -> conflictChecker.put(relativePath, List.of(conflicts.get(0), namedSourcePath));
                case 2 -> conflictChecker.put(relativePath, List.of(conflicts.get(0), conflicts.get(1), namedSourcePath));
                case 3 -> conflictChecker.put(relativePath, List.of(conflicts.get(0), conflicts.get(1), conflicts.get(2), namedSourcePath));
                case 4 -> conflictChecker.put(relativePath, List.of(conflicts.get(0), conflicts.get(1), conflicts.get(2), conflicts.get(3), namedSourcePath));
                default -> {
                    // just ignore it if it has many conflict files
                }
            }
        }
    }

    private void writeJsonSafely(JsonElement json, Path path) {
        ResourcePackJsonUtils.writeJsonSafely(json, path);
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.skipOptimizationParser, this.bundleParser, this.atlasConfigParser};
    }

    public final class ConfigFactoryParser extends SectionConfigParser {
        private static final String[] SECTION_ID = ConfigKeys.of("config_factor(y|ies)");
        private static final String[] BLUEPRINT = ConfigKeys.of("blueprint|prototype|schema");
        private static final String[] INSTANCES = ConfigKeys.of("instance(s)|input(s)");
        private int count = 0;

        @Override
        public Key type() {
            return Key.ce("config_factory");
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            List<ConfigSection> instances = section.getNonEmptyList(INSTANCES, ConfigValue::getAsSection);
            ConfigSection bundle = section.getNonNullSection(BLUEPRINT);
            for (ConfigSection argument : instances) {
                Map<String, TemplateArgument> argumentsMap = new HashMap<>();
                for (String key : argument.keySet()) {
                    argumentsMap.put(key, TemplateArguments.fromConfig(argument.getValue(key)));
                }
                for (String parserId : bundle.keySet()) {
                    Object value = bundle.get(parserId);
                    if (value == null) continue;
                    processConfigEntry(Map.entry(parserId, value), path, pack, argumentsMap);
                }
            }
            this.count++;
        }

        @Override
        public void preProcess() {
            this.count = 0;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public String[] sectionId() {
            return SECTION_ID;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.CONFIG_FACTORY;
        }
    }

    public static final class SkipOptimizationParser extends SectionConfigParser {
        private static final String[] SECTION_ID = ConfigKeys.of("skip_optimization");
        private final Set<String> excludeTexture = new HashSet<>();
        private final Set<String> excludeJson = new HashSet<>();

        public SkipOptimizationParser() {
        }

        @Override
        public Key type() {
            return Key.ce("skip_optimization");
        }

        public void clearCache() {
            this.excludeTexture.clear();
            this.excludeJson.clear();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.SKIP_OPTIMIZATION;
        }

        @Override
        public int count() {
            return this.excludeJson.size() + this.excludeTexture.size();
        }

        public Set<String> excludeTexture() {
            return this.excludeTexture;
        }

        public Set<String> excludeJson() {
            return this.excludeJson;
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            List<String> textures = section.getStringList("texture");
            if (!textures.isEmpty()) {
                for (String texture : textures) {
                    if (texture.endsWith(".png") || texture.endsWith("/")) {
                        this.excludeTexture.add(texture);
                    } else {
                        this.excludeTexture.add(texture + ".png");
                    }
                }
            }
            List<String> jsons = section.getStringList("json");
            if (!jsons.isEmpty()) {
                for (String json : jsons) {
                    if (json.endsWith(".json") || json.endsWith(".mcmeta") || json.endsWith("/")) {
                        this.excludeJson.add(json);
                    } else {
                        this.excludeJson.add(json + ".json");
                    }
                }
            }
        }

        @Override
        public String[] sectionId() {
            return SECTION_ID;
        }
    }

    public static final class AtlasConfigParser extends IdSectionConfigParser {
        private static final String[] SECTION_ID = ConfigKeys.of("atlas(es)");
        private static final String[] SOURCES = ConfigKeys.of("source(s)");
        private final Map<Key, List<SpriteSource>> atlases = new HashMap<>();

        @Override
        public Key type() {
            return Key.ce("atlas");
        }

        @Override
        public String[] sectionId() {
            return SECTION_ID;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ATLAS;
        }

        // 同一个图集id允许出现在多个配置中，按加载顺序合并
        @Override
        protected boolean checkDuplicated() {
            return false;
        }

        @Override
        public int count() {
            return this.atlases.size();
        }

        @Override
        public boolean supportSearch() {
            return false;
        }

        public Map<Key, List<SpriteSource>> atlases() {
            return this.atlases;
        }

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            List<SpriteSource> sources = section.getList(SOURCES, value -> SpriteSource.fromConfig(value.getAsSection()));
            this.atlases.computeIfAbsent(id, k -> new ArrayList<>()).addAll(sources);
        }
    }
}
