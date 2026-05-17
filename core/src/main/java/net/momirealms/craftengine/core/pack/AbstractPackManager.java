package net.momirealms.craftengine.core.pack;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.EquipmentLayerType;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.item.processor.ObfuscatedItemModelProcessor;
import net.momirealms.craftengine.core.pack.atlas.Atlas;
import net.momirealms.craftengine.core.pack.atlas.SimplifiedModelFile;
import net.momirealms.craftengine.core.pack.atlas.TextureStatus;
import net.momirealms.craftengine.core.pack.atlas.TexturedModel;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.pack.host.impl.NoneHost;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.pack.mcmeta.overlay.OverlayCombination;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CustomModelDataRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.simplified.*;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingPyramid;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.config.yaml.DoubleSensitiveSchema;
import net.momirealms.craftengine.core.plugin.config.yaml.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.ClientLangData;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.sparrow.reflection.SReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
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
    public static final Map<Key, SimplifiedModelReader> SIMPLIFIED_MODEL_READERS = new HashMap<>();

    public static final String NEW_TRIM_MATERIAL = "custom";

    public static final Set<String> ALLOWED_VANILLA_EQUIPMENT = Set.of("chainmail", "diamond", "gold", "iron", "netherite");
    public static final Set<String> ALLOWED_MODEL_TAGS = Set.of("parent", "ambientocclusion", "display", "textures", "elements", "gui_light", "overrides");

    private static final Key MISSING_NO = Key.of("missingno");

    private static final Key BUILTIN_GENERATED = Key.of("builtin/generated");
    private static final Key BUILTIN_ENTITY = Key.of("builtin/entity");

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
    private final Consumer<PackCacheData> cacheEventDispatcher;
    private final BiConsumer<Path, Path> generationEventDispatcher;
    private final Map<String, Pack> loadedPacks = new LinkedHashMap<>();
    private final Map<String, ConfigParser> sectionParsers = new HashMap<>();
    private final List<ConfigParser> parsers = new ArrayList<>();
    public final JsonObject vanillaBlockAtlas;
    public final JsonObject vanillaItemAtlas;
    private Map<Path, CachedConfigFile> cachedConfigFiles = Collections.emptyMap();
    private Map<Path, CachedAssetFile> cachedAssetFiles = Collections.emptyMap();
    protected BiConsumer<Path, Path> zipGenerator;
    protected ResourcePackHost resourcePackHost;
    private final SkipOptimizationParser parser = new SkipOptimizationParser();

    public AbstractPackManager(CraftEngine plugin, Consumer<PackCacheData> cacheEventDispatcher, BiConsumer<Path, Path> generationEventDispatcher) {
        this.plugin = plugin;
        this.cacheEventDispatcher = cacheEventDispatcher;
        this.generationEventDispatcher = generationEventDispatcher;
        this.zipGenerator = (p1, p2) -> {
            try {
                ZipUtils.compress(p1, p2);
            } catch (IOException e) {
                throw new RuntimeException("Failed to compress resource pack.", e);
            }
        };
        Path resourcesFolder = this.plugin.dataFolderPath().resolve("resources");
        try {
            if (Files.notExists(resourcesFolder)) {
                Files.createDirectories(resourcesFolder);
                this.saveDefaultConfigs();
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
                JsonElement parent = jsonObject.get("parent");
                if (parent instanceof JsonPrimitive primitive) {
                    String parentModel = primitive.getAsString();
                    if (parentModel.equals("minecraft:item/handheld")) {
                        SIMPLIFIED_MODEL_READERS.put(item, GeneratedModelReader.HANDHELD);
                        continue;
                    }
                }
            }
            if (DYEABLE_LEATHER_ARMOR.contains(item)) {
                SIMPLIFIED_MODEL_READERS.put(item, GeneratedModelReader.LEATHER);
            } else {
                SIMPLIFIED_MODEL_READERS.put(item, GeneratedModelReader.GENERATED);
            }
        }

        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FISHING_ROD, ConditionModelReader.FISHING_ROD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.ELYTRA, ConditionModelReader.ELYTRA);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.SHIELD, ConditionModelReader.SHIELD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.BOW, BowModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.CROSSBOW, CrossbowModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FIREWORK_STAR, GeneratedModelReader.FIREWORK_STAR);
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
    public Path resourcePackPath() {
        return Config.resourcePackPath();
    }

    @Override
    public void load() {
        Object hostingObj = Config.instance().settings().get("resource-pack.delivery.hosting");
        if (hostingObj == null) {
            this.resourcePackHost = NoneHost.INSTANCE;
            return;
        }
        ConfigValue configValue = ConfigValue.of("resource-pack.delivery.hosting", hostingObj);
        try {
            List<ResourcePackHost> hosts = configValue.getAsList(v -> ResourcePackHosts.fromConfig(v.getAsSection()));
            if (hosts.isEmpty()) {
                this.resourcePackHost = NoneHost.INSTANCE;
            } else {
                this.resourcePackHost = hosts.getFirst();
            }
        } catch (KnownResourceException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
            this.resourcePackHost = NoneHost.INSTANCE;
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to load resource-pack.delivery.hosting", e);
            this.resourcePackHost = NoneHost.INSTANCE;
        }
    }

    @Override
    public ResourcePackHost resourcePackHost() {
        return this.resourcePackHost;
    }

    @Override
    public void uploadResourcePack() {
        Timestamp timestamp = new Timestamp();
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("host.upload_started"));
        resourcePackHost().upload(Config.fileToUpload()).whenComplete((d, e) -> {
            if (e != null) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("host.upload_failed"), e);
                return;
            }
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("host.upload_finished", String.valueOf(timestamp.deltaMillis())));
            if (!Config.sendPackOnUpload()) return;
            for (Player player : this.plugin.networkManager().onlineUsers()) {
                sendResourcePack(player);
            }
        });
    }

    @Override
    public int loadResources(Predicate<ConfigParser> predicate) {
        return this.loadResourceConfigs(predicate);
    }

    @Override
    public void unload() {
        this.parser.clearCache();
        this.loadedPacks.clear();
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
            this.cacheEventDispatcher.accept(cacheData);
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
        this.parsers.add(parser);
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
                this.saveDefaultConfigs();
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

    public void saveDefaultConfigs() throws IOException {
        saveFileByIndexFile("resources");
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

    @SuppressWarnings("unchecked")
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
                            String fileName = path.getFileName().toString();
                            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                                CachedConfigFile cachedFile = previousFiles.get(path);
                                long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                                long size = attrs.size();
                                if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                                    AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                } else {
                                    LoadSettings loadSettings = LoadSettings.builder()
                                            .setSchema(DoubleSensitiveSchema.INSTANCE)
                                            .setLabel(path.toAbsolutePath().toString())
                                            .build();
                                    Map<String, Object> data;
                                    try {
                                        String content = Files.readString(path);
                                        try {
                                            data = (Map<String, Object>) new Load(loadSettings, new StringKeyConstructor(loadSettings, path))
                                                    .loadFromString(content);
                                        } catch (ScannerException e1) {
                                            String msg = e1.getMessage();
                                            if (msg != null && msg.contains("TAB") && msg.contains("indentation")) {
                                                try {
                                                    content = content.replace("\t", "    ");
                                                    data = (Map<String, Object>) new Load(loadSettings, new StringKeyConstructor(loadSettings, path))
                                                            .loadFromString(content);
                                                    Files.writeString(path, content);
                                                } catch (ScannerException e2) {
                                                    AbstractPackManager.this.plugin.logger().error("Error found while reading config file (after TAB fix): " + path, e2);
                                                    return FileVisitResult.CONTINUE;
                                                }
                                            } else {
                                                AbstractPackManager.this.plugin.logger().error("Error found while reading config file: " + path, e1);
                                                return FileVisitResult.CONTINUE;
                                            }
                                        }
                                    } catch (IOException e) {
                                        AbstractPackManager.this.plugin.logger().error("Error while reading config file: " + path, e);
                                        return FileVisitResult.CONTINUE;
                                    } catch (ParserException e) {
                                        AbstractPackManager.this.plugin.logger().error("Invalid YAML file found: " + path + ".\n" + e.getMessage() +
                                                "\nIt is recommended to use Visual Studio Code as your YAML editor to fix problems more quickly.");
                                        return FileVisitResult.CONTINUE;
                                    }

                                    if (data == null) {
                                        return FileVisitResult.CONTINUE;
                                    }
                                    cachedFile = new CachedConfigFile(data, pack, lastModifiedTime, size);
                                    AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                }
                                for (Map.Entry<String, Object> entry : cachedFile.config().entrySet()) {
                                    processConfigEntry(entry, path, cachedFile.pack(), ConfigParser::addConfig);
                                }
                            } else if (fileName.endsWith(".json")) {
                                CachedConfigFile cachedFile = previousFiles.get(path);
                                long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                                long size = attrs.size();
                                if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                                    AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                } else {
                                    try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                                        Map<String, Object> data = GsonHelper.parseJsonToMap(inputStream);
                                        if (data == null)  return FileVisitResult.CONTINUE;
                                        cachedFile = new CachedConfigFile(data, pack, lastModifiedTime, size);
                                        AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                    } catch (IOException e) {
                                        AbstractPackManager.this.plugin.logger().error("Error while reading config file: " + path, e);
                                        return FileVisitResult.CONTINUE;
                                    } catch (JsonParseException e) {
                                        AbstractPackManager.this.plugin.logger().error("Invalid JSON file found: " + path + ".\n" + e.getMessage() + "\nIt is recommended to use Visual Studio Code as your JSON editor to fix problems more quickly.");
                                        return FileVisitResult.CONTINUE;
                                    }
                                }
                                for (Map.Entry<String, Object> entry : cachedFile.config().entrySet()) {
                                    processConfigEntry(entry, path, cachedFile.pack(), ConfigParser::addConfig);
                                }
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

    private int loadResourceConfigs(Predicate<ConfigParser> predicate) {
        LoadingPyramid pyramid = new LoadingPyramid();
        Map<Path, List<ResourceException>> errorByPath = new ConcurrentHashMap<>();
        for (ConfigParser parser : this.parsers) {
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
                parser.preProcess();
                parser.loadAll();
                parser.postProcess();
                long t2 = System.nanoTime();
                int count = parser.count();
                if (parser.silentIfNotExists() && count == 0) {
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
        for (ConfigParser parser : this.parsers) {
            parser.clearConfigs();
        }
    }

    private void processConfigEntry(Map.Entry<String, Object> entry, Path path, Pack pack, BiConsumer<ConfigParser, CachedConfigSection> callback) {
        if (entry.getValue() instanceof Map<?,?> m) {
            String key = entry.getKey();
            int hashIndex = key.indexOf('#');
            String configType = hashIndex != -1 ? key.substring(0, hashIndex) : key;
            ConfigParser parser = this.sectionParsers.get(configType);
            if (parser != null) {
                callback.accept(parser, new CachedConfigSection(pack, path, ConfigSection.of(key, MiscUtils.castToMap(m))));
            }
        }
    }

    @Override
    public void generateResourcePack() {
        this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.generation_started"));
        Timestamp timestamp = new Timestamp();
        ObfuscatedItemModelProcessor.resetMappings();

        // Create cache data
        PackCacheData cacheData = new PackCacheData(this.plugin);
        this.cacheEventDispatcher.accept(cacheData);

        // get the target location
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
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
            this.generateEquipments(generatedPackPath, revisions::add);
            this.generateParticle(generatedPackPath);

            // 有地图兼容的情况下，先生成一半
            boolean mapCompatibility = Config.enableMapPluginCompatibility();
            if (mapCompatibility) {
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

            // 排除shaders
            if (Config.excludeShaders()) {
                this.removeAllShaders(generatedPackPath);
            }

            // 如果开启地图兼容，先校验一遍 mcmeta
            if (mapCompatibility) {
                this.validatePackMetadata(packMcMeta, overlays);
                this.writeJsonSafely(packMcMeta, generatedPackPath.resolve("pack.mcmeta"));
                try {
                    ZipUtils.compress(generatedPackPath, Config.mapPluginCompatibilityPath());
                } catch (IOException e) {
                    this.plugin.logger().error("Error creating map plugin resource pack", e);
                }
                this.deleteMapCompatibilityAssets(generatedPackPath);
                // 再生成覆写原版的overrides
                this.generateBlockOverrides(generatedPackPath, true, false);
                if (!Config.generateModAssets()) {
                    this.deleteModAssets(generatedPackPath);
                }
            }

            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.generation_finished", String.valueOf(timestamp.deltaMillis())));

            // 校验资源包
            if (Config.validateResourcePack()) {
                this.validateResourcePack(generatedPackPath, overlays);
                this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.validation_finished", String.valueOf(timestamp.deltaMillis())));
            }

            // 验证完成后，应该重新校验pack.mcmeta并写入
            this.validatePackMetadata(packMcMeta, overlays);
            this.writeJsonSafely(packMcMeta, generatedPackPath.resolve("pack.mcmeta"));

            // 优化资源包
            if (Config.optimizeResourcePack()) {
                this.optimizeResourcePack(generatedPackPath);
                this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.optimization_finished", String.valueOf(timestamp.deltaMillis())));
            }

            if (Config.enablePackSquash()) {

            }

            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.compression_started"));
            Path finalPath = resourcePackPath();
            Files.createDirectories(finalPath.getParent());

            // 生成无保护资源包
            if (VersionHelper.PREMIUM && Config.createUnprotectedCopy()) {
                try {
                    ZipUtils.compress(generatedPackPath, Config.unprotectedCopyPath());
                } catch (IOException e) {
                    this.plugin.logger().error("Error creating unprotected resource pack", e);
                }
            }
            // 生成资源包
            try {
                this.zipGenerator.accept(generatedPackPath, finalPath);
            } catch (Exception e) {
                this.plugin.logger().error("Error creating resource pack", e);
            }
            this.plugin.logger().info(TranslationManager.instance().plainTranslation("resource_pack.compression_finished", String.valueOf(timestamp.deltaMillis())));
            this.generationEventDispatcher.accept(generatedPackPath, finalPath);
            if (Config.autoUpload() && resourcePackHost().canUpload()) {
                uploadResourcePack();
            }
        } catch (IOException e) {
            this.plugin.logger().error("Error generating resource pack", e);
        }
    }

    private void validatePackMetadata(JsonObject rawMeta, Overlays packOverlays) {
        // 获取设定的最大和最小值
        PackVersion minVersion = Config.packMinVersion().packFormat();
        PackVersion maxVersion = Config.packMaxVersion().packFormat();

        // 设置pack
        {
            JsonObject packJson = new JsonObject();
            rawMeta.add("pack", packJson);
            JsonElement description = AdventureHelper.componentToJsonElement(AdventureHelper.miniMessage().deserialize(AdventureHelper.legacyToMiniMessage(Config.packDescription())));
            packJson.add("description", description);
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
        excludeTexture.addAll(this.parser.excludeTexture());
        excludeJson.addAll(this.parser.excludeJson());
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
                if (jsonExcluder.test(metaPath)) {
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
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int amount = commonJsonToOptimize.size() + modelJsonToOptimize.size();
            AtomicInteger finished = new AtomicInteger(0);
            for (Path jsonPath : commonJsonToOptimize) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        byte[] before = Files.readAllBytes(jsonPath);
                        previousBytes.getAndAdd(before.length);
                        byte[] after = GsonHelper.toString(GsonHelper.parseJson(new String(before, StandardCharsets.UTF_8))).replace("\"minecraft:", "\"").getBytes(StandardCharsets.UTF_8);
                        if (after.length < before.length) {
                            afterBytes.addAndGet(after.length);
                            Files.write(jsonPath, after);
                        } else {
                            afterBytes.addAndGet(before.length);
                        }
                        finished.incrementAndGet();
                    } catch (IOException | JsonParseException ignored) {
                    }
                }, this.plugin.scheduler().async()));
            }
            for (Path jsonPath : modelJsonToOptimize) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        byte[] before = Files.readAllBytes(jsonPath);
                        previousBytes.getAndAdd(before.length);
                        JsonObject json = GsonHelper.parseJson(new String(before, StandardCharsets.UTF_8)).getAsJsonObject();
                        List<String> invalidKey = json.keySet().stream().filter(k -> !ALLOWED_MODEL_TAGS.contains(k)).toList();
                        if (!invalidKey.isEmpty()) {
                            for (String key : invalidKey) {
                                json.remove(key);
                            }
                        }
                        byte[] after = GsonHelper.toString(json).replace("\"minecraft:", "\"").getBytes(StandardCharsets.UTF_8);
                        if (after.length < before.length) {
                            afterBytes.addAndGet(after.length);
                            Files.write(jsonPath, after);
                        } else {
                            afterBytes.addAndGet(before.length);
                        }
                        finished.incrementAndGet();
                    } catch (IOException | JsonParseException | IllegalStateException ignored) {
                    }
                }, this.plugin.scheduler().async()));
            }

            CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);
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
            AtomicLong previousBytes = new AtomicLong(0L);
            AtomicLong afterBytes = new AtomicLong(0L);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int amount = imagesToOptimize.size();
            AtomicInteger finished = new AtomicInteger(0);
            for (Path imagePath : imagesToOptimize) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        byte[] previousImageBytes = Files.readAllBytes(imagePath);
                        byte[] optimized = optimizeImage(imagePath, previousImageBytes);
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
                }, this.plugin.scheduler().async()));
            }
            CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);
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

    private byte[] optimizeImage(Path imagePath, byte[] previousImageBytes) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(previousImageBytes)) {
            BufferedImage src = ImageIO.read(is);
            if (src == null) {
                Debugger.RESOURCE_PACK.debug(() -> "Cannot read image " + imagePath.toString());
                return previousImageBytes;
            }
            if (src.getType() == BufferedImage.TYPE_CUSTOM) {
                return previousImageBytes;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new PngOptimizer(src).write(baos);
            return baos.toByteArray();
        }
    }

    private void validateResourcePack(Path path, Overlays packOverlays) {
        List<OverlayCombination.Segment> segments = new ArrayList<>();
        // 完全小于1.21.11或完全大于1.21.11
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_11) || Config.packMinVersion().isAtOrAbove(MinecraftVersion.V1_21_11)) {
            OverlayCombination combination = new OverlayCombination(packOverlays.overlays(), Config.packMinVersion().majorPackFormat(), Config.packMaxVersion().majorPackFormat());
            while (combination.hasNext()) {
                OverlayCombination.Segment segment = combination.nextSegment();
                if (segment != null) {
                    segments.add(segment);
                } else {
                    break;
                }
            }
        }
        // 混合版本
        else {
            OverlayCombination combinationLegacy = new OverlayCombination(packOverlays.overlays(), Config.packMinVersion().majorPackFormat(), 72 /* 25w44a */);
            while (combinationLegacy.hasNext()) {
                OverlayCombination.Segment segment = combinationLegacy.nextSegment();
                if (segment != null) {
                    segments.add(segment);
                } else {
                    break;
                }
            }
            OverlayCombination combinationModern = new OverlayCombination(packOverlays.overlays(), 73 /* 25w45a */, Config.packMaxVersion().majorPackFormat());
            while (combinationModern.hasNext()) {
                OverlayCombination.Segment segment = combinationModern.nextSegment();
                if (segment != null) {
                    segments.add(segment);
                } else {
                    break;
                }
            }
        }

        AtlasFixer itemFixer = new AtlasFixer();
        AtlasFixer blockFixer = new AtlasFixer();

        boolean hasNonOverlaySupport = false;
        if (!segments.isEmpty()) {
            // 第一个segment一定是最小的
            hasNonOverlaySupport = segments.getFirst().min() <= MinecraftVersion.V1_20_1.packFormat().major();
        }

        boolean fixAtlasOnValidation = Config.fixTextureAtlas() && !Config.enableObfuscation();
        Set<Revision> revisions = new TreeSet<>();
        for (int i = 0, size = segments.size(); i < size; i++) {
            OverlayCombination.Segment segment = segments.get(i);
            List<Path> rootPathList = new ArrayList<>();
            rootPathList.add(path);
            List<Overlay> overlayInOrder = new ArrayList<>(segment.overlays().size());
            for (Overlay overlay : packOverlays.overlays()) {
                if (segment.overlays().contains(overlay)) {
                    Path overlayDir = path.resolve(overlay.directory());
                    if (Files.isDirectory(overlayDir)) {
                        overlayInOrder.add(overlay);
                        rootPathList.add(overlayDir);
                    }
                }
            }

            this.plugin.logger().info(TranslationManager.instance().plainTranslation(
                    "resource_pack.validation_started",
                    String.valueOf(i + 1), String.valueOf(size), String.valueOf(segment.min()), String.valueOf(segment.max()), overlayInOrder.stream().map(Overlay::directory).toList().toString()
            ));

            Set<Path> fixedModels = new HashSet<>();
            ValidationResult result = validateResourcePackWithOverlays(
                    path,
                    rootPathList.toArray(new Path[0]),
                    fixedModels,
                    revisions,
                    segment.min() >= MinecraftVersion.V1_21_6.packFormat().major(),
                    segment.max() >= MinecraftVersion.V1_21_11.packFormat().major(),
                    segment.min() >= MinecraftVersion.V26_1.packFormat().major()
            );
            if (fixAtlasOnValidation) {
                // 有修复物品
                if (result.fixedItemAtlas != null) {
                    itemFixer.addEntry(segment.min(), segment.max(), result.fixedItemAtlas);
                }
                // 有修复方块
                if (result.fixedBlockAtlas != null) {
                    blockFixer.addEntry(segment.min(), segment.max(), result.fixedBlockAtlas);
                } else if (hasNonOverlaySupport) {
                    // 如果有低版本的支持，那么要通过overlay复原atlas
                    blockFixer.addEntry(segment.min(), segment.max(), Objects.requireNonNullElseGet(result.originalBlockAtlas, JsonObject::new));
                }
            }
        }

        // 加入新的 overlay
        for (Revision revision : revisions) {
            packOverlays.addOverlay(revision.createOverlay());
        }

        // 尝试修复atlas
        if (fixAtlasOnValidation) {
            // 物品
            for (AtlasFixer.Entry entry : itemFixer.entries()) {
                int min = entry.min();
                int max = entry.max();
                String directoryName = Config.createOverlayFolderName(min + "-" + max);
                Path atlasPath = path.resolve(directoryName)
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("atlases")
                        .resolve("items.json");
                writeJsonSafely(entry.atlas(), atlasPath);
                packOverlays.addOverlay(new Overlay(new PackVersion(min), new PackVersion(max), directoryName));
            }
            // 方块
            for (AtlasFixer.Entry entry : blockFixer.entries()) {
                int min = entry.min();
                int max = entry.max();
                String directoryName = Config.createOverlayFolderName(min + "-" + max);
                // 这个版本不认可overlay，得把atlas直接写进主包内
                if (min <= MinecraftVersion.V1_20_1.packFormat().major()) {
                    Path atlasPath = path.resolve("assets")
                            .resolve("minecraft")
                            .resolve("atlases")
                            .resolve("blocks.json");
                    writeJsonSafely(entry.atlas(), atlasPath);
                } else {
                    Path atlasPath = path.resolve(directoryName)
                            .resolve("assets")
                            .resolve("minecraft")
                            .resolve("atlases")
                            .resolve("blocks.json");
                    writeJsonSafely(entry.atlas(), atlasPath);
                    packOverlays.addOverlay(new Overlay(new PackVersion(min), new PackVersion(max), directoryName));
                }
            }
        }
    }

    private JsonObject readJsonObjectFromFileOrWarn(Path path) {
        try {
            JsonObject jsonObject = GsonHelper.readJsonObjectFromFile(path);
            if (jsonObject == null) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.not_a_json_object", path.toString()));
            }
            return jsonObject;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.malformatted_json", path.toString()));
        }
        return null;
    }

    @SuppressWarnings("DuplicatedCode")
    private ValidationResult validateResourcePackWithOverlays(
            Path resourcePackPath,
            Path[] rootPaths,
            Set<Path> checkedModels,
            Set<Revision> revisions,
            boolean v1_21_6, // no 22.5 angle limit
            boolean v1_21_11, // item atlas + no -45~45 angle limit
            boolean v26_1  // texture format update
    ) {
        Multimap<Key, Key> glyphToFonts = HashMultimap.create(128, 32); // 图片到字体的映射
        Multimap<Key, Key> modelToItemDefinitions = HashMultimap.create(128, 4); // 模型到物品的映射
        Multimap<Key, String> modelToBlockStates = HashMultimap.create(128, 32); // 模型到方块的映射
        Multimap<Key, Key> textureToModels = HashMultimap.create(128, 8); // 纹理到模型的映射
        Multimap<Key, Key> textureToEquipments = HashMultimap.create(128, 8); // 纹理到盔甲的映射
        Multimap<Key, Key> oggToSoundEvents = HashMultimap.create(128, 4); // 音频到声音的映射

        ResourcePackView rpView = new ResourcePackView(rootPaths);

        // 如果需要验证资源包，则需要先读取所有atlas
        JsonObject lastBlocksAtlas = rpView.getExistingReversed("assets/minecraft/atlases/blocks.json", this::readJsonObjectFromFileOrWarn);
        JsonObject lastItemAtlas = v1_21_11 ? rpView.getExistingReversed("assets/minecraft/atlases/items.json", this::readJsonObjectFromFileOrWarn) : null;


        /*


        构建Atlas文件，
        验证只使用默认的atlas，否则整个过程将会变成非常复杂


         */
        Atlas blockAtlas;
        Atlas itemAtlas;
        if (v1_21_11) {
            blockAtlas = new Atlas(ListUtils.newNonNullList(lastBlocksAtlas, this.vanillaBlockAtlas));
            itemAtlas = new Atlas(ListUtils.newNonNullList(lastItemAtlas, this.vanillaItemAtlas));
        } else {
            blockAtlas = new Atlas(ListUtils.newNonNullList(lastBlocksAtlas, this.vanillaBlockAtlas, this.vanillaItemAtlas));
            itemAtlas = null;
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
                continue;
            }

            for (Path namespacePath : namespaces) {
                String namespace = namespacePath.getFileName().toString(); // 命名空间

                // 字体文件夹
                Path fontPath = namespacePath.resolve("font");
                if (Files.isDirectory(fontPath)) {
                    try {
                        Files.walkFileTree(fontPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject fontJson = readJsonObjectFromFileOrWarn(file);
                                if (fontJson == null) return FileVisitResult.CONTINUE;
                                JsonArray providers = fontJson.getAsJsonArray("providers");
                                if (providers != null) {
                                    Key fontName = Key.of(namespace, FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                    for (JsonElement provider : providers) {
                                        if (!(provider instanceof JsonObject providerJO)) continue;
                                        JsonPrimitive typePrimitive = providerJO.getAsJsonPrimitive("type");
                                        if (typePrimitive == null) continue;
                                        String type = typePrimitive.getAsString();
                                        if (!type.equals("bitmap")) continue;
                                        JsonPrimitive filePrimitive = providerJO.getAsJsonPrimitive("file");
                                        if (filePrimitive == null) continue;
                                        String pngFile = filePrimitive.getAsString();
                                        Key identifier = Key.of(FileUtils.pathWithoutExtension(pngFile));
                                        glyphToFonts.put(identifier, fontName);
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to walk through font", e);
                    }
                }

                // 1.21.4+的物品模型
                Path itemsPath = namespacePath.resolve("items");
                if (Files.isDirectory(itemsPath)) {
                    try {
                        Files.walkFileTree(itemsPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject itemJson = readJsonObjectFromFileOrWarn(file);
                                if (itemJson == null) return FileVisitResult.CONTINUE;
                                Key item = Key.of(namespace, FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                collectItemModelsDeeply(itemJson, (identifier) -> modelToItemDefinitions.put(identifier, item));
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to walk through items", e);
                    }
                }

                // 方块状态json
                Path blockStatesPath = namespacePath.resolve("blockstates");
                if (Files.isDirectory(blockStatesPath)) {
                    try {
                        Files.walkFileTree(blockStatesPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject blockStateJson = readJsonObjectFromFileOrWarn(file);
                                if (blockStateJson == null) return FileVisitResult.CONTINUE;
                                String blockId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                                if (blockStateJson.has("multipart")) {
                                    collectMultipart(blockStateJson.getAsJsonArray("multipart"), (location) -> modelToBlockStates.put(location, blockId));
                                } else if (blockStateJson.has("variants")) {
                                    collectVariants(blockId, blockStateJson.getAsJsonObject("variants"), modelToBlockStates::put);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to walk through blockstates", e);
                    }
                }

                // 装备
                Path equipmentPath = namespacePath.resolve("equipment");
                if (Files.isDirectory(equipmentPath)) {
                    try {
                        Files.walkFileTree(equipmentPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject equipmentJson = readJsonObjectFromFileOrWarn(file);
                                if (equipmentJson == null) return FileVisitResult.CONTINUE;
                                String equipmentId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                                if (equipmentJson.get("layers") instanceof JsonObject layersObject) {
                                    for (Map.Entry<String, JsonElement> layerEntry : layersObject.entrySet()) {
                                        String type = layerEntry.getKey();
                                        if (layerEntry.getValue() instanceof JsonArray equipmentLayers) {
                                            for (JsonElement equipmentLayer : equipmentLayers) {
                                                if (equipmentLayer instanceof JsonObject layer && layer.get("texture") instanceof JsonPrimitive layerTexture) {
                                                    Key rawTexture = Key.of(layerTexture.getAsString());
                                                    Key fullPath = Key.of(rawTexture.namespace(), "entity/equipment/" + type + "/" + rawTexture.value());
                                                    textureToEquipments.put(fullPath, Key.of(namespace, equipmentId));
                                                }
                                            }
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException | ClassCastException e) {
                        this.plugin.logger().warn("Failed to walk through equipments", e);
                    }
                }

                // 声音文件
                Path soundsPath = namespacePath.resolve("sounds.json");
                if (Files.exists(soundsPath)) {
                    JsonObject soundsJson = readJsonObjectFromFileOrWarn(soundsPath);
                    if (soundsJson != null) {
                        for (Map.Entry<String, JsonElement> soundEventEntry : soundsJson.entrySet()) {
                            Key soundKey = Key.of(namespace, soundEventEntry.getKey());
                            if (soundEventEntry.getValue() instanceof JsonObject soundEventObj) {
                                JsonArray soundArray = soundEventObj.getAsJsonArray("sounds");
                                if (soundArray != null) {
                                    for (JsonElement sound : soundArray) {
                                        if (sound instanceof JsonPrimitive primitive) {
                                            oggToSoundEvents.put(Key.of(primitive.getAsString()), soundKey);
                                        } else if (sound instanceof JsonObject soundObj && soundObj.get("name") instanceof JsonPrimitive name) {
                                            if (soundObj.get("type") instanceof JsonPrimitive type && type.getAsString().equals("file")) {
                                                oggToSoundEvents.put(Key.of(name.getAsString()), soundKey);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 验证font的贴图是否存在
        for (Map.Entry<Key, Collection<Key>> entry : glyphToFonts.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_TEXTURES.contains(key)) continue;
            String imagePath = "assets/" + key.namespace + "/textures/" + key.value + ".png";
            if (rpView.exists(imagePath)) continue;
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_font_texture", entry.getValue().stream().distinct().toList().toString(), imagePath));
        }

        // 验证equipment的贴图是否存在
        for (Map.Entry<Key, Collection<Key>> entry : textureToEquipments.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_TEXTURES.contains(key)) continue;
            String imagePath = "assets/" + key.namespace() + "/textures/" + key.value() + ".png";
            if (rpView.exists(imagePath)) continue;
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_equipment_texture", entry.getValue().stream().distinct().toList().toString(), imagePath));
        }

        // 验证sounds的ogg文件是否存在
        for (Map.Entry<Key, Collection<Key>> entry : oggToSoundEvents.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_SOUNDS.contains(key)) continue;
            String oggPath = "assets/" + key.namespace() + "/sounds/" + key.value() + ".ogg";
            if (rpView.exists(oggPath)) continue;
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_sound", entry.getValue().stream().distinct().toList().toString(), oggPath));
        }

        // 获取所有能直接从资源包里捕获到的方块和物品模型
        Map<Key, TexturedModel> blockModels = new HashMap<>(256);
        Map<Key, TexturedModel> itemModels = new HashMap<>(256);
        // 获取全部minecraft模型
        Map<Key, TexturedModel> modelsCache = new HashMap<>(256);

        boolean fixMissingTexture = Config.fixMissingTexture();
        boolean fixTexturesFormat = Config.fixTexturesFormat() && !v26_1;
        boolean fixRotationAngle = Config.fixRotationAngle() && !v1_21_11;
        boolean fixElement = fixMissingTexture || fixRotationAngle;
        ModelFixFlags flags = new ModelFixFlags(fixMissingTexture, fixTexturesFormat, fixRotationAngle, fixElement, v1_21_6);

        // 收集全部方块状态的模型贴图
        for (Map.Entry<Key, Collection<String>> entry : modelToBlockStates.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (blockModels.containsKey(modelPath)) {
                continue;
            }

            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            Path modelJsonPath = rpView.getExistingReversed(modelStringPath);
            if (modelJsonPath != null) {
                JsonObject modelJson = readJsonObjectFromFileOrWarn(modelJsonPath);
                if (modelJson == null) continue;
                TexturedModel texturedModel = getTexturedModel(modelPath, TexturedModel.getParent(modelJson), TexturedModel.getTextures(modelJson), rootPaths, modelsCache);
                blockModels.put(modelPath, texturedModel);
                if (checkedModels.add(modelJsonPath)) {
                    validateModel(resourcePackPath, modelJson, modelJsonPath, modelStringPath, texturedModel, revisions, flags);
                }
                continue;
            }
            // 提示方块状态缺少模型
            if (!VANILLA_MODELS.contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_block_model", entry.getValue().toString(), modelStringPath));
            }
        }

        // 收集全部物品定义的模型贴图
        for (Map.Entry<Key, Collection<Key>> entry : modelToItemDefinitions.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (itemModels.containsKey(modelPath)) {
                continue;
            }
            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            Path modelJsonPath = rpView.getExistingReversed(modelStringPath);
            if (modelJsonPath != null) {
                JsonObject modelJson = readJsonObjectFromFileOrWarn(modelJsonPath);
                if (modelJson == null) continue;
                TexturedModel texturedModel = getTexturedModel(modelPath, TexturedModel.getParent(modelJson), TexturedModel.getTextures(modelJson), rootPaths, modelsCache);
                itemModels.put(modelPath, texturedModel);
                if (checkedModels.add(modelJsonPath)) {
                    validateModel(resourcePackPath, modelJson, modelJsonPath, modelStringPath, texturedModel, revisions, flags);
                }
                continue;
            }
            if (!VANILLA_MODELS.contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation("resource_pack.missing_item_model", entry.getValue().toString(), modelStringPath));
            }
        }

        ValidationResult result = null;

        if (!Config.enableObfuscation()) {

            if (v1_21_11) {

                // 并查集联通贴图
                Map<Key, Key> parent = new HashMap<>(512);
                Map<Key, TextureStatus> textureStatuses = new HashMap<>(512);
                Set<Key> notInAtlasTextures = new HashSet<>();

                // 对全部方块状态的模型的贴图进行归类
                for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    Key[] paths = textures.values().toArray(new Key[0]);
                    for (Key spritePath : paths) {
                        parent.putIfAbsent(spritePath, spritePath);
                        textureStatuses.compute(spritePath, (path, status) -> {
                            if (status == null) {
                                status = new TextureStatus();
                                if (itemAtlas.isDefined(spritePath)) {
                                    status.setInItemAtlas(true);
                                }
                                if (blockAtlas.isDefined(spritePath)) {
                                    status.setInBlockAtlas(true);
                                }
                            }
                            status.setShouldBeInBlockAtlas(true);
                            if (!Config.fixTextureAtlas()) {
                                // 不修复就骂你
                                if (status.inItemAtlas()) {
                                    if (status.inBlockAtlas()) {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png")
                                        );
                                    } else {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.bad_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/blocks.png", "minecraft:textures/atlas/items.png"
                                        ));
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.texture_not_in_atlas",
                                                spritePath.asString()
                                        ));
                                    }
                                }
                            }
                            return status;
                        });
                    }
                    for (int i = 1; i < paths.length; i++) {
                        union(parent, paths[0], paths[i]);
                    }
                }

                // 对全部物品的模型的贴图进行归类
                for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    Key[] paths = textures.values().toArray(new Key[0]);

                    boolean hasBlockAtlas = false;
                    for (Key spritePath : paths) {
                        TextureStatus status = textureStatuses.computeIfAbsent(spritePath, (path) -> {
                            TextureStatus newStatus = new TextureStatus();
                            if (itemAtlas.isDefined(spritePath)) {
                                newStatus.setInItemAtlas(true);
                            }
                            if (blockAtlas.isDefined(spritePath)) {
                                newStatus.setInBlockAtlas(true);
                            }
                            return newStatus;
                        });
                        if (status.inBlockAtlas()) {
                            hasBlockAtlas = true;
                            break;
                        }
                    }

                    boolean finalHasBlockAtlas = hasBlockAtlas;
                    for (Key spritePath : paths) {
                        parent.putIfAbsent(spritePath, spritePath);
                        textureStatuses.compute(spritePath, (path, status) -> {
                            if (status == null) {
                                status = new TextureStatus();
                                if (itemAtlas.isDefined(spritePath)) {
                                    status.setInItemAtlas(true);
                                }
                                if (blockAtlas.isDefined(spritePath)) {
                                    status.setInBlockAtlas(true);
                                }
                            }
                            if (finalHasBlockAtlas && !status.shouldBeInBlockAtlas()) {
                                status.setShouldBeInBlockAtlas(true);
                            }
                            if (!Config.fixTextureAtlas()) {
                                // 不修复就骂你
                                if (status.inItemAtlas()) {
                                    if (status.inBlockAtlas()) {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png"
                                        ));
                                    } else if (finalHasBlockAtlas) {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.multiple_atlases",
                                                entry.getKey().asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png"
                                        ));
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                                "resource_pack.texture_not_in_atlas",
                                                spritePath.asString()
                                        ));
                                    }
                                }
                            }
                            return status;
                        });
                    }
                    for (int i = 1; i < paths.length; i++) {
                        union(parent, paths[0], paths[i]);
                    }
                }

                if (Config.fixTextureAtlas()) {

                    // 使用并查集处理
                    // 按根节点汇总结果
                    Map<Key, Set<Key>> groups = new HashMap<>();
                    for (Key element : parent.keySet()) {
                        Key root = find(parent, element);
                        groups.computeIfAbsent(root, k -> new HashSet<>()).add(element);
                    }
                    Map<Key, MutableBoolean> groupedTextures = new HashMap<>();
                    for (Set<Key> set : groups.values()) {
                        MutableBoolean bool = new MutableBoolean(false);
                        for (Key key : set) {
                            groupedTextures.put(key, bool);
                        }
                    }

                    // 收集剩余未处理过的
                    for (Map.Entry<Key, SimplifiedModelFile> entry : PRESET_MODELS.entrySet()) {
                        Key modelPath = entry.getKey();
                        if (!modelsCache.containsKey(modelPath)) continue;
                        SimplifiedModelFile modelFile = entry.getValue();
                        getTexturedModel(modelPath, modelFile.parent, modelFile.textures, rootPaths, modelsCache);
                    }

                    // 创建所有贴图到模型的索引
                    Multimap<Key, TexturedModel> textureToModel = HashMultimap.create(512, 4);
                    for (Map.Entry<Key, TexturedModel> entry : modelsCache.entrySet()) {
                        TexturedModel texturedModel = entry.getValue();
                        for (Key texture : entry.getValue().textures.values()) {
                            textureToModel.put(texture, texturedModel);
                        }
                    }

                    Set<Key> blockAtlasToAdd = new HashSet<>();
                    Set<Key> itemAtlasToAdd = new HashSet<>();
                    Set<Key> itemAtlasToRemove = new HashSet<>();

                    // 分配不在图集内的贴图到适合的图集内
                    List<Key> lateInitTexture = new ArrayList<>();
                    for (Map.Entry<Key, TextureStatus> entry : textureStatuses.entrySet()) {
                        Key spritePath = entry.getKey();
                        TextureStatus status = entry.getValue();
                        if (status.shouldBeInBlockAtlas()) {
                            if (status.inItemAtlas()) {
                                // 此贴图需要链式影响
                                itemAtlasToRemove.add(spritePath);
                            }
                            if (!status.inBlockAtlas()) {
                                blockAtlasToAdd.add(spritePath);
                                groupedTextures.get(spritePath).set(true);
                            }
                        } else {
                            if (status.inItemAtlas()) {
                                continue;
                            }
                            if (status.inBlockAtlas()) {
                                continue;
                            }
                            lateInitTexture.add(spritePath);
                        }
                    }

                    for (Key spritePath : lateInitTexture) {
                        MutableBoolean inBlockAtlas = groupedTextures.get(spritePath);
                        if (inBlockAtlas.booleanValue()) {
                            blockAtlasToAdd.add(spritePath);
                        } else {
                            itemAtlasToAdd.add(spritePath);
                        }
                    }

                    Stack<Key> chainDetection = new Stack<>();
                    chainDetection.addAll(itemAtlasToRemove);
                    while (!chainDetection.isEmpty()) {
                        Key spritePath = chainDetection.pop();
                        Collection<TexturedModel> texturedModels = textureToModel.get(spritePath);
                        for (TexturedModel texturedModel : texturedModels) {
                            for (Key texture : texturedModel.textures.values()) {
                                if (itemAtlas.isDefined(texture)) {
                                    if (itemAtlasToRemove.add(texture)) {
                                        blockAtlasToAdd.add(texture);
                                        chainDetection.push(texture);
                                    }
                                }
                            }
                        }
                    }

                    itemAtlasToAdd.remove(MISSING_NO);
                    itemAtlasToRemove.remove(MISSING_NO);
                    blockAtlasToAdd.remove(MISSING_NO);

                    JsonObject fixedItemAtlas = null;
                    if (!itemAtlasToAdd.isEmpty() || !itemAtlasToRemove.isEmpty()) {
                        fixedItemAtlas = lastItemAtlas == null ? new JsonObject() : lastItemAtlas;
                        JsonArray sources = fixedItemAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedItemAtlas.add("sources", sources);
                        }
                        for (Key texture : itemAtlasToAdd) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", texture.asString());
                            sources.add(source);
                            itemAtlas.addSingle(texture);
                        }
                        for (Key texture : itemAtlasToRemove) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "filter");
                            JsonObject pattern = new JsonObject();
                            pattern.addProperty("namespace", "^" + texture.namespace() + "$");
                            pattern.addProperty("path", "^" + texture.value() + "$");
                            source.add("pattern", pattern);
                            sources.add(source);
                            itemAtlas.addDeleted(texture);
                        }
                    }

                    JsonObject fixedBlockAtlas = null;
                    if (!blockAtlasToAdd.isEmpty()) {
                        fixedBlockAtlas = lastBlocksAtlas == null ? new JsonObject() : lastBlocksAtlas;
                        JsonArray sources = fixedBlockAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedBlockAtlas.add("sources", sources);
                        }
                        for (Key texture : blockAtlasToAdd) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", texture.asString());
                            sources.add(source);
                            blockAtlas.addSingle(texture);
                        }
                    }

                    // 至少有一个修复
                    if (fixedBlockAtlas != null || fixedItemAtlas != null) {
                        result = new ValidationResult(fixedItemAtlas, lastItemAtlas, fixedBlockAtlas, lastBlocksAtlas);
                    }
                }
            } else {

                Set<Key> blockAtlasToAdd = new HashSet<>();

                // 只需要收集即可
                for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    for (Map.Entry<String, Key> texture : textures.entrySet()) {
                        Key spritePath = texture.getValue();
                        if (!blockAtlas.isDefined(spritePath)) {
                            blockAtlasToAdd.add(spritePath);
                        }
                    }
                }
                for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
                    Map<String, Key> textures = entry.getValue().textures;
                    for (Map.Entry<String, Key> texture : textures.entrySet()) {
                        Key spritePath = texture.getValue();
                        if (!blockAtlas.isDefined(spritePath)) {
                            blockAtlasToAdd.add(spritePath);
                        }
                    }
                }

                blockAtlasToAdd.remove(MISSING_NO);

                if (Config.fixTextureAtlas()) {
                    JsonObject fixedBlockAtlas = null;
                    if (!blockAtlasToAdd.isEmpty()) {
                        fixedBlockAtlas = lastBlocksAtlas == null ? new JsonObject() : lastBlocksAtlas;
                        JsonArray sources = fixedBlockAtlas.getAsJsonArray("sources");
                        if (sources == null) {
                            sources = new JsonArray();
                            fixedBlockAtlas.add("sources", sources);
                        }
                        for (Key texture : blockAtlasToAdd) {
                            JsonObject source = new JsonObject();
                            source.addProperty("type", "single");
                            source.addProperty("resource", texture.asString());
                            sources.add(source);
                            blockAtlas.addSingle(texture);
                        }
                    }

                    // 至少有一个修复
                    if (fixedBlockAtlas != null) {
                        result = new ValidationResult(null, null, fixedBlockAtlas, lastBlocksAtlas);
                    }
                } else {
                    // 骂死你臭杂鱼
                    for (Key spritePath : blockAtlasToAdd) {
                        this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                                "resource_pack.texture_not_in_atlas",
                                spritePath.asString()
                        ));
                    }
                }
            }
        }

        // 再次遍历验证贴图
        // 只允许在图集内的进行下一步操作
        for (Map.Entry<Key, TexturedModel> entry : blockModels.entrySet()) {
            Map<String, Key> textures = entry.getValue().textures;
            for (Map.Entry<String, Key> texture : textures.entrySet()) {
                Key spritePath = texture.getValue();
                Key sourceTexturePath = blockAtlas.getSourceTexturePath(spritePath);
                if (sourceTexturePath != null) {
                    textureToModels.put(sourceTexturePath, entry.getKey());
                }
            }
        }

        for (Map.Entry<Key, TexturedModel> entry : itemModels.entrySet()) {
            Map<String, Key> textures = entry.getValue().textures;
            for (Map.Entry<String, Key> texture : textures.entrySet()) {
                Key spritePath = texture.getValue();
                Key sourceTexturePath = itemAtlas == null ? null : itemAtlas.getSourceTexturePath(spritePath);
                if (sourceTexturePath != null) {
                    textureToModels.put(sourceTexturePath, entry.getKey());
                } else {
                    sourceTexturePath = blockAtlas.getSourceTexturePath(spritePath);
                    if (sourceTexturePath != null) {
                        textureToModels.put(sourceTexturePath, entry.getKey());
                    }
                }
            }
        }

        // 验证贴图是否存在
        for (Map.Entry<Key, Collection<Key>> entry : textureToModels.asMap().entrySet()) {
            Key sourceTexturePath = entry.getKey();
            if (!VANILLA_TEXTURES.contains(sourceTexturePath)) {
                String texturePath = "assets/" + sourceTexturePath.namespace() + "/textures/" + sourceTexturePath.value() + ".png";
                if (rpView.exists(texturePath)) continue;
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                        "resource_pack.missing_model_texture",
                        entry.getValue().toString(), texturePath
                ));
            }
        }

        if (result == null) {
            result = new ValidationResult(null, lastItemAtlas, null, lastBlocksAtlas);
        }

        return result;
    }

    private void validateModel(Path resourcePackPath,
                               JsonObject modelJson,
                               Path modelJsonPath,
                               String modelStringPath,
                               TexturedModel texturedModel,
                               Set<Revision> revisions,
                               ModelFixFlags flags) {
        boolean changed = false;
        boolean illegalRotation225 = false;
        boolean illegalRotation45 = false;
        boolean illegalNewRotationFormat = false;
        boolean illegalTextures = false;

        if (flags.fixTexturesFormat && modelJson.get("textures") instanceof JsonObject textures) {
            for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                if (textureEntry.getValue() instanceof JsonObject texture && texture.has("sprite")) {
                    illegalTextures = true;
                    break;
                }
            }
        }

        if (flags.fixElement && modelJson.get("elements") instanceof JsonArray elements) {
            for (JsonElement e : elements) {
                if (e instanceof JsonObject element) {

                    // 修复含有 #missing 的贴图的元素
                    if (flags.fixMissingTexture) {
                        if (element.get("faces") instanceof JsonObject faces) {
                            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                                if (faceEntry.getValue() instanceof JsonObject face) {
                                    if (face.get("texture") instanceof JsonPrimitive texture && texture.getAsString().equals("#missing")) {
                                        Optional<String> first = texturedModel.textures.keySet().stream().findFirst();
                                        if (first.isPresent()) {
                                            face.addProperty("texture", "#" + first.get());
                                            changed = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 检查含有非法旋转角度的元素
                    if (flags.fixRotationAngle) {
                        if (element.get("rotation") instanceof JsonObject rotation) {
                            if (rotation.get("angle") instanceof JsonPrimitive angle) {
                                float angleValue = angle.getAsFloat();
                                if (angleValue < -45 || angleValue > 45) {
                                    illegalRotation45 = true;
                                }
                                if (!flags.v1_21_6 && angleValue % 22.5f != 0) {
                                    illegalRotation225 = true;
                                }
                            } else if (!rotation.has("axis")) {
                                illegalNewRotationFormat = true;
                            }
                        }
                    }
                }
            }
        }

        if (illegalTextures) {
            // 如果资源包支持 26.1
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V26_1)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(Config.createOverlayFolderName(Revisions.SINCE_26_1.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_26_1);
                writeJsonSafely(modelJson, overlayModelPath);
            }

            // 再进行修复
            JsonObject newTextures = new JsonObject();
            if (modelJson.get("textures") instanceof JsonObject textures) {
                for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                    if (textureEntry.getValue() instanceof JsonObject texture) {
                        newTextures.addProperty(textureEntry.getKey(), texture.getAsJsonPrimitive("sprite").getAsString());
                    } else if (textureEntry.getValue() instanceof JsonPrimitive texture) {
                        newTextures.addProperty(textureEntry.getKey(), texture.getAsString());
                    }
                }
            }
            modelJson.add("textures", newTextures);
            changed = true;
        }

        if (illegalRotation45 || illegalNewRotationFormat) {
            // 如果支持 1.21.11
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_11)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(Config.createOverlayFolderName(Revisions.SINCE_1_21_11.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_1_21_11);
                writeJsonSafely(modelJson, overlayModelPath);
            }

            if (modelJson.get("elements") instanceof JsonArray elements) {
                for (JsonElement e : elements) {
                    if (e instanceof JsonObject element) {
                        if (element.get("rotation") instanceof JsonObject rotation) {

                            if (!rotation.has("angle")) {
                                float x = 0f;
                                float y = 0f;
                                float z = 0f;

                                if (rotation.remove("x") instanceof JsonPrimitive xp) {
                                    x = xp.getAsFloat();
                                }
                                if (rotation.remove("y") instanceof JsonPrimitive yp) {
                                    y = yp.getAsFloat();
                                }
                                if (rotation.remove("z") instanceof JsonPrimitive zp) {
                                    z = zp.getAsFloat();
                                }

                                float absX = Math.abs(x);
                                float absY = Math.abs(y);
                                float absZ = Math.abs(z);

                                float finalAngle;
                                String finalAxis;

                                if (absX >= absY && absX >= absZ) {
                                    finalAngle = x;
                                    finalAxis = "x";
                                } else if (absY >= absZ) {
                                    finalAngle = y;
                                    finalAxis = "y";
                                } else {
                                    finalAngle = z;
                                    finalAxis = "z";
                                }

                                rotation.addProperty("axis", finalAxis);
                                rotation.addProperty("angle", finalAngle);
                            }

                            if (rotation.get("angle") instanceof JsonPrimitive angle) {
                                float angleValue = angle.getAsFloat();
                                if (angleValue < -45 || angleValue > 45) {
                                    Vector3f from = VectorUtils.vector3f(element.getAsJsonArray("from"));
                                    Vector3f to = VectorUtils.vector3f(element.getAsJsonArray("to"));
                                    Vector3f origin = VectorUtils.vector3f(rotation.getAsJsonArray("origin"));
                                    String axis = rotation.getAsJsonPrimitive("axis").getAsString();

                                    CubeRotationProcessor.RotationResult result = CubeRotationProcessor.process(from, to, origin, axis, angleValue);

                                    element.add("from", VectorUtils.toJson(result.from()));
                                    element.add("to", VectorUtils.toJson(result.to()));
                                    rotation.add("origin", VectorUtils.toJson(result.origin()));
                                    rotation.addProperty("angle", result.angle());

                                    if (element.has("faces")) {
                                        JsonObject oldFaces = element.getAsJsonObject("faces");
                                        JsonObject newFaces = new JsonObject();

                                        for (CubeRotationProcessor.Face sourceFace : CubeRotationProcessor.Face.values()) {
                                            if (oldFaces.has(sourceFace.name())) {
                                                JsonObject faceObj = oldFaces.getAsJsonObject(sourceFace.name()).deepCopy();
                                                CubeRotationProcessor.FaceData mappingData = result.faceChanges().get(sourceFace);

                                                int oldRot = faceObj.has("rotation") ? faceObj.get("rotation").getAsInt() : 0;
                                                int finalRot = (oldRot + mappingData.uvRotation()) % 360;

                                                if (finalRot != 0) {
                                                    faceObj.addProperty("rotation", finalRot);
                                                } else {
                                                    faceObj.remove("rotation");
                                                }

                                                newFaces.add(mappingData.targetFace().name(), faceObj);
                                            }
                                        }
                                        element.add("faces", newFaces);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            changed = true;
        }

        // 遵循 22.5 且在 [-45, 45]
        if (illegalRotation225) {
            // 如果支持 1.21.6
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_6)) {
                // 先把高版本格式写到 overlay 内
                Path overlayModelPath = resourcePackPath
                        .resolve(Config.createOverlayFolderName(Revisions.SINCE_1_21_6.versionString()))
                        .resolve(modelStringPath);
                revisions.add(Revisions.SINCE_1_21_6);
                writeJsonSafely(modelJson, overlayModelPath);
            }

            if (modelJson.get("elements") instanceof JsonArray elements) {
                for (JsonElement e : elements) {
                    if (e instanceof JsonObject element) {
                        if (element.get("rotation") instanceof JsonObject rotation && rotation.get("angle") instanceof JsonPrimitive angle) {
                            float finalValue = Math.round(angle.getAsFloat() / 22.5f) * 22.5f;
                            rotation.addProperty("angle", finalValue);
                        }
                    }
                }
            }
            changed = true;
        }

        if (changed) {
            writeJsonSafely(modelJson, modelJsonPath);
        }
    }

    public record ModelFixFlags(boolean fixMissingTexture, boolean fixTexturesFormat, boolean fixRotationAngle, boolean fixElement, boolean v1_21_6) {}

    private static Key find(Map<Key, Key> parent, Key i) {
        if (parent.get(i).equals(i)) {
            return i;
        }
        Key root = find(parent, parent.get(i));
        parent.put(i, root); // 路径压缩
        return root;
    }

    private static void union(Map<Key, Key> parent, Key i, Key j) {
        Key rootI = find(parent, i);
        Key rootJ = find(parent, j);
        if (!rootI.equals(rootJ)) {
            parent.put(rootI, rootJ);
        }
    }

    protected record ValidationResult(JsonObject fixedItemAtlas,
                                   JsonObject originalItemAtlas,
                                   JsonObject fixedBlockAtlas,
                                   JsonObject originalBlockAtlas) {
    }

    @SuppressWarnings("all")
    public TexturedModel getTexturedModel(Key currentRL, @Nullable Key parentRL, Map<String, Key> textures, Path[] rootPaths, Map<Key, TexturedModel> cached) {
        TexturedModel cachedModel = cached.get(currentRL);
        if (cachedModel != null) {
            return cachedModel;
        }
        TexturedModel texturedModel = new TexturedModel(currentRL, textures);
        // 放这里防止parent互相引用造成死循环
        cached.put(currentRL, texturedModel);
        if (parentRL != null) {
            TexturedModel parentModel = cached.get(parentRL);
            if (parentModel == null) {
                String parentModelStringPath = "assets/" + parentRL.namespace() + "/models/" + parentRL.value() + ".json";
                // 找一下有没有自定义的父模型
                JsonObject parentModelJson = getParentModel(parentModelStringPath, rootPaths);
                if (parentModelJson != null) {
                    parentModel = getTexturedModel(parentRL, TexturedModel.getParent(parentModelJson), TexturedModel.getTextures(parentModelJson), rootPaths, cached);
                } else {
                    if (VANILLA_MODELS.contains(parentRL)) {
                        // 可能为空，因为存在built-in模型
                        SimplifiedModelFile simplifiedModelFile = PRESET_MODELS.get(parentRL);
                        if (simplifiedModelFile == null) {
                            parentModel = TexturedModel.BUILTIN;
                            cached.put(parentRL, parentModel);
                        } else {
                            parentModel = getTexturedModel(parentRL, simplifiedModelFile.parent, simplifiedModelFile.textures, rootPaths, cached);
                        }
                    } else {
                        parentModel = TexturedModel.EMPTY;
                        cached.put(parentRL, parentModel);
                    }
                }
            }
            if (parentModel == TexturedModel.EMPTY) {
                String parentModelStringPath = "assets/" + parentRL.namespace() + "/models/" + parentRL.value() + ".json";
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                        "resource_pack.missing_parent_model",
                        currentRL.asString(), parentModelStringPath
                ));
            } else {
                texturedModel.addParent(parentModel);
            }
        }
        return texturedModel;
    }

    private JsonObject getParentModel(String path, Path[] rootPaths) {
        // 倒序遍历数组
        for (int i = rootPaths.length - 1; i >= 0; i--) {
            Path rootPath = rootPaths[i];
            Path modelJsonPath = rootPath.resolve(path);
            if (Files.exists(modelJsonPath)) {
                JsonObject parentModelJson = readJsonObjectFromFileOrWarn(modelJsonPath);
                if (parentModelJson != null) {
                    return parentModelJson;
                }
            }
        }
        return null;
    }

    private static void collectMultipart(JsonArray jsonArray, Consumer<Key> callback) {
        for (JsonElement element : jsonArray) {
            if (element instanceof JsonObject jo) {
                JsonElement applyJE = jo.get("apply");
                if (applyJE instanceof JsonObject applyJO) {
                    String modelPath = applyJO.get("model").getAsString();
                    Key location = Key.from(modelPath);
                    callback.accept(location);
                } else if (applyJE instanceof JsonArray applyJA) {
                    for (JsonElement applyInnerJE : applyJA) {
                        if (applyInnerJE instanceof JsonObject applyInnerJO) {
                            String modelPath = applyInnerJO.get("model").getAsString();
                            Key location = Key.from(modelPath);
                            callback.accept(location);
                        }
                    }
                }
            }
        }
    }

    private static void collectVariants(String block, JsonObject jsonObject, BiConsumer<Key, String> callback) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue() instanceof JsonObject entryJO) {
                String modelPath = entryJO.get("model").getAsString();
                Key location = Key.from(modelPath);
                callback.accept(location, block + "[" + entry.getKey() + "]");
            } else if (entry.getValue() instanceof JsonArray entryJA) {
                for (JsonElement entryInnerJE : entryJA) {
                    if (entryInnerJE instanceof JsonObject entryJO) {
                        String modelPath = entryJO.get("model").getAsString();
                        Key location = Key.from(modelPath);
                        callback.accept(location, block + "[" + entry.getKey() + "]");
                    }
                }
            }
        }
    }

    private static void collectItemModelsDeeply(JsonObject jo, Consumer<Key> callback) {
        JsonElement modelJE = jo.get("model");
        if (modelJE instanceof JsonPrimitive jsonPrimitive) {
            Key location = Key.from(jsonPrimitive.getAsString());
            callback.accept(location);
            return;
        }
        if (jo.has("type") && jo.has("base")) {
            if (jo.get("type") instanceof JsonPrimitive jp1 && jo.get("base") instanceof JsonPrimitive jp2) {
                String type = jp1.getAsString();
                if (type.equals("minecraft:special") || type.equals("special")) {
                    Key location = Key.from(jp2.getAsString());
                    callback.accept(location);
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            if (entry.getValue() instanceof JsonObject innerJO) {
                collectItemModelsDeeply(innerJO, callback);
            } else if (entry.getValue() instanceof JsonArray innerJA) {
                for (JsonElement innerElement : innerJA) {
                    if (innerElement instanceof JsonObject innerJO) {
                        collectItemModelsDeeply(innerJO, callback);
                    }
                }
            }
        }
    }

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

    private void generateEquipments(Path generatedPackPath, Consumer<Revision> callback) {
        // asset id + 是否有上身 + 是否有腿
        List<Tuple<Key, Boolean, Boolean>> collectedTrims = new ArrayList<>();

        // 为trim类型提供的两个兼容性值
        boolean needLegacyCompatibility = Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2);
        boolean needModernCompatibility = Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2);

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
            if (needModernCompatibility) {
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
            writeJsonSafely(needLegacyCompatibility ? legacyTrimAtlasJson : modernTrimAtlasJson, atlasPath);
            // 既要又要，那么需要overlay
            if (needLegacyCompatibility && needModernCompatibility) {
                Revision revision = Revisions.SINCE_1_21_2;
                Path overlayAtlasPath = generatedPackPath
                        .resolve(Config.createOverlayFolderName(revision.versionString()))
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("atlases")
                        .resolve("armor_trims.json");
                // 创建atlas文件夹
                writeJsonSafely(modernTrimAtlasJson, overlayAtlasPath);
                callback.accept(revision);
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
    private Pair<Boolean, Boolean> processTrimBasedEquipment(TrimBasedEquipment trimBasedEquipment, Path generatedPackPath) {
        Key assetId = trimBasedEquipment.assetId();

        Key humanoidIdentifier = trimBasedEquipment.humanoid();
        boolean hasLayer1 = humanoidIdentifier != null;
        Key humanoidLeggingsIdentifier = trimBasedEquipment.humanoidLeggings();
        boolean hasLayer2 = humanoidLeggingsIdentifier != null;

        if (hasLayer1) {
            Path texture = generatedPackPath
                    .resolve("assets")
                    .resolve(humanoidIdentifier.namespace())
                    .resolve("textures")
                    .resolve(humanoidIdentifier.value() + ".png");
            if (!Files.exists(texture) || !Files.isRegularFile(texture)) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                        "resource_pack.missing_equipment_texture",
                        assetId.asString(), texture.toString()
                ));
                return null;
            }
            boolean shouldPreserve = false;
            if (Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2)) {
                Path legacyTarget = generatedPackPath
                        .resolve("assets")
                        .resolve(assetId.namespace())
                        .resolve("textures")
                        .resolve("trims")
                        .resolve("models")
                        .resolve("armor")
                        .resolve(assetId.value() + "_" + NEW_TRIM_MATERIAL + ".png");
                if (!legacyTarget.equals(texture)) {
                    try {
                        Files.createDirectories(legacyTarget.getParent());
                        Files.copy(texture, legacyTarget, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error writing armor texture file from " + texture + " to " + legacyTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2)) {
                Path modernTarget = generatedPackPath
                        .resolve("assets")
                        .resolve(assetId.namespace())
                        .resolve("textures")
                        .resolve("trims")
                        .resolve("entity")
                        .resolve("humanoid")
                        .resolve(assetId.value() + "_" + NEW_TRIM_MATERIAL + ".png");
                if (!modernTarget.equals(texture)) {
                    try {
                        Files.createDirectories(modernTarget.getParent());
                        Files.copy(texture, modernTarget, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error writing armor texture file from " + texture + " to " + modernTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (!shouldPreserve) {
                try {
                    Files.delete(texture);
                } catch (IOException e) {
                    this.plugin.logger().error("Error deleting armor texture file from " + texture, e);
                }
            }
        }
        if (hasLayer2) {
            Path texture = generatedPackPath
                    .resolve("assets")
                    .resolve(humanoidLeggingsIdentifier.namespace())
                    .resolve("textures")
                    .resolve(humanoidLeggingsIdentifier.value() + ".png");
            if (!Files.exists(texture) && !Files.isRegularFile(texture)) {
                this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                        "resource_pack.missing_equipment_texture",
                        assetId.asString(), texture.toString()
                ));
                return null;
            }
            boolean shouldPreserve = false;
            if (Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2)) {
                Path legacyTarget = generatedPackPath
                        .resolve("assets")
                        .resolve(assetId.namespace())
                        .resolve("textures")
                        .resolve("trims")
                        .resolve("models")
                        .resolve("armor")
                        .resolve(assetId.value() + "_leggings_" + NEW_TRIM_MATERIAL + ".png");
                if (!legacyTarget.equals(texture)) {
                    try {
                        Files.createDirectories(legacyTarget.getParent());
                        Files.copy(texture, legacyTarget, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error writing armor texture file from " + texture + " to " + legacyTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2)) {
                Path modernTarget = generatedPackPath
                        .resolve("assets")
                        .resolve(assetId.namespace())
                        .resolve("textures")
                        .resolve("trims")
                        .resolve("entity")
                        .resolve("humanoid_leggings")
                        .resolve(assetId.value() + "_" + NEW_TRIM_MATERIAL + ".png");
                if (!modernTarget.equals(texture)) {
                    try {
                        Files.createDirectories(modernTarget.getParent());
                        Files.copy(texture, modernTarget, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        this.plugin.logger().error("Error writing armor texture file from " + texture + " to " + modernTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (!shouldPreserve) {
                try {
                    Files.delete(texture);
                } catch (IOException e) {
                    this.plugin.logger().error("Error deleting armor texture file from " + texture, e);
                }
            }
        }

        return Pair.of(hasLayer1, hasLayer2);
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
            for (Map.Entry<String, String> pair : entry.getValue().translations.entrySet()) {
                json.addProperty(pair.getKey(), pair.getValue());
            }
            writeJsonSafely(json, langPath);
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
            for (SoundEvent soundEvent : entry.getValue()) {
                soundJson.add(soundEvent.id().value(), soundEvent.get());
            }
            writeJsonSafely(soundJson, soundPath);
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

        writeJsonSafely(soundJson, soundPath);
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
                        String variantName = variantEntry.getKey();
                        if (!newVariants.has(variantName)) {
                            newVariants.add(variantName, variantEntry.getValue());
                        }
                    }
                }
                for (Map.Entry<String, JsonElement> resourcePathEntry : entry.getValue().entrySet()) {
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
            if (!blueMapBlockStates.isEmpty()) {
                writeJsonSafely(blueMapBlockStates, generatedPackPath.resolve("assets").resolve(Key.CRAFTENGINE_NAMESPACE).resolve("blockColors.json"));
            }
        }
    }

    private void deleteMapCompatibilityAssets(Path generatedPackPath) {
        Path assetPath = generatedPackPath.resolve("assets");
        Path blueMapblockColorsFile = assetPath.resolve(Key.CRAFTENGINE_NAMESPACE).resolve("blockColors.json");
        if (Files.exists(blueMapblockColorsFile)) {
            try {
                Files.delete(blueMapblockColorsFile);
            } catch (IOException ignored) {
            }
        }
    }

    private void deleteModAssets(Path generatedPackPath) {
        Path assetPath = generatedPackPath.resolve("assets");
        Path blockStatePath = assetPath.resolve(Key.CRAFTENGINE_NAMESPACE).resolve("blockstates");
        for (Map.Entry<Integer, JsonElement> entry : this.plugin.blockManager().modBlockStates().entrySet()) {
            Path overridedBlockPath = blockStatePath.resolve("custom_" + entry.getKey() + ".json");
            if (Files.exists(overridedBlockPath)) {
                try {
                    Files.delete(overridedBlockPath);
                } catch (IOException ignored) {
                }
            }
        }
        FileUtils.deleteEmptyParentDirectories(blockStatePath, assetPath);
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

                itemJson.addProperty("parent", firstBaseModel.model().asMinimalString());
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

            for (BitmapImage image : font.bitmapImages()) {
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
        Map<String, List<Path>> conflictChecker = new HashMap<>(Math.max(128, this.cachedAssetFiles.size()), 0.6f);
        Map<Path, CachedAssetFile> previousFiles = this.cachedAssetFiles;
        this.cachedAssetFiles = new HashMap<>(Math.max(128, this.cachedAssetFiles.size()), 0.6f);

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
            processZipFile(zip, zip.getParent(), fs, conflictChecker, previousFiles);
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
        CachedAssetFile cachedAsset = previousFiles.get(file);
        long lastModified = attrs.lastModifiedTime().toMillis();
        long size = attrs.size();
        if (cachedAsset != null && cachedAsset.lastModified() == lastModified && cachedAsset.size() == size) {
            this.cachedAssetFiles.put(file, cachedAsset);
        } else {
            cachedAsset = new CachedAssetFile(Files.readAllBytes(file), lastModified, size);
            this.cachedAssetFiles.put(file, cachedAsset);
        }
        if (fs == null) return;
        Path relative = sourceFolder.relativize(file);
        updateConflictChecker(fs, conflictChecker, file, file, relative, cachedAsset.data());
    }

    private void processZipFile(Path zipFile, Path sourceFolder, @Nullable FileSystem fs,
                                Map<String, List<Path>> conflictChecker, Map<Path, CachedAssetFile> previousFiles) {
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
                    Path entryPathInZip = zipRoot.relativize(entry);
                    Path sourcePath = Path.of(zipFile + "!" + entryPathInZip);
                    CachedAssetFile cachedAsset = previousFiles.get(sourcePath);
                    if (cachedAsset != null && cachedAsset.lastModified() == zipLastModified && cachedAsset.size() == zipSize) {
                        cachedAssetFiles.put(sourcePath, cachedAsset);
                    } else {
                        byte[] data = Files.readAllBytes(entry);
                        cachedAsset = new CachedAssetFile(data, zipLastModified, zipSize);
                        cachedAssetFiles.put(sourcePath, cachedAsset);
                    }
                    if (fs != null) {
                        try {
                            updateConflictChecker(fs, conflictChecker, entry, sourcePath, entryPathInZip, cachedAsset.data());
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
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            this.plugin.logger().error("Error creating " + path.toAbsolutePath(), e);
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            GsonHelper.get().toJson(json, writer);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to write json file " + json.toString() + " to " + path, e);
        }
    }

    @Override
    public ConfigParser parser() {
        return this.parser;
    }

    public static final class SkipOptimizationParser extends SectionConfigParser {
        private static final String[] SECTION_ID = new String[] {"skip-optimization", "skip_optimization"};
        private final Set<String> excludeTexture = new HashSet<>();
        private final Set<String> excludeJson = new HashSet<>();

        public SkipOptimizationParser() {
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
            if (!Config.optimizeResourcePack()) return;
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
}
