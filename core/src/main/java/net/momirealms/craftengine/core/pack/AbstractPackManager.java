package net.momirealms.craftengine.core.pack;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.EquipmentLayerType;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
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
import net.momirealms.craftengine.core.pack.mcmeta.PackMcMeta;
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
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.SectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.LangData;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

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
            ItemKeys.LEATHER_HELMET, ItemKeys.LEATHER_CHESTPLATE, ItemKeys.LEATHER_LEGGINGS, ItemKeys.LEATHER_BOOTS,
            ItemKeys.WOLF_ARMOR, ItemKeys.LEATHER_HORSE_ARMOR
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
    private final Map<String, Pack> loadedPacks = new HashMap<>();
    private final Map<String, ConfigParser> sectionParsers = new HashMap<>();
    private final TreeSet<ConfigParser> sortedParsers = new TreeSet<>();
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to read internal/atlases/blocks.json", e);
        }
        try (InputStream inputStream = plugin.resourceStream("internal/atlases/items.json")) {
            this.vanillaItemAtlas = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
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
                SIMPLIFIED_MODEL_READERS.put(item, DyeableModelReader.INSTANCE);
            } else {
                SIMPLIFIED_MODEL_READERS.put(item, GeneratedModelReader.GENERATED);
            }
        }

        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FISHING_ROD, ConditionModelReader.FISHING_ROD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.ELYTRA, ConditionModelReader.ELYTRA);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.SHIELD, ConditionModelReader.SHIELD);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.BOW, BowModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.CROSSBOW, CrossbowModelReader.INSTANCE);
        SIMPLIFIED_MODEL_READERS.put(ItemKeys.FIREWORK_STAR, FireworkStarModelReader.INSTANCE);
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        Map<String, Object> arguments;
        if (hostingObj instanceof Map<?,?>) {
            arguments = MiscUtils.castToMap(hostingObj, false);
        } else if (hostingObj instanceof List<?> list && !list.isEmpty()) {
            arguments = MiscUtils.castToMap(list.getFirst(), false);
        } else {
            this.resourcePackHost = NoneHost.INSTANCE;
            return;
        }
        try {
            // we might add multiple host methods in future versions
            this.resourcePackHost = ResourcePackHosts.fromMap(arguments);
        } catch (LocalizedException e) {
            if (e instanceof LocalizedResourceConfigException exception) {
                exception.setPath(plugin.dataFolderPath().resolve("config.yml"));
                e.setArgument(1, "hosting");
            }
            TranslationManager.instance().log(e.node(), e.arguments());
            this.resourcePackHost = NoneHost.INSTANCE;
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to load resource pack host", e);
            this.resourcePackHost = NoneHost.INSTANCE;
        }
    }

    @Override
    public ResourcePackHost resourcePackHost() {
        return this.resourcePackHost;
    }

    @Override
    public void loadResources(Predicate<ConfigParser> predicate) {
        this.loadResourceConfigs(predicate);
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
            if (ReflectionUtils.UNSAFE.allocateInstance(c).equals(this)) initInternalData();
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
        this.sortedParsers.add(parser);
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
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(resourcesFolder)) {
                for (Path path : paths) {
                    if (!Files.isDirectory(path)) {
                        this.plugin.logger().warn(path.toAbsolutePath() + " is not a directory");
                        continue;
                    }
                    String namespace = path.getFileName().toString();
                    if (namespace.charAt(0) == '.') {
                        continue;
                    }
                    if (!ResourceLocation.isValidNamespace(namespace)) {
                        namespace = "minecraft";
                    }
                    Path metaFile = path.resolve("pack.yml");
                    String description = null;
                    String version = null;
                    String author = null;
                    boolean enable = true;
                    List<String> subPacks = new ArrayList<>();
                    if (Files.exists(metaFile) && Files.isRegularFile(metaFile)) {
                        Yaml yaml = new Yaml(new StringKeyConstructor(path, new LoaderOptions()));
                        try (InputStream is = Files.newInputStream(metaFile)) {
                            Map<String, Object> data = yaml.load(is);
                            if (data != null) {
                                enable = ResourceConfigUtils.getAsBoolean(data.getOrDefault("enable", true), "enable");
                                namespace = data.getOrDefault("namespace", namespace).toString();
                                description = Optional.ofNullable(data.get("description")).map(String::valueOf).orElse(null);
                                version = Optional.ofNullable(data.get("version")).map(String::valueOf).orElse(null);
                                author = Optional.ofNullable(data.get("author")).map(String::valueOf).orElse(null);
                                Map<String, Object> subpacks = ResourceConfigUtils.getAsMapOrNull(data.get("subpacks"), "subpacks");
                                if (subpacks != null) {
                                    for (Map.Entry<String, Object> entry : subpacks.entrySet()) {
                                        if (ResourceConfigUtils.getAsBoolean(entry.getValue(), entry.getKey())) {
                                            subPacks.add(entry.getKey());
                                        }
                                    }
                                }
                            } else {
                                this.plugin.logger().warn("Failed to load resource meta file: " + metaFile);
                            }
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to load " + metaFile, e);
                        }
                    }
                    Pack pack = new Pack(path, new PackMeta(author, description, version, namespace), enable, subPacks.toArray(new String[0]));
                    this.loadedPacks.put(path.getFileName().toString(), pack);
                    this.plugin.logger().info(TranslationManager.instance().translateLog("info.pack.load", pack.folder().getFileName().toString(), namespace));
                }
            }
        } catch (IOException e) {
            this.plugin.logger().severe("Error loading packs", e);
        }
    }

    public void saveDefaultConfigs() {
        // remove shulker head
        plugin.saveResource("resources/remove_shulker_head/resourcepack/pack.mcmeta");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/assets/minecraft/shaders/core/rendertype_entity_solid.fsh");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/1_20_5_remove_shulker_head_overlay/minecraft/shaders/core/rendertype_entity_solid.fsh");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/assets/minecraft/textures/entity/shulker/shulker_white.png");
        plugin.saveResource("resources/remove_shulker_head/pack.yml");

        // legacy armor
        plugin.saveResource("resources/legacy_armor/configuration/chainmail.yml");
        plugin.saveResource("resources/legacy_armor/pack.yml");

        // internal
        plugin.saveResource("resources/internal/pack.yml");
        plugin.saveResource("resources/internal/configuration/fix_client_visual.yml");
        plugin.saveResource("resources/internal/configuration/offset_chars.yml");
        plugin.saveResource("resources/internal/configuration/gui.yml");
        plugin.saveResource("resources/internal/configuration/mappings.yml");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/trims/entity/humanoid/chainmail.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/trims/entity/humanoid_leggings/chainmail.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/offset/space_split.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/item_browser.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/category.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/blasting.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/smoking.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/smelting.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/campfire.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/stonecutting_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/smithing_transform_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/cooking_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/crafting_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/brewing_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/no_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/get_item.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/next_page_0.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/next_page_1.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/previous_page_0.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/previous_page_1.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/return.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/exit.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/cooking_info.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/cooking_info.png.mcmeta");

        // default
        plugin.saveResource("resources/default/pack.yml");
        // pack meta
        plugin.saveResource("resources/default/resourcepack/pack.mcmeta");
        plugin.saveResource("resources/default/resourcepack/pack.png");
        // configs
        plugin.saveResource("resources/default/configuration/templates/block_settings.yml");
        plugin.saveResource("resources/default/configuration/templates/block_states.yml");
        plugin.saveResource("resources/default/configuration/templates/models.yml");
        plugin.saveResource("resources/default/configuration/templates/loot_tables.yml");
        plugin.saveResource("resources/default/configuration/templates/recipes.yml");
        plugin.saveResource("resources/default/configuration/templates/tool_levels.yml");
        plugin.saveResource("resources/default/configuration/templates/events.yml");
        plugin.saveResource("resources/default/configuration/categories.yml");
        plugin.saveResource("resources/default/configuration/emoji.yml");
        plugin.saveResource("resources/default/configuration/lang.yml");
        plugin.saveResource("resources/default/configuration/items/cap.yml");
        plugin.saveResource("resources/default/configuration/items/flame_elytra.yml");
        plugin.saveResource("resources/default/configuration/items/gui_head.yml");
        plugin.saveResource("resources/default/configuration/items/topaz_armor.yml");
        plugin.saveResource("resources/default/configuration/items/topaz_tool_weapon.yml");
        plugin.saveResource("resources/default/configuration/items/drill.yml");
        plugin.saveResource("resources/default/configuration/furniture/bench.yml");
        plugin.saveResource("resources/default/configuration/furniture/wooden_chair.yml");
        plugin.saveResource("resources/default/configuration/furniture/flower_basket.yml");
        plugin.saveResource("resources/default/configuration/furniture/globe.yml");
        plugin.saveResource("resources/default/configuration/blocks/chessboard_block.yml");
        plugin.saveResource("resources/default/configuration/blocks/chinese_lantern.yml");
        plugin.saveResource("resources/default/configuration/blocks/copper_coil.yml");
        plugin.saveResource("resources/default/configuration/blocks/ender_pearl_flower.yml");
        plugin.saveResource("resources/default/configuration/blocks/fairy_flower.yml");
        plugin.saveResource("resources/default/configuration/blocks/flame_cane.yml");
        plugin.saveResource("resources/default/configuration/blocks/gunpowder_block.yml");
        plugin.saveResource("resources/default/configuration/blocks/palm_tree.yml");
        plugin.saveResource("resources/default/configuration/blocks/pebble.yml");
        plugin.saveResource("resources/default/configuration/blocks/reed.yml");
        plugin.saveResource("resources/default/configuration/blocks/safe_block.yml");
        plugin.saveResource("resources/default/configuration/blocks/sofa.yml");
        plugin.saveResource("resources/default/configuration/blocks/table_lamp.yml");
        plugin.saveResource("resources/default/configuration/blocks/topaz_ore.yml");
        plugin.saveResource("resources/default/configuration/blocks/netherite_anvil.yml");
        plugin.saveResource("resources/default/configuration/blocks/amethyst_torch.yml");
        plugin.saveResource("resources/default/configuration/blocks/hami_melon.yml");
        plugin.saveResource("resources/default/configuration/blocks/magma_plant.yml");
        // assets
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/font/image/emojis.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/netherite_anvil.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/netherite_anvil_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/solid_gunpowder_block.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/gunpowder_block.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_side.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_on.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_on_side.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chessboard_block.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/safe_block_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/safe_block_bottom.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/safe_block_side.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/safe_block_front.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/safe_block_front_open.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/magma_plant.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/magma_plant.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/magma_fruit.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/magma_fruit.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_rod.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_rod_cast.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_bow.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_bow_pulling_0.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_bow_pulling_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_bow_pulling_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow_arrow.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow_firework.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow_pulling_0.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow_pulling_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow_pulling_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_crossbow.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_trident.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_trident_3d.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/entity/equipment/humanoid/topaz.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/entity/equipment/humanoid_leggings/topaz.png");
        for (String item : List.of("helmet", "chestplate", "leggings", "boots", "pickaxe", "axe", "sword", "hoe", "shovel")) {
            plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_" + item + ".png");
            plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz_" + item + ".png.mcmeta");
        }
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flame_elytra.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flame_elytra_broken.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/entity/equipment/wings/flame_elytra.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/cap.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/cap.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/pebble.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/pebble.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_1.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_2.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_3.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/sleeper_sofa.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/sofa_inner.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/sofa.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/sofa.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/deepslate_topaz_ore.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/deepslate_topaz_ore.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/topaz_ore.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/topaz_ore.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_sapling.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_planks.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_log.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_log_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/stripped_palm_log.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/stripped_palm_log_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_leaves.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_trapdoor.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_door_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_door_bottom.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/palm_door.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_3.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_4.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/reed.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/amethyst_torch.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/flame_cane_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/flame_cane_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/ender_pearl_flower_stage_0.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/ender_pearl_flower_stage_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/ender_pearl_flower_stage_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/fairy_flower.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/reed.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flame_cane.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/ender_pearl_flower_seeds.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/fairy_flower_1.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/reed.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/topaz_trident_in_hand.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/topaz_trident_throwing.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/table_lamp.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/wooden_chair.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/bench.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/table_lamp.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/table_lamp_on.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/wooden_chair.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/bench.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_ceiling.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_ground.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_wall.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/globe.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/globe_base.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/globe_earth.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flower_basket.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flower_basket_2d.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/drill.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/drill_selected.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/drill_selected.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/hami_melon.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/hami_melon_bottom.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/hami_melon_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/hami_melon_slice.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/hami_melon_seeds.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/globe.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/fence_side.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/magma_plant_stage_0.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/magma_plant_stage_1.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/magma_plant_stage_2.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/magma_plant_stage_3.json");
    }

    @Override
    public void updateCachedConfigFiles() {
        Map<Path, CachedConfigFile> previousFiles = this.cachedConfigFiles;
        this.cachedConfigFiles = new HashMap<>(64, 0.5f);
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
                                    try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                                        LoaderOptions loaderOptions = new LoaderOptions();
                                        loaderOptions.setNestingDepthLimit(256);
                                        Yaml yaml = new Yaml(new StringKeyConstructor(path, loaderOptions));
                                        Map<String, Object> data = yaml.load(inputStream);
                                        if (data == null)  return FileVisitResult.CONTINUE;
                                        cachedFile = new CachedConfigFile(data, pack, lastModifiedTime, size);
                                        AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                    } catch (IOException e) {
                                        AbstractPackManager.this.plugin.logger().severe("Error while reading config file: " + path, e);
                                        return FileVisitResult.CONTINUE;
                                    } catch (ScannerException e) {
                                        if (e.getMessage() != null && e.getMessage().contains("TAB") && e.getMessage().contains("indentation")) {
                                            try {
                                                String content = Files.readString(path);
                                                content = content.replace("\t", "    ");
                                                Files.writeString(path, content);
                                            } catch (Exception ex) {
                                                AbstractPackManager.this.plugin.logger().severe("Failed to fix tab indentation in config file: " + path, ex);
                                            }
                                        } else {
                                            AbstractPackManager.this.plugin.logger().severe("Error found while reading config file: " + path, e);
                                        }
                                        return FileVisitResult.CONTINUE;
                                    } catch (ParserException e) {
                                        AbstractPackManager.this.plugin.logger().severe("Invalid YAML file found: " + path + ".\n" + e.getMessage() + "\nIt is recommended to use Visual Studio Code as your YAML editor to fix problems more quickly.");
                                        return FileVisitResult.CONTINUE;
                                    } catch (LocalizedException e) {
                                        e.setArgument(0, path.toString());
                                        TranslationManager.instance().log(e.node(), e.arguments());
                                        return FileVisitResult.CONTINUE;
                                    }
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
                                        AbstractPackManager.this.plugin.logger().severe("Error while reading config file: " + path, e);
                                        return FileVisitResult.CONTINUE;
                                    } catch (JsonParseException e) {
                                        AbstractPackManager.this.plugin.logger().severe("Invalid JSON file found: " + path + ".\n" + e.getMessage() + "\nIt is recommended to use Visual Studio Code as your JSON editor to fix problems more quickly.");
                                        return FileVisitResult.CONTINUE;
                                    } catch (LocalizedException e) {
                                        e.setArgument(0, path.toString());
                                        TranslationManager.instance().log(e.node(), e.arguments());
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
                    this.plugin.logger().severe("Error while reading config file", e);
                }
            }
        }
    }

    private void loadResourceConfigs(Predicate<ConfigParser> predicate) {
        for (ConfigParser parser : this.sortedParsers) {
            if (!predicate.test(parser)) {
                continue;
            }
            long t1 = System.nanoTime();
            parser.preProcess();
            parser.loadAll();
            parser.postProcess();
            long t2 = System.nanoTime();
            int count = parser.count();
            if (parser.silentIfNotExists() && count == 0) {
               continue;
            }
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource.load",
                    parser.sectionId()[0], String.format("%.2f", ((t2 - t1) / 1_000_000.0)), String.valueOf(count)));
        }
    }

    @Override
    public void clearResourceConfigs() {
        for (ConfigParser parser : this.sortedParsers) {
            parser.clear();
        }
    }

    private void processConfigEntry(Map.Entry<String, Object> entry, Path path, Pack pack, BiConsumer<ConfigParser, CachedConfigSection> callback) {
        if (entry.getValue() instanceof Map<?,?> typeSections0) {
            String key = entry.getKey();
            int hashIndex = key.indexOf('#');
            String configType = hashIndex != -1 ? key.substring(0, hashIndex) : key;
            Optional.ofNullable(this.sectionParsers.get(configType))
                    .ifPresent(parser -> {
                        callback.accept(parser, new CachedConfigSection(key, castToMap(typeSections0, false), path, pack));
                    });
        }
    }

    @Override
    public void generateResourcePack() {
        this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.generate.start"));
        long time1 = System.currentTimeMillis();

        // Create cache data
        PackCacheData cacheData = new PackCacheData(this.plugin);
        this.cacheEventDispatcher.accept(cacheData);

        // get the target location
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            // firstly merge existing folders
            Path generatedPackPath = fs.getPath("resource_pack");
            List<Pair<String, List<Path>>> duplicated = this.updateCachedAssets(cacheData, fs);
            if (!duplicated.isEmpty()) {
                plugin.logger().severe(AdventureHelper.miniMessage().stripTags(TranslationManager.instance().miniMessageTranslation("warning.config.pack.duplicated_files")));
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

            HashSet<Revision> revisions = new HashSet<>();
            this.generateFonts(generatedPackPath);
            this.generateItemModels(generatedPackPath, this.plugin.itemManager());
            this.generateItemModels(generatedPackPath, this.plugin.blockManager());
            this.generateBlockOverrides(generatedPackPath);
            this.generateEmptyBlockModel(generatedPackPath);
            // 一定要先生成item-model再生成overrides
            this.generateModernItemModels1_21_2(generatedPackPath);
            this.generateModernItemModels1_21_4(generatedPackPath, revisions::add);
            this.generateLegacyItemOverrides(generatedPackPath);
            this.generateModernItemOverrides(generatedPackPath, revisions::add);
            this.generateOverrideSounds(generatedPackPath);
            this.generateCustomSounds(generatedPackPath);
            this.generateClientLang(generatedPackPath);
            this.generateEquipments(generatedPackPath, revisions::add);
            this.generateParticle(generatedPackPath);

            Path packMcMetaPath = generatedPackPath.resolve("pack.mcmeta");
            JsonObject packMcMeta = Files.isRegularFile(packMcMetaPath) ? GsonHelper.readJsonFile(packMcMetaPath).getAsJsonObject() : new JsonObject();
            // 生成revision overlay
            this.generateRevisionOverlays(packMcMeta, revisions);

            if (Config.excludeShaders()) {
                this.removeAllShaders(generatedPackPath);
            }
            long time2 = System.currentTimeMillis();
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.generate.finish", String.valueOf(time2 - time1)));
            // 校验资源包
            if (Config.validateResourcePack()) {
                this.validateResourcePack(generatedPackPath, packMcMeta);
            }
            long time3 = System.currentTimeMillis();
            if (Config.validateResourcePack()) {
                this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.validate.finish", String.valueOf(time3 - time2)));
            }
            // 验证完成后，应该重新校验pack.mcmeta并写入
            this.validatePackMetadata(generatedPackPath.resolve("pack.mcmeta"), packMcMeta);
            // 优化资源包
            if (Config.optimizeResourcePack()) {
                this.optimizeResourcePack(generatedPackPath);
            }
            long time4 = System.currentTimeMillis();
            if (Config.optimizeResourcePack()) {
                this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.optimize.finish", String.valueOf(time4 - time3)));
            }
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.create.start"));
            Path finalPath = resourcePackPath();
            Files.createDirectories(finalPath.getParent());
            if (VersionHelper.PREMIUM && Config.createUnprotectedCopy()) {
                Path newPath = finalPath.resolveSibling("unprotected_" + finalPath.getFileName());
                try {
                    ZipUtils.compress(generatedPackPath, newPath);
                } catch (IOException e) {
                    this.plugin.logger().severe("Error zipping unprotected resource pack", e);
                }
            }
            try {
                this.zipGenerator.accept(generatedPackPath, finalPath);
            } catch (Exception e) {
                this.plugin.logger().severe("Error zipping resource pack", e);
            }
            long time5 = System.currentTimeMillis();
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.create.finish", String.valueOf(time5 - time4)));
            this.generationEventDispatcher.accept(generatedPackPath, finalPath);
        } catch (IOException e) {
            this.plugin.logger().severe("Error generating resource pack", e);
        }
    }

    private void validatePackMetadata(Path path, JsonObject rawMeta) throws IOException {
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
            PackMcMeta mcMeta = new PackMcMeta(rawMeta);
            List<Overlay> overlays = mcMeta.overlays();
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

        GsonHelper.writeJsonFile(rawMeta, path);
    }

    // 这里都是随便写写的，重点在之后的校验里
    private void generateRevisionOverlays(JsonObject rawMeta, Set<Revision> revisions) throws IOException {
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
            plugin.logger().warn("Failed to collect overlays for " + path.toAbsolutePath(), e);
            return;
        }
        for (Path rootPath : rootPaths) {
            Path shadersPath = rootPath.resolve("assets/minecraft/shaders");
            try {
                FileUtils.deleteDirectory(shadersPath);
            } catch (IOException e) {
                plugin.logger().warn("Failed to delete shaders directory for " + shadersPath.toAbsolutePath(), e);
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
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.optimize.json"));
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
                    } catch (IOException | JsonParseException ignored) {
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
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.optimize.result", formatSize(originalSize), formatSize(optimizedSize), String.format("%.2f%%", compressionRatio)));
        }

        if (Config.optimizeTexture()) {
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.optimize.texture"));
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
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.optimize.result", formatSize(originalSize), formatSize(optimizedSize), String.format("%.2f%%", compressionRatio)));
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

    private void validateResourcePack(Path path, JsonObject packMetaJson) {
        PackMcMeta packMeta = new PackMcMeta(packMetaJson);
        List<OverlayCombination.Segment> segments = new ArrayList<>();
        // 完全小于1.21.11或完全大于1.21.11
        if (Config.packMaxVersion().isBelow(MinecraftVersion.V1_21_11) || Config.packMinVersion().isAtOrAbove(MinecraftVersion.V1_21_11)) {
            OverlayCombination combination = new OverlayCombination(packMeta.overlays(), Config.packMinVersion().majorPackFormat(), Config.packMaxVersion().majorPackFormat());
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
            OverlayCombination combinationLegacy = new OverlayCombination(packMeta.overlays(), Config.packMinVersion().majorPackFormat(), 72 /* 25w44a */);
            while (combinationLegacy.hasNext()) {
                OverlayCombination.Segment segment = combinationLegacy.nextSegment();
                if (segment != null) {
                    segments.add(segment);
                } else {
                    break;
                }
            }
            OverlayCombination combinationModern = new OverlayCombination(packMeta.overlays(), 73 /* 25w45a */, Config.packMaxVersion().majorPackFormat());
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

        Set<Path> fixedModels = new HashSet<>();
        for (int i = 0, size = segments.size(); i < size; i++) {
            OverlayCombination.Segment segment = segments.get(i);
            List<Path> rootPathList = new ArrayList<>();
            rootPathList.add(path);
            List<Overlay> overlayInOrder = new ArrayList<>(segment.overlays().size());
            for (Overlay overlay : packMeta.overlays()) {
                if (segment.overlays().contains(overlay)) {
                    Path resolve = path.resolve(overlay.directory());
                    if (Files.isDirectory(resolve)) {
                        overlayInOrder.add(overlay);
                        rootPathList.add(resolve);
                    }
                }
            }
            this.plugin.logger().info(TranslationManager.instance().translateLog("info.resource_pack.validate.start",
                    String.valueOf(i + 1), String.valueOf(size), String.valueOf(segment.min()), String.valueOf(segment.max()), overlayInOrder.stream().map(Overlay::directory).toList().toString()));
            ValidationResult result = validateOverlayedResourcePack(rootPathList.toArray(new Path[0]), segment.max() >= MinecraftVersion.V1_21_11.packFormat().major(), fixedModels);
            if (Config.fixTextureAtlas() && !Config.enableObfuscation()) {
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

        // 尝试修复atlas
        if (Config.fixTextureAtlas() && !Config.enableObfuscation()) {
            Map<String, JsonObject> atlasToAdd = new LinkedHashMap<>();
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
                try {
                    Files.createDirectories(atlasPath.getParent());
                    GsonHelper.writeJsonFile(entry.atlas(), atlasPath);
                    if (!atlasToAdd.containsKey(directoryName)) {
                        Overlay overlay = new Overlay(new PackVersion(min), new PackVersion(max), directoryName);
                        atlasToAdd.put(directoryName, overlay.getAsOverlayEntry(true));
                    }
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to write atlas " + atlasPath.toAbsolutePath(), e);
                }
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
                    try {
                        Files.createDirectories(atlasPath.getParent());
                        GsonHelper.writeJsonFile(entry.atlas(), atlasPath);
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to write atlas " + atlasPath.toAbsolutePath(), e);
                    }
                } else {
                    Path atlasPath = path.resolve(directoryName)
                            .resolve("assets")
                            .resolve("minecraft")
                            .resolve("atlases")
                            .resolve("blocks.json");
                    try {
                        Files.createDirectories(atlasPath.getParent());
                        GsonHelper.writeJsonFile(entry.atlas(), atlasPath);
                        if (!atlasToAdd.containsKey(directoryName)) {
                            Overlay overlay = new Overlay(new PackVersion(min), new PackVersion(max), directoryName);
                            atlasToAdd.put(directoryName, overlay.getAsOverlayEntry(true));
                        }
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to write atlas " + atlasPath.toAbsolutePath(), e);
                    }
                }
            }

            JsonObject overlaysJson = packMetaJson.getAsJsonObject("overlays");
            if (overlaysJson == null) {
                overlaysJson = new JsonObject();
                packMetaJson.add("overlays", overlaysJson);
            }
            JsonArray overlayEntries = overlaysJson.getAsJsonArray("entries");
            if (overlayEntries == null) {
                overlayEntries = new JsonArray();
                overlaysJson.add("entries", overlayEntries);
            }
            for (JsonElement entry : atlasToAdd.values()) {
                overlayEntries.add(entry);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private ValidationResult validateOverlayedResourcePack(Path[] rootPaths, boolean above1_21_11, Set<Path> fixedModels) {
        Multimap<Key, Key> glyphToFonts = HashMultimap.create(128, 32); // 图片到字体的映射
        Multimap<Key, Key> modelToItemDefinitions = HashMultimap.create(128, 4); // 模型到物品的映射
        Multimap<Key, String> modelToBlockStates = HashMultimap.create(128, 32); // 模型到方块的映射
        Multimap<Key, Key> textureToModels = HashMultimap.create(128, 8); // 纹理到模型的映射
        Multimap<Key, Key> textureToEquipments = HashMultimap.create(128, 8); // 纹理到盔甲的映射
        Multimap<Key, Key> oggToSoundEvents = HashMultimap.create(128, 4); // 音频到声音的映射

        JsonObject lastBlocksAtlas = null;
        JsonObject lastItemAtlas = null;

        // 如果需要验证资源包，则需要先读取所有atlas
        for (Path rootPath : rootPaths) {
            Path blockAtlasFile = rootPath
                    .resolve("assets")
                    .resolve("minecraft")
                    .resolve("atlases")
                    .resolve("blocks.json");
            if (Files.exists(blockAtlasFile)) {
                try {
                    lastBlocksAtlas = GsonHelper.readJsonFile(blockAtlasFile).getAsJsonObject();
                } catch (IOException | JsonParseException e) {
                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", blockAtlasFile.toAbsolutePath().toString());
                }
            }
            if (above1_21_11) {
                Path itemAtlasFile = rootPath
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("atlases")
                        .resolve("items.json");
                if (Files.exists(itemAtlasFile)) {
                    try {
                        lastItemAtlas = GsonHelper.readJsonFile(itemAtlasFile).getAsJsonObject();
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", itemAtlasFile.toAbsolutePath().toString());
                    }
                }
            }
        }

        /*


        构建Atlas文件，
        验证只使用默认的atlas，否则整个过程将会变成非常复杂


         */
        Atlas blockAtlas;
        Atlas itemAtlas;
        if (above1_21_11) {
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
                                JsonObject fontJson;
                                try {
                                    fontJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
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
                                        Key resourceLocation = Key.of(FileUtils.pathWithoutExtension(pngFile));
                                        glyphToFonts.put(resourceLocation, fontName);
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
                                JsonObject itemJson;
                                try {
                                    itemJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                Key item = Key.of(namespace, FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                collectItemModelsDeeply(itemJson, (resourceLocation) -> modelToItemDefinitions.put(resourceLocation, item));
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
                                JsonObject blockStateJson;
                                try {
                                    blockStateJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
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
                                JsonObject equipmentJson;
                                try {
                                    equipmentJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                String equipmentId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                                if (equipmentJson.has("layers")) {
                                    for (Map.Entry<String, JsonElement> layer : equipmentJson.getAsJsonObject("layers").entrySet()) {
                                        String type = layer.getKey();
                                        if (layer.getValue() instanceof JsonArray equipmentLayer) {
                                            for (JsonElement lay : equipmentLayer) {
                                                if (lay instanceof JsonObject layObj) {
                                                    if (layObj.get("texture") instanceof JsonPrimitive layerTexture) {
                                                        Key rawTexture = Key.of(layerTexture.getAsString());
                                                        Key fullPath = Key.of(rawTexture.namespace(), "entity/equipment/" + type + "/" + rawTexture.value());
                                                        textureToEquipments.put(fullPath, Key.of(namespace, equipmentId));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        this.plugin.logger().warn("Failed to walk through equipments", e);
                    }
                }

                // 声音文件
                Path soundsPath = namespacePath.resolve("sounds.json");
                if (Files.exists(soundsPath)) {
                    try {
                        JsonObject soundsJson = GsonHelper.readJsonFile(soundsPath).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> soundEventEntry : soundsJson.entrySet()) {
                            Key soundKey = Key.of(namespace, soundEventEntry.getKey());
                            if (soundEventEntry.getValue() instanceof JsonObject soundEventObj) {
                                JsonArray soundArray = soundEventObj.getAsJsonArray("sounds");
                                if (soundArray != null) {
                                    for (JsonElement sound : soundArray) {
                                        if (sound instanceof JsonPrimitive primitive) {
                                            oggToSoundEvents.put(Key.of(primitive.getAsString()), soundKey);
                                        } else if (sound instanceof JsonObject soundObj && soundObj.has("name")) {
                                            if (soundObj.has("type")) {
                                                String type = soundObj.get("type").getAsString();
                                                if (!type.equals("file")) continue;
                                            }
                                            String name = soundObj.get("name").getAsString();
                                            oggToSoundEvents.put(Key.of(name), soundKey);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException | JsonParseException e) {
                        this.plugin.logger().warn("Failed to visit sounds.json", e);
                    }
                }
            }
        }

        // 验证font的贴图是否存在
        label: for (Map.Entry<Key, Collection<Key>> entry : glyphToFonts.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_TEXTURES.contains(key)) continue;
            String imagePath = "assets/" + key.namespace() + "/textures/" + key.value() + ".png";
            for (Path rootPath : rootPaths) {
                if (Files.exists(rootPath.resolve(imagePath))) {
                    continue label;
                }
            }
            TranslationManager.instance().log("warning.config.resource_pack.generation.missing_font_texture", entry.getValue().stream().distinct().toList().toString(), imagePath);
        }

        // 验证equipment的贴图是否存在
        label: for (Map.Entry<Key, Collection<Key>> entry : textureToEquipments.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_TEXTURES.contains(key)) continue;
            String imagePath = "assets/" + key.namespace() + "/textures/" + key.value() + ".png";
            for (Path rootPath : rootPaths) {
                if (Files.exists(rootPath.resolve(imagePath))) {
                    continue label;
                }
            }
            TranslationManager.instance().log("warning.config.resource_pack.generation.missing_equipment_texture", entry.getValue().stream().distinct().toList().toString(), imagePath);
        }

        // 验证sounds的ogg文件是否存在
        label: for (Map.Entry<Key, Collection<Key>> entry : oggToSoundEvents.asMap().entrySet()) {
            Key key = entry.getKey();
            if (VANILLA_SOUNDS.contains(key)) continue;
            String oggPath = "assets/" + key.namespace() + "/sounds/" + key.value() + ".ogg";
            for (Path rootPath : rootPaths) {
                if (Files.exists(rootPath.resolve(oggPath))) {
                    continue label;
                }
            }
            TranslationManager.instance().log("warning.config.resource_pack.generation.missing_sound", entry.getValue().stream().distinct().toList().toString(), oggPath);
        }

        // 获取所有能直接从资源包里捕获到的方块和物品模型
        Map<Key, TexturedModel> blockModels = new HashMap<>(256);
        Map<Key, TexturedModel> itemModels = new HashMap<>(256);
        // 获取全部minecraft模型
        Map<Key, TexturedModel> modelsCache = new HashMap<>(256);

        // 收集全部方块状态的模型贴图
        label: for (Map.Entry<Key, Collection<String>> entry : modelToBlockStates.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (modelsCache.containsKey(modelPath)) continue;
            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            for (Path rootPath : rootPaths) {
                Path modelJsonPath = rootPath.resolve(modelStringPath);
                if (Files.exists(modelJsonPath)) {
                    JsonObject modelJson;
                    try {
                        modelJson = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                        continue;
                    }
                    TexturedModel texturedModel = getTexturedModel(modelPath, TexturedModel.getParent(modelJson), TexturedModel.getTextures(modelJson), rootPaths, modelsCache);
                    blockModels.put(modelPath, texturedModel);
                    if (Config.fixMissingTexture() && fixedModels.add(modelJsonPath)) {
                        if (modelJson.get("elements") instanceof JsonArray elements) {
                            boolean changed = false;
                            for (JsonElement e : elements) {
                                if (e instanceof JsonObject element) {
                                    if (element.get("faces") instanceof JsonObject faces) {
                                        for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                                            if (faceEntry.getValue() instanceof JsonObject face) {
                                                if (face.get("texture") instanceof JsonPrimitive texturePrimitive) {
                                                    String texture = texturePrimitive.getAsString();
                                                    if (texture.equals("#missing")) {
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
                                }
                            }
                            if (changed) {
                                try {
                                    GsonHelper.writeJsonFile(modelJson, modelJsonPath);
                                } catch (IOException e) {
                                    this.plugin.logger().warn("Failed to write JSON file", e);
                                }
                            }
                        }
                    }
                    continue label;
                }
            }
            // 提示方块状态缺少模型
            if (!VANILLA_MODELS.contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_block_model", entry.getValue().toString(), modelStringPath);
            }
        }

        // 收集全部物品定义的模型贴图
        label: for (Map.Entry<Key, Collection<Key>> entry : modelToItemDefinitions.asMap().entrySet()) {
            Key modelPath = entry.getKey();
            if (modelsCache.containsKey(modelPath)) continue;
            String modelStringPath = "assets/" + modelPath.namespace() + "/models/" + modelPath.value() + ".json";
            for (Path rootPath : rootPaths) {
                Path modelJsonPath = rootPath.resolve(modelStringPath);
                if (Files.exists(rootPath.resolve(modelStringPath))) {
                    JsonObject modelJson;
                    try {
                        modelJson = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                        continue;
                    }
                    TexturedModel texturedModel = getTexturedModel(modelPath, TexturedModel.getParent(modelJson), TexturedModel.getTextures(modelJson), rootPaths, modelsCache);
                    itemModels.put(modelPath, texturedModel);
                    if (Config.fixMissingTexture() && fixedModels.add(modelJsonPath)) {
                        if (modelJson.get("elements") instanceof JsonArray elements) {
                            boolean changed = false;
                            for (JsonElement e : elements) {
                                if (e instanceof JsonObject element) {
                                    if (element.get("faces") instanceof JsonObject faces) {
                                        for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                                            if (faceEntry.getValue() instanceof JsonObject face) {
                                                if (face.get("texture") instanceof JsonPrimitive texturePrimitive) {
                                                    String texture = texturePrimitive.getAsString();
                                                    if (texture.equals("#missing")) {
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
                                }
                            }
                            if (changed) {
                                try {
                                    GsonHelper.writeJsonFile(modelJson, modelJsonPath);
                                } catch (IOException e) {
                                    this.plugin.logger().warn("Failed to write JSON file", e);
                                }
                            }
                        }
                    }
                    continue label;
                }
            }
            if (!VANILLA_MODELS.contains(modelPath)) {
                modelsCache.put(modelPath, TexturedModel.EMPTY);
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_item_model", entry.getValue().toString(), modelStringPath);
            }
        }

        ValidationResult result = null;

        if (!Config.enableObfuscation()) {

            if (above1_21_11) {

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
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png");
                                    } else {
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.bad_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/blocks.png", "minecraft:textures/atlas/items.png");
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.texture_not_in_atlas",
                                                spritePath.asString());
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
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.duplicated_sprite",
                                                entry.getKey().asString(), spritePath.asString(),
                                                "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png");
                                    } else if (finalHasBlockAtlas) {
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.multiple_atlases",
                                                entry.getKey().asString(), "minecraft:textures/atlas/items.png", "minecraft:textures/atlas/blocks.png");
                                    }
                                } else {
                                    if (!status.inBlockAtlas() && notInAtlasTextures.add(spritePath)) {
                                        TranslationManager.instance().log("warning.config.resource_pack.generation.texture_not_in_atlas",
                                                spritePath.asString());
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
                        TranslationManager.instance().log("warning.config.resource_pack.generation.texture_not_in_atlas",
                                spritePath.asString());
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
                outer: {
                    for (Path rootPath : rootPaths) {
                        if (Files.exists(rootPath.resolve(texturePath))) {
                            break outer;
                        }
                    }
                    TranslationManager.instance().log("warning.config.resource_pack.generation.missing_model_texture", entry.getValue().toString(), texturePath);
                }
            }
        }

        if (result == null) {
            result = new ValidationResult(null, lastItemAtlas, null, lastBlocksAtlas);
        }

        // todo 验证 unstitch 和 paletted permutations
        return result;
    }

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
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_parent_model", currentRL.asString(), parentModelStringPath);
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
                JsonObject parentModelJson;
                try {
                    parentModelJson = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                } catch (IOException | JsonParseException e) {
                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                    continue;
                }
                return parentModelJson;
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
            this.plugin.logger().severe("Error creating directories", e);
            return;
        }
        try {
            GsonHelper.writeJsonFile(particleJson, jsonPath);
            Files.write(pngPath, EMPTY_1X1_IMAGE);
        } catch (IOException e) {
            this.plugin.logger().severe("Error writing particles file", e);
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
                    previousAtlasSources = GsonHelper.readJsonFile(atlasPath).getAsJsonObject().getAsJsonArray("sources");
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
            // 创建atlas文件夹
            try {
                Files.createDirectories(atlasPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().severe("Error creating " + atlasPath.toAbsolutePath(), e);
                return;
            }
            // 写入atlas文件
            try (BufferedWriter writer = Files.newBufferedWriter(atlasPath)) {
                JsonObject selected = needLegacyCompatibility ? legacyTrimAtlasJson : modernTrimAtlasJson;
                // 优先写入旧版
                GsonHelper.get().toJson(selected, writer);
            } catch (IOException e) {
                this.plugin.logger().severe("Error writing " + atlasPath.toAbsolutePath(), e);
            }
            // 既要又要，那么需要overlay
            if (needLegacyCompatibility && needModernCompatibility) {
                Revision revision = Revisions.SINCE_1_21_2;
                callback.accept(revision);
                Path overlayAtlasPath = generatedPackPath
                        .resolve(Config.createOverlayFolderName(revision.versionString()))
                        .resolve("assets")
                        .resolve("minecraft")
                        .resolve("atlases")
                        .resolve("armor_trims.json");
                // 创建atlas文件夹
                try {
                    Files.createDirectories(overlayAtlasPath.getParent());
                } catch (IOException e) {
                    this.plugin.logger().severe("Error creating " + overlayAtlasPath.toAbsolutePath(), e);
                    return;
                }
                // 写入atlas文件
                try (BufferedWriter writer = Files.newBufferedWriter(overlayAtlasPath)) {
                    GsonHelper.get().toJson(modernTrimAtlasJson, writer);
                    callback.accept(revision);
                } catch (IOException e) {
                    this.plugin.logger().severe("Error writing " + overlayAtlasPath.toAbsolutePath(), e);
                }
            }
        }
    }

    private void processComponentBasedEquipment(ComponentBasedEquipment componentBasedEquipment, Path generatedPackPath) {
        Key assetId = componentBasedEquipment.assetId();
        if (assetId == null) {
            this.plugin.logger().severe("Asset id is null for equipment " + componentBasedEquipment);
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
                        plugin.logger().severe("Error creating " + correctPath.toAbsolutePath());
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
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load existing sounds.json", e);
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
                plugin.logger().severe("Error creating " + equipmentPath.toAbsolutePath());
                return;
            }
            try {
                GsonHelper.writeJsonFile(equipmentJson, equipmentPath);
            } catch (IOException e) {
                this.plugin.logger().severe("Error writing equipment file", e);
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
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load existing sounds.json", e);
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
                plugin.logger().severe("Error creating " + equipmentPath.toAbsolutePath());
                return;
            }
            try {
                GsonHelper.writeJsonFile(equipmentJson, equipmentPath);
            } catch (IOException e) {
                this.plugin.logger().severe("Error writing equipment file", e);
            }
        }
    }

    @Nullable
    private Pair<Boolean, Boolean> processTrimBasedEquipment(TrimBasedEquipment trimBasedEquipment, Path generatedPackPath) {
        Key assetId = trimBasedEquipment.assetId();

        Key humanoidResourceLocation = trimBasedEquipment.humanoid();
        boolean hasLayer1 = humanoidResourceLocation != null;
        Key humanoidLeggingsResourceLocation = trimBasedEquipment.humanoidLeggings();
        boolean hasLayer2 = humanoidLeggingsResourceLocation != null;

        if (hasLayer1) {
            Path texture = generatedPackPath
                    .resolve("assets")
                    .resolve(humanoidResourceLocation.namespace())
                    .resolve("textures")
                    .resolve(humanoidResourceLocation.value() + ".png");
            if (!Files.exists(texture) || !Files.isRegularFile(texture)) {
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_equipment_texture", assetId.asString(), texture.toString());
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
                        plugin.logger().severe("Error writing armor texture file from " + texture + " to " + legacyTarget, e);
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
                        plugin.logger().severe("Error writing armor texture file from " + texture + " to " + modernTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (!shouldPreserve) {
                try {
                    Files.delete(texture);
                } catch (IOException e) {
                    this.plugin.logger().severe("Error deleting armor texture file from " + texture, e);
                }
            }
        }
        if (hasLayer2) {
            Path texture = generatedPackPath
                    .resolve("assets")
                    .resolve(humanoidLeggingsResourceLocation.namespace())
                    .resolve("textures")
                    .resolve(humanoidLeggingsResourceLocation.value() + ".png");
            if (!Files.exists(texture) && !Files.isRegularFile(texture)) {
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_equipment_texture", assetId.asString(), texture.toString());
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
                        this.plugin.logger().severe("Error writing armor texture file from " + texture + " to " + legacyTarget, e);
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
                        this.plugin.logger().severe("Error writing armor texture file from " + texture + " to " + modernTarget, e);
                    }
                } else {
                    shouldPreserve = true;
                }
            }
            if (!shouldPreserve) {
                try {
                    Files.delete(texture);
                } catch (IOException e) {
                    this.plugin.logger().severe("Error deleting armor texture file from " + texture, e);
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
            this.plugin.logger().severe("Error writing empty block model", e);
        }
        try {
            Files.createDirectories(texturePath.getParent());
            Files.write(texturePath, EMPTY_16X16_IMAGE);
        } catch (IOException e) {
            this.plugin.logger().severe("Error writing empty block texture", e);
        }
    }

    private void generateClientLang(Path generatedPackPath) {
        for (Map.Entry<String, LangData> entry : this.plugin.translationManager().clientLangData().entrySet()) {
            Path langPath = generatedPackPath
                    .resolve("assets")
                    .resolve("minecraft")
                    .resolve("lang")
                    .resolve(entry.getKey() + ".json");
            JsonObject json;
            if (Files.exists(langPath)) {
                try {
                    json = GsonHelper.readJsonFile(langPath).getAsJsonObject();
                } catch (Exception e) {
                    json = new JsonObject();
                }
            } else {
                json = new JsonObject();
            }
            for (Map.Entry<String, String> pair : entry.getValue().translations.entrySet()) {
                json.addProperty(pair.getKey(), pair.getValue());
            }
            try {
                Files.createDirectories(langPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + langPath.toAbsolutePath());
                return;
            }
            try {
                GsonHelper.writeJsonFile(json, langPath);
            } catch (IOException e) {
                this.plugin.logger().severe("Error writing language file", e);
            }
        }
    }

    private void generateCustomSounds(Path generatedPackPath) {
        AbstractSoundManager soundManager = (AbstractSoundManager) plugin.soundManager();
        for (Map.Entry<String, List<SoundEvent>> entry : soundManager.soundsByNamespace().entrySet()) {
            Path soundPath = generatedPackPath
                    .resolve("assets")
                    .resolve(entry.getKey())
                    .resolve("sounds.json");
            JsonObject soundJson;
            if (Files.exists(soundPath)) {
                try (BufferedReader reader = Files.newBufferedReader(soundPath)) {
                    soundJson = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load existing sounds.json", e);
                    return;
                }
            } else {
                soundJson = new JsonObject();
            }
            for (SoundEvent soundEvent : entry.getValue()) {
                soundJson.add(soundEvent.id().value(), soundEvent.get());
            }
            try {
                Files.createDirectories(soundPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + soundPath.toAbsolutePath());
                return;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(soundPath)) {
                GsonHelper.get().toJson(soundJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to generate sounds.json: " + soundPath.toAbsolutePath(), e);
            }
        }
    }

    private void generateOverrideSounds(Path generatedPackPath) {
        if (!Config.enableSoundSystem()) return;

        Path soundPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("sounds.json");

        JsonObject soundTemplate;
        try (InputStream inputStream = plugin.resourceStream("internal/sounds.json")) {
            if (inputStream == null) {
                plugin.logger().warn("Failed to load internal/sounds.json");
                return;
            }
            soundTemplate = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            plugin.logger().warn("Failed to load internal/sounds.json", e);
            return;
        }

        JsonObject soundJson;
        if (Files.exists(soundPath)) {
            try (BufferedReader reader = Files.newBufferedReader(soundPath)) {
                soundJson = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                plugin.logger().warn("Failed to load existing sounds.json", e);
                return;
            }
        } else {
            soundJson = new JsonObject();
        }

        for (Map.Entry<Key, Key> mapper : plugin.blockManager().soundReplacements().entrySet()) {
            Key originalKey = mapper.getKey();
            JsonObject empty = new JsonObject();
            empty.add("sounds", new JsonArray());
            empty.addProperty("replace", true);
            soundJson.add(originalKey.value(), empty);
            JsonObject originalSounds = soundTemplate.getAsJsonObject(originalKey.value());
            if (originalSounds != null) {
                soundJson.add(mapper.getValue().value(), originalSounds);
            } else {
                plugin.logger().warn("Cannot find " + originalKey.value() + " in sound template");
            }
        }
        try {
            Files.createDirectories(soundPath.getParent());
        } catch (IOException e) {
            plugin.logger().severe("Error creating " + soundPath.toAbsolutePath());
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(soundPath)) {
            GsonHelper.get().toJson(soundJson, writer);
        } catch (IOException e) {
            plugin.logger().warn("Failed to generate sounds.json: " + soundPath.toAbsolutePath(), e);
        }
    }

    private void generateItemModels(Path generatedPackPath, ModelGenerator generator) {
        for (ModelGeneration generation : generator.modelsToGenerate()) {
            Path modelPath = generatedPackPath
                    .resolve("assets")
                    .resolve(generation.path().namespace())
                    .resolve("models")
                    .resolve(generation.path().value() + ".json");
            if (Files.exists(modelPath)) {
                TranslationManager.instance().log("warning.config.resource_pack.model.generation.already_exist", modelPath.toAbsolutePath().toString());
                continue;
            }
            try {
                Files.createDirectories(modelPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + modelPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(modelPath)) {
                GsonHelper.get().toJson(generation.get(), writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to generate model: " + modelPath.toAbsolutePath(), e);
            }
        }
    }

    private void generateBlockOverrides(Path generatedPackPath) {
        for (Map.Entry<Key, Map<String, JsonElement>> entry : plugin.blockManager().blockOverrides().entrySet()) {
            Key key = entry.getKey();
            Path overridedBlockPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("blockstates")
                    .resolve(key.value() + ".json");
            JsonObject stateJson;
            if (Files.exists(overridedBlockPath)) {
                try {
                    stateJson = GsonHelper.readJsonFile(overridedBlockPath).getAsJsonObject();
                    if (!stateJson.has("variants")) {
                        stateJson = new JsonObject();
                    }
                } catch (IOException e) {
                    stateJson = new JsonObject();
                }
            } else {
                stateJson = new JsonObject();
            }
            JsonObject variants;
            if (!stateJson.has("variants")) {
                variants = new JsonObject();
                stateJson.add("variants", variants);
            } else {
                variants = stateJson.get("variants").getAsJsonObject();
            }
            for (Map.Entry<String, JsonElement> resourcePathEntry : entry.getValue().entrySet()) {
                variants.add(resourcePathEntry.getKey(), resourcePathEntry.getValue());
            }
            try {
                Files.createDirectories(overridedBlockPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + overridedBlockPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedBlockPath)) {
                GsonHelper.get().toJson(stateJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to create block states for " + key, e);
            }
        }

        if (!Config.generateModAssets()) return;
        for (Map.Entry<Key, JsonElement> entry : plugin.blockManager().modBlockStates().entrySet()) {
            Key key = entry.getKey();
            Path overridedBlockPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("blockstates")
                    .resolve(key.value() + ".json");
            JsonObject stateJson = new JsonObject();
            JsonObject variants = new JsonObject();
            stateJson.add("variants", variants);
            variants.add("", entry.getValue());
            try {
                Files.createDirectories(overridedBlockPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + overridedBlockPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedBlockPath)) {
                GsonHelper.get().toJson(stateJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to create block states for " + key, e);
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
                    itemJson = GsonHelper.readJsonFile(itemPath).getAsJsonObject();
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
                } catch (IOException e) {
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

                itemJson.addProperty("parent", firstBaseModel.model());
                if (!overrideJsons.isEmpty()) {
                    JsonArray overrides = new JsonArray();
                    for (JsonObject override : overrideJsons) {
                        overrides.add(override);
                    }
                    itemJson.add("overrides", overrides);
                }
            }
            try {
                Files.createDirectories(itemPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + itemPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(itemPath)) {
                GsonHelper.get().toJson(itemJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for " + itemModelPath, e);
            }
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
                TranslationManager.instance().log("warning.config.resource_pack.item_model.already_exist", key.asString(), itemPath.toAbsolutePath().toString());
                continue;
            }
            try {
                Files.createDirectories(itemPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().severe("Error creating " + itemPath.toAbsolutePath(), e);
                continue;
            }
            ModernItemModel modernItemModel = entry.getValue();
            try (BufferedWriter writer = Files.newBufferedWriter(itemPath)) {
                GsonHelper.get().toJson(modernItemModel.toJson(Config.packMinVersion()), writer);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to save item model for " + key, e);
            }

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
                        try {
                            Files.createDirectories(overlayItemPath.getParent());
                        } catch (IOException e) {
                            this.plugin.logger().severe("Error creating " + overlayItemPath.toAbsolutePath(), e);
                            continue;
                        }
                        try (BufferedWriter writer = Files.newBufferedWriter(overlayItemPath)) {
                            GsonHelper.get().toJson(modernItemModel.toJson(revision.minVersion()), writer);
                            callback.accept(revision);
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to save item model for " + key, e);
                        }
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
                    originalItemModel = ModernItemModel.fromJson(GsonHelper.readJsonFile(overridedItemPath).getAsJsonObject());
                } catch (IOException e) {
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
                entries.put(modelWithDataEntry.getKey().floatValue(), modernItemModel.itemModel());
                if (modernItemModel.handAnimationOnSwap()) {
                    handAnimationOnSwap = true;
                }
                if (modernItemModel.oversizedInGui()) {
                    oversizedInGui = true;
                }
            }

            RangeDispatchItemModel rangeDispatch = new RangeDispatchItemModel(
                new CustomModelDataRangeDispatchProperty(0),
                1f,
                    originalItemModel.itemModel(),
                    entries
            );

            ModernItemModel newItemModel = new ModernItemModel(rangeDispatch, handAnimationOnSwap, oversizedInGui, swapAnimationScale);
            try {
                Files.createDirectories(overridedItemPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().severe("Error creating " + overridedItemPath.toAbsolutePath(), e);
                continue;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(overridedItemPath)) {
                GsonHelper.get().toJson(newItemModel.toJson(Config.packMinVersion()), writer);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to save item model for " + vanillaItemModel, e);
            }

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
                        try {
                            Files.createDirectories(overlayItemPath.getParent());
                        } catch (IOException e) {
                            this.plugin.logger().severe("Error creating " + overlayItemPath.toAbsolutePath(), e);
                            continue;
                        }
                        try (BufferedWriter writer = Files.newBufferedWriter(overlayItemPath)) {
                            GsonHelper.get().toJson(newItemModel.toJson(revision.minVersion()), writer);
                            callback.accept(revision);
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to save item model for " + vanillaItemModel, e);
                        }
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
                } catch (IOException e) {
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
            try {
                Files.createDirectories(overridedItemPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + overridedItemPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedItemPath)) {
                GsonHelper.get().toJson(originalItemModel, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for " + vanillaLegacyModel, e);
            }
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
                } catch (IOException e) {
                    fontJson = new JsonObject();
                    this.plugin.logger().warn(fontPath + " is not a valid font json file");
                }
            } else {
                fontJson = new JsonObject();
                try {
                    Files.createDirectories(fontPath.getParent());
                } catch (IOException e) {
                    this.plugin.logger().severe("Error creating " + fontPath.toAbsolutePath(), e);
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
                this.plugin.logger().severe("Error writing font to " + fontPath.toAbsolutePath(), e);
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

    @Override
    public ConfigParser parser() {
        return this.parser;
    }

    public static class SkipOptimizationParser extends SectionConfigParser {
        private static final String[] SECTION_ID = new String[] {"skip-optimization"};
        private final Set<String> excludeTexture = new HashSet<>();
        private final Set<String> excludeJson = new HashSet<>();

        public SkipOptimizationParser() {
        }

        public void clearCache() {
            this.excludeTexture.clear();
            this.excludeJson.clear();
        }

        @Override
        public int count() {
            return this.excludeJson.size() + this.excludeTexture.size();
        }

        public Set<String> excludeTexture() {
            return excludeTexture;
        }

        public Set<String> excludeJson() {
            return excludeJson;
        }

        @Override
        protected void parseSection(Pack pack, Path path, Map<String, Object> section) throws LocalizedException {
            if (!Config.optimizeResourcePack()) return;
            List<String> textures = MiscUtils.getAsStringList(section.get("texture"));
            if (!textures.isEmpty()) {
                for (String texture : textures) {
                    if (texture.endsWith(".png") || texture.endsWith("/")) {
                        this.excludeTexture.add(texture);
                    } else {
                        this.excludeTexture.add(texture + ".png");
                    }
                }
            }
            List<String> jsons = MiscUtils.getAsStringList(section.get("json"));
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

        @Override
        public int loadingSequence() {
            return LoadingSequence.SKIP_OPTIMIZATION;
        }
    }
}
