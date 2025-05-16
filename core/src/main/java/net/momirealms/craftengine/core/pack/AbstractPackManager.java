package net.momirealms.craftengine.core.pack;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.EquipmentData;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionConditional;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.pack.host.impl.NoneHost;
import net.momirealms.craftengine.core.pack.misc.EquipmentGeneration;
import net.momirealms.craftengine.core.pack.model.ItemModel;
import net.momirealms.craftengine.core.pack.model.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.pack.obfuscation.ObfA;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.I18NData;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

public abstract class AbstractPackManager implements PackManager {
    public static final Map<Key, JsonObject> PRESET_MODERN_MODELS_ITEM = new HashMap<>();
    public static final Map<Key, JsonObject> PRESET_LEGACY_MODELS_ITEM = new HashMap<>();
    public static final Map<Key, JsonObject> PRESET_MODELS_BLOCK = new HashMap<>();
    public static final Map<Key, JsonObject> PRESET_ITEMS = new HashMap<>();
    public static final Set<Key> VANILLA_ITEM_TEXTURES = new HashSet<>();
    public static final Set<Key> VANILLA_BLOCK_TEXTURES = new HashSet<>();
    public static final Set<Key> VANILLA_FONT_TEXTURES = new HashSet<>();
    private static final byte[] EMPTY_IMAGE;
    static {
        var stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        EMPTY_IMAGE = stream.toByteArray();
    }

    private final CraftEngine plugin;
    private final BiConsumer<Path, Path> eventDispatcher;
    private final Map<String, Pack> loadedPacks = new HashMap<>();
    private final Map<String, ConfigParser> sectionParsers = new HashMap<>();
    private Map<Path, CachedConfigFile> cachedConfigFiles = Collections.emptyMap();
    private Map<Path, CachedAssetFile> cachedAssetFiles = Collections.emptyMap();
    protected BiConsumer<Path, Path> zipGenerator;
    protected ResourcePackHost resourcePackHost;

    public AbstractPackManager(CraftEngine plugin, BiConsumer<Path, Path> eventDispatcher) {
        this.plugin = plugin;
        this.eventDispatcher = eventDispatcher;
        this.zipGenerator = (p1, p2) -> {};
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
    }

    private void initInternalData() {
        loadInternalData("internal/models/item/legacy/_all.json", PRESET_LEGACY_MODELS_ITEM::put);
        loadInternalData("internal/models/item/modern/_all.json", PRESET_MODERN_MODELS_ITEM::put);
        loadInternalData("internal/models/block/_all.json", PRESET_MODELS_BLOCK::put);
        loadInternalData("internal/items/_all.json", PRESET_ITEMS::put);

        loadInternalList("internal/textures/block/_list.json", VANILLA_BLOCK_TEXTURES::add);
        loadInternalList("internal/textures/item/_list.json", VANILLA_ITEM_TEXTURES::add);
        loadInternalList("internal/textures/font/_list.json", VANILLA_FONT_TEXTURES::add);
    }

    private void loadInternalData(String path, BiConsumer<Key, JsonObject> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                JsonObject allModelsItems = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
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
                JsonObject listJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                JsonArray list = listJson.getAsJsonArray("files");
                for (JsonElement element : list) {
                    if (element instanceof JsonPrimitive primitive) {
                        callback.accept(Key.of(FileUtils.pathWithoutExtension(primitive.getAsString())));
                    }
                }
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load " + path, e);
        }
    }

    @Override
    public Path resourcePackPath() {
        return this.plugin.dataFolderPath()
                .resolve("generated")
                .resolve("resource_pack.zip");
    }

    @Override
    public void load() {
        List<Map<?, ?>> list = Config.instance().settings().getMapList("resource-pack.delivery.hosting");
        if (list == null || list.isEmpty()) {
            this.resourcePackHost = NoneHost.INSTANCE;
        } else {
            try {
                // we might add multiple host methods in future versions
                this.resourcePackHost = ResourcePackHosts.fromMap(MiscUtils.castToMap(list.get(0), false));
            } catch (LocalizedException e) {
                if (e instanceof LocalizedResourceConfigException exception) {
                    exception.setPath(plugin.dataFolderPath().resolve("config.yml"));
                    e.setArgument(1, "hosting");
                }
                TranslationManager.instance().log(e.node(), e.arguments());
                this.resourcePackHost = NoneHost.INSTANCE;
            }
        }
    }

    @Override
    public ResourcePackHost resourcePackHost() {
        return this.resourcePackHost;
    }

    @Override
    public void loadResources(boolean recipe) {
        this.loadPacks();
        this.loadResourceConfigs(recipe ? (p) -> true : (p) -> p.loadingSequence() != LoadingSequence.RECIPE);
    }

    @Override
    public void unload() {
        this.loadedPacks.clear();
    }

    @Override
    public void delayedInit() {
       try {
           Class<?> magicClazz = ReflectionUtils.getClazz(getClass().getSuperclass().getPackageName() + new String(Base64Utils.decode(ObfA.VALUES, Integer.parseInt(String.valueOf(ObfA.VALUES[71]).substring(0,1))), StandardCharsets.UTF_8));
           if (magicClazz != null) {
               int fileCount = ObfA.VALUES[1] - ObfA.VALUES[17];
               Constructor<?> magicConstructor = ReflectionUtils.getConstructor(magicClazz, fileCount);
               assert magicConstructor != null;
//               magicConstructor.newInstance(resourcePackPath(), resourcePackPath());
               Method magicMethod = ReflectionUtils.getMethod(magicClazz, void.class);
               assert magicMethod != null;
               this.zipGenerator = (p1, p2) -> {
                   try {
                       Object magicObject = magicConstructor.newInstance(p1, p2);
                       magicMethod.invoke(magicObject);
                   } catch (Exception e) {
                       this.plugin.logger().warn("Failed to generate zip files", e);
                   }
               };
           } else {
               this.plugin.logger().warn("Magic class doesn't exist");
           }
       } catch (Exception e) {
           this.plugin.logger().warn("Failed to initialize pack manager", e);
       }
    }

    @Override
    public void initCachedAssets() {
        try {
            this.updateCachedAssets(null);
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
        return true;
    }

    @Override
    public boolean unregisterConfigSectionParser(String id) {
        if (!this.sectionParsers.containsKey(id)) return false;
        this.sectionParsers.remove(id);
        return true;
    }

    private void loadPacks() {
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
                    Path metaFile = path.resolve("pack.yml");
                    String namespace = path.getFileName().toString();
                    String description = null;
                    String version = null;
                    String author = null;
                    boolean enable = true;
                    if (Files.exists(metaFile) && Files.isRegularFile(metaFile)) {
                        YamlDocument metaYML = Config.instance().loadYamlData(metaFile.toFile());
                        enable = metaYML.getBoolean("enable", true);
                        namespace = metaYML.getString("namespace", namespace);
                        description = metaYML.getString("description");
                        version = metaYML.getString("version");
                        author = metaYML.getString("author");
                    }
                    Pack pack = new Pack(path, new PackMeta(author, description, version, namespace), enable);
                    this.loadedPacks.put(path.getFileName().toString(), pack);
                    this.plugin.logger().info("Loaded pack: " + pack.folder().getFileName() + ". Default namespace: " + namespace);
                }
            }
        } catch (IOException e) {
            this.plugin.logger().severe("Error loading packs", e);
        }
    }

    private void saveDefaultConfigs() {
        // internal
        plugin.saveResource("resources/remove_shulker_head/resourcepack/pack.mcmeta");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/assets/minecraft/shaders/core/rendertype_entity_solid.fsh");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/1_20_5_assets/minecraft/shaders/core/rendertype_entity_solid.fsh");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/assets/minecraft/textures/entity/shulker/shulker_white.png");
        plugin.saveResource("resources/remove_shulker_head/pack.yml");
        // internal
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/models/block/default_chorus_plant.json");
        plugin.saveResource("resources/internal/pack.yml");
        // i18n
        plugin.saveResource("resources/internal/configuration/i18n.yml");
        // offset
        plugin.saveResource("resources/internal/configuration/offset_chars.yml");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/offset/space_split.png");
        // gui
        plugin.saveResource("resources/internal/configuration/gui.yml");
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
        // default pack
        plugin.saveResource("resources/default/pack.yml");
        // pack meta
        plugin.saveResource("resources/default/resourcepack/pack.mcmeta");
        plugin.saveResource("resources/default/resourcepack/pack.png");
        // templates
        plugin.saveResource("resources/default/configuration/templates.yml");
        // emoji
        plugin.saveResource("resources/default/configuration/emoji.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/font/image/emojis.png");
        // i18n
        plugin.saveResource("resources/default/configuration/i18n.yml");
        // block_name
        plugin.saveResource("resources/default/configuration/block_name.yml");
        // categories
        plugin.saveResource("resources/default/configuration/categories.yml");
        // for mods
        plugin.saveResource("resources/default/configuration/fix_client_visual.yml");
        // icons
        plugin.saveResource("resources/default/configuration/icons.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/font/image/icons.png");
        // blocks
        plugin.saveResource("resources/default/configuration/blocks.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/netherite_anvil.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/netherite_anvil_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/solid_gunpowder_block.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/gunpowder_block.png");
        // items
        plugin.saveResource("resources/default/configuration/items.yml");
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

        // ores
        plugin.saveResource("resources/default/configuration/ores.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/deepslate_topaz_ore.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/deepslate_topaz_ore.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/topaz_ore.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/topaz_ore.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/topaz.png.mcmeta");
        // palm tree
        plugin.saveResource("resources/default/configuration/palm_tree.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_sapling.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_planks.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_log.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_log_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/stripped_palm_log.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/stripped_palm_log_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_leaves.png");
        // plants
        plugin.saveResource("resources/default/configuration/plants.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_3.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_4.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/reed.png");
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
        // furniture
        plugin.saveResource("resources/default/configuration/furniture.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/topaz_trident_in_hand.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/topaz_trident_throwing.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/table_lamp.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/wooden_chair.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/bench.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/table_lamp.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/wooden_chair.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/bench.png");
        // tooltip
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png.mcmeta");
    }

    private void updateCachedConfigFiles() {
        Map<Path, CachedConfigFile> previousFiles = this.cachedConfigFiles;
        this.cachedConfigFiles = new HashMap<>();
        for (Pack pack : loadedPacks()) {
            if (!pack.enabled()) continue;
            List<Path> files = FileUtils.getYmlConfigsDeeply(pack.configurationFolder());
            for (Path path : files) {
                CachedConfigFile cachedFile = previousFiles.get(path);
                try {
                    long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();
                    long size = Files.size(path);
                    if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                        this.cachedConfigFiles.put(path, cachedFile);
                        continue;
                    }
                    try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                        Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()));
                        Map<String, Object> data = yaml.load(inputStream);
                        if (data == null) continue;

                        this.cachedConfigFiles.put(path, new CachedConfigFile(data, pack, lastModifiedTime, size));
                    } catch (Exception e) {
                        this.plugin.logger().warn(path, "Error loading config file", e);
                    }
                } catch (IOException e) {
                    this.plugin.logger().warn(path, "Error reading last modified time", e);
                }
            }
        }
    }

    private void loadResourceConfigs(Predicate<ConfigParser> predicate) {
        TreeMap<ConfigParser, List<CachedConfigSection>> cachedConfigs = new TreeMap<>();
        long o1 = System.nanoTime();
        this.updateCachedConfigFiles();
        for (Map.Entry<Path, CachedConfigFile> fileEntry : this.cachedConfigFiles.entrySet()) {
            CachedConfigFile cachedFile = fileEntry.getValue();
            for (Map.Entry<String, Object> entry : cachedFile.config().entrySet()) {
                processConfigEntry(entry, fileEntry.getKey(), cachedFile.pack(), (p, c) ->
                        cachedConfigs.computeIfAbsent(p, k -> new ArrayList<>()).add(c));
            }
        }
        long o2 = System.nanoTime();
        this.plugin.logger().info("Loaded packs. Took " + String.format("%.2f", ((o2 - o1) / 1_000_000.0)) + " ms");
        for (Map.Entry<ConfigParser, List<CachedConfigSection>> entry : cachedConfigs.entrySet()) {
            ConfigParser parser = entry.getKey();
            long t1 = System.nanoTime();
            for (CachedConfigSection cached : entry.getValue()) {
                for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
                    String key = configEntry.getKey();
                    try {
                        Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
                        try {
                            if (parser.supportsParsingObject()) {
                                parser.parseObject(cached.pack(), cached.filePath(), id, configEntry.getValue());
                            } else if (predicate.test(parser)) {
                                if (configEntry.getValue() instanceof Map<?, ?> configSection0) {
                                    Map<String, Object> configSection1 = castToMap(configSection0, false);
                                    if ((boolean) configSection1.getOrDefault("enable", true)) {
                                        parser.parseSection(cached.pack(), cached.filePath(), id, plugin.templateManager().applyTemplates(configSection1));
                                    }
                                } else {
                                    TranslationManager.instance().log("warning.config.structure.not_section", cached.filePath().toString(), cached.prefix() + "." + key, configEntry.getValue().getClass().getSimpleName());
                                }
                            }
                        } catch (LocalizedException e) {
                            if (e instanceof LocalizedResourceConfigException exception) {
                                exception.setPath(cached.filePath());
                                exception.setId(cached.prefix() + "." + key);
                            }
                            TranslationManager.instance().log(e.node(), e.arguments());
                            this.plugin.debug(e::node);
                        }
                    } catch (Exception e) {
                        this.plugin.logger().warn("Unexpected error loading file " + cached.filePath() + " - '" + parser.sectionId()[0] + "." + key + "'. Please find the cause according to the stacktrace or seek developer help.", e);
                    }
                }
            }
            long t2 = System.nanoTime();
            this.plugin.logger().info("Loaded " + parser.sectionId()[0] + " in " + String.format("%.2f", ((t2 - t1) / 1_000_000.0)) + " ms");
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

//    private static void initFileSystemProvider() {
//        String osName = System.getProperty("os.name").toLowerCase();
//        String providerClass = null;
//        if (osName.contains("win")) {
//            providerClass = "sun.nio.fs.WindowsFileSystemProvider";
//        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
//            providerClass = "sun.nio.fs.LinuxFileSystemProvider";
//        } else if (osName.contains("mac")) {
//            providerClass = "sun.nio.fs.MacOSXFileSystemProvider";
//        }
//        if (providerClass != null) {
//            try {
//                System.setProperty("java.nio.file.spi.DefaultFileSystemProvider", providerClass);
//            } catch (Exception ignored) {}
//        }
//    }
//
//    private static void deleteDirectory(Path folder) throws IOException {
//        if (!Files.exists(folder)) return;
//        try (Stream<Path> walk = Files.walk(folder)) {
//            walk.sorted(Comparator.reverseOrder())
//                    .parallel()
//                    .forEach(path -> {
//                        try {
//                            Files.delete(path);
//                        } catch (IOException ignored) {}
//                    });
//        }
//    }

    private List<Pair<Path, List<Path>>> updateCachedAssets(@Nullable FileSystem fs) throws IOException {
        List<Path> folders = new ArrayList<>();
        Map<Path, List<Path>> conflictChecker = new HashMap<>(Math.max(128, this.cachedAssetFiles.size()));
        Map<Path, CachedAssetFile> previousFiles = this.cachedAssetFiles;
        this.cachedAssetFiles = new HashMap<>(Math.max(128, this.cachedAssetFiles.size()));
        folders.addAll(loadedPacks().stream().filter(Pack::enabled).map(Pack::resourcePackFolder).toList());
        folders.addAll(Config.foldersToMerge().stream().map(it -> plugin.dataFolderPath().getParent().resolve(it)).filter(Files::exists).toList());
        for (Path sourceFolder : folders) {
            if (Files.exists(sourceFolder)) {
                Files.walkFileTree(sourceFolder, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        CachedAssetFile cachedAsset = previousFiles.get(file);
                        long lastModified = Files.getLastModifiedTime(file).toMillis();
                        long size = Files.size(file);
                        if (cachedAsset != null && cachedAsset.lastModified() == lastModified && cachedAsset.size() == size) {
                            AbstractPackManager.this.cachedAssetFiles.put(file, cachedAsset);
                        } else {
                            cachedAsset = new CachedAssetFile(Files.readAllBytes(file), lastModified, size);
                            AbstractPackManager.this.cachedAssetFiles.put(file, cachedAsset);
                        }
                        if (fs == null) return FileVisitResult.CONTINUE;
                        Path relative = sourceFolder.relativize(file);
                        Path targetPath = fs.getPath("resource_pack/" + relative.toString().replace("\\", "/"));
                        List<Path> conflicts = conflictChecker.get(relative);
                        if (conflicts == null) {
                            Files.createDirectories(targetPath.getParent());
                            Files.write(targetPath, cachedAsset.data());
                            conflictChecker.put(relative, List.of(file));
                        } else {
                            PathContext relativeCTX = PathContext.of(relative);
                            PathContext targetCTX = PathContext.of(targetPath);
                            PathContext fileCTX = PathContext.of(file);
                            for (ResolutionConditional resolution : Config.resolutions()) {
                                if (resolution.matcher().test(relativeCTX)) {
                                    resolution.resolution().run(targetCTX, fileCTX);
                                    return FileVisitResult.CONTINUE;
                                }
                            }
                            switch (conflicts.size()) {
                                case 1 -> conflictChecker.put(relative, List.of(conflicts.get(0), file));
                                case 2 -> conflictChecker.put(relative, List.of(conflicts.get(0), conflicts.get(1), file));
                                case 3 -> conflictChecker.put(relative, List.of(conflicts.get(0), conflicts.get(1), conflicts.get(2), file));
                                case 4 -> conflictChecker.put(relative, List.of(conflicts.get(0), conflicts.get(1), conflicts.get(2), conflicts.get(3), file));
                                default -> {
                                    // just ignore it if it has many conflict files
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        List<Pair<Path, List<Path>>> conflicts = new ArrayList<>();
        for (Map.Entry<Path, List<Path>> entry : conflictChecker.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(Pair.of(entry.getKey(), entry.getValue()));
            }
        }
        return conflicts;
    }

    @Override
    public void generateResourcePack() throws IOException {
        this.plugin.logger().info("Generating resource pack...");
        long start = System.currentTimeMillis();

        // get the target location
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            // firstly merge existing folders
            Path generatedPackPath = fs.getPath("resource_pack");
            List<Pair<Path, List<Path>>> duplicated = this.updateCachedAssets(fs);
            if (!duplicated.isEmpty()) {
                plugin.logger().severe(AdventureHelper.miniMessage().stripTags(TranslationManager.instance().miniMessageTranslation("warning.config.pack.duplicated_files")));
                int x = 1;
                for (Pair<Path, List<Path>> path : duplicated) {
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

            this.generateFonts(generatedPackPath);
            this.generateItemModels(generatedPackPath, this.plugin.itemManager());
            this.generateItemModels(generatedPackPath, this.plugin.blockManager());
            this.generateBlockOverrides(generatedPackPath);
            this.generateLegacyItemOverrides(generatedPackPath);
            this.generateModernItemOverrides(generatedPackPath);
            this.generateModernItemModels1_21_2(generatedPackPath);
            this.generateModernItemModels1_21_4(generatedPackPath);
            this.generateOverrideSounds(generatedPackPath);
            this.generateCustomSounds(generatedPackPath);
            this.generateClientLang(generatedPackPath);
            this.generateEquipments(generatedPackPath);
            this.generateParticle(generatedPackPath);
            Path finalPath = resourcePackPath();
            Files.createDirectories(finalPath.getParent());
            try {
                this.zipGenerator.accept(generatedPackPath, finalPath);
            } catch (Exception e) {
                this.plugin.logger().severe("Error zipping resource pack", e);
            }
            long end = System.currentTimeMillis();
            this.plugin.logger().info("Finished generating resource pack in " + (end - start) + "ms");
            this.eventDispatcher.accept(generatedPackPath, finalPath);
        }
    }

    private void generateParticle(Path generatedPackPath) {
        if (!Config.removeTintedLeavesParticle()) return;
        if (Config.packMaxVersion() < 21.49f) return;
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
            Files.write(pngPath, EMPTY_IMAGE);
        } catch (IOException e) {
            this.plugin.logger().severe("Error writing particles file", e);
        }
    }

    private void generateEquipments(Path generatedPackPath) {
        for (EquipmentGeneration generator : this.plugin.itemManager().equipmentsToGenerate()) {
            EquipmentData equipmentData = generator.modernData();
            if (equipmentData != null && Config.packMaxVersion() >= 21.4f) {
                Path equipmentPath = generatedPackPath
                        .resolve("assets")
                        .resolve(equipmentData.assetId().namespace())
                        .resolve("equipment")
                        .resolve(equipmentData.assetId().value() + ".json");

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
                    equipmentJson = GsonHelper.deepMerge(equipmentJson, generator.get());
                } else {
                    equipmentJson = generator.get();
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
            if (equipmentData != null && Config.packMaxVersion() >= 21.2f && Config.packMinVersion() < 21.4f) {
                Path equipmentPath = generatedPackPath
                        .resolve("assets")
                        .resolve(equipmentData.assetId().namespace())
                        .resolve("models")
                        .resolve("equipment")
                        .resolve(equipmentData.assetId().value() + ".json");

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
                    equipmentJson = GsonHelper.deepMerge(equipmentJson, generator.get());
                } else {
                    equipmentJson = generator.get();
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
    }

    private void generateClientLang(Path generatedPackPath) {
        for (Map.Entry<String, I18NData> entry : this.plugin.translationManager().clientLangData().entrySet()) {
            JsonObject json = new JsonObject();
            for (Map.Entry<String, String> pair : entry.getValue().translations.entrySet()) {
                json.addProperty(pair.getKey(), pair.getValue());
            }
            Path langPath = generatedPackPath
                    .resolve("assets")
                    .resolve("minecraft")
                    .resolve("lang")
                    .resolve(entry.getKey() + ".json");
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
            soundTemplate = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
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

        for (Map.Entry<Key, Key> mapper : plugin.blockManager().soundMapper().entrySet()) {
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
        File blockStatesFile = new File(plugin.dataFolderFile(), "blockstates.yml");
        if (!blockStatesFile.exists()) plugin.saveResource("blockstates.yml");
        YamlDocument preset = Config.instance().loadYamlData(blockStatesFile);
        for (Map.Entry<Key, Map<String, JsonElement>> entry : plugin.blockManager().blockOverrides().entrySet()) {
            Key key = entry.getKey();
            Path overridedBlockPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("blockstates")
                    .resolve(key.value() + ".json");
            JsonObject stateJson = new JsonObject();
            JsonObject variants = new JsonObject();
            stateJson.add("variants", variants);
            Section presetSection = preset.getSection(key.toString());
            if (presetSection != null) {
                for (Map.Entry<String, Object> presetEntry : presetSection.getStringRouteMappedValues(false).entrySet()) {
                    if (presetEntry.getValue() instanceof Section section) {
                        variants.add(presetEntry.getKey(), YamlUtils.sectionToJson(section));
                    }
                }
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
        if (Config.packMaxVersion() < 21.19f) return;
        if (Config.packMinVersion() > 21.39f) return;

        // 此段代码生成1.21.2专用的item model文件，情况非常复杂！
        for (Map.Entry<Key, List<LegacyOverridesModel>> entry : this.plugin.itemManager().modernItemModels1_21_2().entrySet()) {
            Key itemModelPath = entry.getKey();
            List<LegacyOverridesModel> legacyOverridesModels = entry.getValue();

            // 检测item model合法性
            if (PRESET_MODERN_MODELS_ITEM.containsKey(itemModelPath) || PRESET_LEGACY_MODELS_ITEM.containsKey(itemModelPath)) {
                TranslationManager.instance().log("warning.config.resource_pack.item_model.conflict.vanilla", itemModelPath.asString());
                continue;
            }

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
                        overrides.add(legacyOverridesModel.toLegacyPredicateElement());
                    }
                    itemJson.add("overrides", overrides);
                } catch (IOException e) {
                    this.plugin.logger().warn("Failed to read item json " + itemPath.toAbsolutePath());
                    continue;
                }
            } else {
                // 如果路径不存在，则需要我们创建一个json对象，并对接model的路径
                itemJson = new JsonObject();
                LegacyOverridesModel firstModel = legacyOverridesModels.getFirst();
                itemJson.addProperty("parent", firstModel.model());
                JsonArray overrides = new JsonArray();
                for (LegacyOverridesModel legacyOverridesModel : legacyOverridesModels) {
                    overrides.add(legacyOverridesModel.toLegacyPredicateElement());
                }
                itemJson.add("overrides", overrides);
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

    private void generateModernItemModels1_21_4(Path generatedPackPath) {
        if (Config.packMaxVersion() < 21.39f) return;
        for (Map.Entry<Key, ItemModel> entry : this.plugin.itemManager().modernItemModels1_21_4().entrySet()) {
            Key key = entry.getKey();
            Path itemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("items")
                    .resolve(key.value() + ".json");

            if (PRESET_ITEMS.containsKey(key)) {
                TranslationManager.instance().log("warning.config.resource_pack.item_model.conflict.vanilla", key.asString());
                continue;
            }
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
            JsonObject model = new JsonObject();
            model.add("model", entry.getValue().get());
            try (BufferedWriter writer = Files.newBufferedWriter(itemPath)) {
                GsonHelper.get().toJson(model, writer);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to save item model for " + key, e);
            }
        }
    }

    private void generateModernItemOverrides(Path generatedPackPath) {
        if (Config.packMaxVersion() < 21.39f) return;
        for (Map.Entry<Key, TreeMap<Integer, ItemModel>> entry : this.plugin.itemManager().modernItemOverrides().entrySet()) {
            Key vanillaItemModel = entry.getKey();
            Path overridedItemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(vanillaItemModel.namespace())
                    .resolve("items")
                    .resolve(vanillaItemModel.value() + ".json");

            JsonObject originalItemModel;
            if (Files.exists(overridedItemPath)) {
                try {
                    originalItemModel = GsonHelper.readJsonFile(overridedItemPath).getAsJsonObject();
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

            boolean handAnimationOnSwap = Optional.ofNullable(originalItemModel.get("hand_animation_on_swap")).map(JsonElement::getAsBoolean).orElse(true);
            JsonObject fallbackModel = originalItemModel.get("model").getAsJsonObject();
            JsonObject newJson = new JsonObject();
            JsonObject model = new JsonObject();
            newJson.add("model", model);
            model.addProperty("type", "minecraft:range_dispatch");
            model.addProperty("property", "minecraft:custom_model_data");
            if (!handAnimationOnSwap) {
                model.addProperty("hand_animation_on_swap", false);
            }
            // 将原有的json读成fallback
            model.add("fallback", fallbackModel);
            JsonArray entries = new JsonArray();
            model.add("entries", entries);
            for (Map.Entry<Integer, ItemModel> modelWithDataEntry : entry.getValue().entrySet()) {
                JsonObject entryObject = new JsonObject();
                entryObject.addProperty("threshold", modelWithDataEntry.getKey());
                entryObject.add("model", modelWithDataEntry.getValue().get());
                entries.add(entryObject);
            }
            try {
                Files.createDirectories(overridedItemPath.getParent());
            } catch (IOException e) {
                this.plugin.logger().severe("Error creating " + overridedItemPath.toAbsolutePath(), e);
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedItemPath)) {
                GsonHelper.get().toJson(newJson, writer);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to save item model for " + vanillaItemModel, e);
            }
        }
    }

    private void generateLegacyItemOverrides(Path generatedPackPath) {
        if (Config.packMinVersion() > 21.39f) return;
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
            JsonArray overrides;
            if (originalItemModel.has("overrides")) {
                overrides = originalItemModel.getAsJsonArray("overrides");
            } else {
                overrides = new JsonArray();
                originalItemModel.add("overrides", overrides);
            }
            Collection<LegacyOverridesModel> legacyOverridesModels = entry.getValue();
            for (LegacyOverridesModel model : legacyOverridesModels) {
                overrides.add(model.toLegacyPredicateElement());
            }
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
                Files.writeString(fontPath, fontJson.toString().replace("\\\\u", "\\u"));
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
}
