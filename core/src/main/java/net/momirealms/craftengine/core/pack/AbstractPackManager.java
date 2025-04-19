package net.momirealms.craftengine.core.pack;

import com.google.gson.*;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.EquipmentData;
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
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
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.I18NData;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
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
    private final Map<String, ConfigSectionParser> sectionParsers = new HashMap<>();
    private final TreeMap<ConfigSectionParser, List<CachedConfig>> cachedConfigs = new TreeMap<>();
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

    private void loadInternalPng(String path, Consumer<byte[]> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                callback.accept(inputStream.readAllBytes());
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
            this.resourcePackHost = ResourcePackHosts.fromMap(MiscUtils.castToMap(list.get(0), false));
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
        this.cachedConfigs.clear();
    }

    @Override
    public void delayedInit() {
       try {
           Class<?> magicClazz = ReflectionUtils.getClazz(getClass().getSuperclass().getPackageName() + new String(Base64Utils.decode(ObfA.VALUES, Integer.parseInt(String.valueOf(ObfA.VALUES[71]).substring(0,1))), StandardCharsets.UTF_8));
           if (magicClazz != null) {
               int fileCount = ObfA.VALUES[1] - ObfA.VALUES[17];
               Constructor<?> magicConstructor = ReflectionUtils.getConstructor(magicClazz, fileCount);
               Method magicMethod = ReflectionUtils.getMethod(magicClazz, void.class);
               this.zipGenerator = (p1, p2) -> {
                   try {
                       assert magicConstructor != null;
                       Object magicObject = magicConstructor.newInstance(p1, p2);
                       assert magicMethod != null;
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

    @NotNull
    @Override
    public Collection<Pack> loadedPacks() {
        return this.loadedPacks.values();
    }

    @Override
    public boolean registerConfigSectionParser(ConfigSectionParser parser) {
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

    private void loadResourceConfigs(Predicate<ConfigSectionParser> predicate) {
        long o1 = System.nanoTime();
        for (Pack pack : loadedPacks()) {
            if (!pack.enabled()) continue;
            Pair<List<Path>, List<Path>> files = FileUtils.getConfigsDeeply(pack.configurationFolder());
            for (Path path : files.left()) {
                try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
                    Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()));
                    Map<String, Object> data = yaml.load(inputStream);
                    if (data == null) continue;
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        processConfigEntry(entry, path, pack);
                    }
                } catch (Exception e) {
                    this.plugin.logger().warn(path, "Error loading config file", e);
                }
            }
            for (Path path : files.right()) {
                try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
                    Map<?, ?> dataRaw = GsonHelper.get().fromJson(JsonParser.parseReader(inputStream).getAsJsonObject(), Map.class);
                    Map<String, Object> data = MiscUtils.castToMap(dataRaw, false);
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        processConfigEntry(entry, path, pack);
                    }
                } catch (Exception e) {
                    this.plugin.logger().warn(path, "Error loading config file", e);
                }
            }
        }
        long o2 = System.nanoTime();
        this.plugin.logger().info("Loaded packs. Took " + String.format("%.2f", ((o2 - o1) / 1_000_000.0)) + " ms");
        for (Map.Entry<ConfigSectionParser, List<CachedConfig>> entry : this.cachedConfigs.entrySet()) {
            ConfigSectionParser parser = entry.getKey();
            long t1 = System.nanoTime();
            for (CachedConfig cached : entry.getValue()) {
                for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
                    String key = configEntry.getKey();
                    try {
                        Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
                        if (parser.isTemplate()) {
                            this.plugin.templateManager().addTemplate(cached.pack(), cached.filePath(), id, configEntry.getValue());
                        } else if (predicate.test(parser)) {
                            if (configEntry.getValue() instanceof Map<?, ?> configSection0) {
                                Map<String, Object> configSection1 = castToMap(configSection0, false);
                                if ((boolean) configSection1.getOrDefault("enable", true)) {
                                    parser.parseSection(cached.pack(), cached.filePath(), id, plugin.templateManager().applyTemplates(configSection1));
                                }
                            } else {
                                this.plugin.logger().warn(cached.filePath(), "Configuration section is required for " + parser.sectionId()[0] + "." + configEntry.getKey() + " - ");
                            }
                        }
                    } catch (Exception e) {
                        this.plugin.logger().warn(cached.filePath(), "Error loading " + parser.sectionId()[0] + "." + key, e);
                    }
                }
            }
            long t2 = System.nanoTime();
            this.plugin.logger().info("Loaded " + parser.sectionId()[0] + " in " + String.format("%.2f", ((t2 - t1) / 1_000_000.0)) + " ms");
        }
        this.cachedConfigs.clear();
    }

    private void processConfigEntry(Map.Entry<String, Object> entry, Path path, Pack pack) {
        if (entry.getValue() instanceof Map<?,?> typeSections0) {
            String key = entry.getKey();
            int hashIndex = key.indexOf('#');
            String configType = hashIndex != -1 ? key.substring(0, hashIndex) : key;
            Optional.ofNullable(this.sectionParsers.get(configType))
                    .ifPresent(parser -> this.cachedConfigs.computeIfAbsent(parser, k -> new ArrayList<>())
                            .add(new CachedConfig(castToMap(typeSections0, false), path, pack)));
        }
    }

    @Override
    public void generateResourcePack() {
        this.plugin.logger().info("Generating resource pack...");
        long start = System.currentTimeMillis();
        // get the target location
        Path generatedPackPath = this.plugin.dataFolderPath()
                .resolve("generated")
                .resolve("resource_pack");

        try {
            org.apache.commons.io.FileUtils.deleteDirectory(generatedPackPath.toFile());
        } catch (IOException e) {
            this.plugin.logger().severe("Error deleting previous resource pack", e);
        }

        // firstly merge existing folders
        try {
            List<Path> folders = new ArrayList<>();
            folders.addAll(loadedPacks().stream().map(Pack::resourcePackFolder).toList());
            folders.addAll(Config.foldersToMerge().stream().map(it -> plugin.dataFolderPath().getParent().resolve(it)).filter(Files::exists).toList());

            List<Pair<Path, List<Path>>> duplicated = mergeFolder(folders, generatedPackPath);
            if (!duplicated.isEmpty()) {
                this.plugin.logger().severe("Duplicated files Found. Please resolve them through config.yml resource-pack.duplicated-files-handler.");
                for (Pair<Path, List<Path>> path : duplicated) {
                    this.plugin.logger().warn("");
                    this.plugin.logger().warn("Target: " + path.left());
                    for (Path path0 : path.right()) {
                        this.plugin.logger().warn(" - " + path0.toAbsolutePath());
                    }
                }
            }
        } catch (IOException e) {
            this.plugin.logger().severe("Error merging resource pack", e);
        }

        this.generateFonts(generatedPackPath);
        this.generateLegacyItemOverrides(generatedPackPath);
        this.generateModernItemOverrides(generatedPackPath);
        this.generateModernItemModels1_21_2(generatedPackPath);
        this.generateModernItemModels1_21_4(generatedPackPath);
        this.generateBlockOverrides(generatedPackPath);
        this.generateItemModels(generatedPackPath, this.plugin.itemManager());
        this.generateItemModels(generatedPackPath, this.plugin.blockManager());
        this.generateOverrideSounds(generatedPackPath);
        this.generateCustomSounds(generatedPackPath);
        this.generateClientLang(generatedPackPath);
        this.generateEquipments(generatedPackPath);
        this.generateParticle(generatedPackPath);

        Path zipFile = resourcePackPath();
        try {
            this.zipGenerator.accept(generatedPackPath, zipFile);
        } catch (Exception e) {
            this.plugin.logger().severe("Error zipping resource pack", e);
        }

        long end = System.currentTimeMillis();
        this.plugin.logger().info("Finished generating resource pack in " + (end - start) + "ms");
        this.eventDispatcher.accept(generatedPackPath, zipFile);
    }

    private void generateParticle(Path generatedPackPath) {
        if (!Config.removeTintedLeavesParticle()) return;
        if (Config.packMaxVersion() < 21.49f) return;
        var json = new JsonObject();
        var textures = new JsonArray();
        textures.add("empty");
        json.add("textures", textures);
        var jsonPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("particles")
                .resolve("tinted_leaves.json");
        var pngPath = generatedPackPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("textures")
                .resolve("particle")
                .resolve("empty.png");
        try {
            Files.createDirectories(jsonPath.getParent());
            Files.createDirectories(pngPath.getParent());
        } catch (IOException e) {
            plugin.logger().severe("Error creating directories", e);
            return;
        }
        try {
            GsonHelper.writeJsonFile(json, jsonPath);
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
                plugin.logger().warn("Failed to generate model because " + modelPath.toAbsolutePath() + " already exists");
                continue;
            }

            try {
                Files.createDirectories(modelPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + modelPath.toAbsolutePath());
                continue;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(modelPath)) {
                GsonHelper.get().toJson(generation.getJson(), writer);
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
                plugin.logger().severe("Error creating " + overridedBlockPath.toAbsolutePath());
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedBlockPath)) {
                GsonHelper.get().toJson(stateJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to create block states for [" + key + "]");
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
                plugin.logger().severe("Error creating " + overridedBlockPath.toAbsolutePath());
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedBlockPath)) {
                GsonHelper.get().toJson(stateJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to create block states for [" + key + "]");
            }
        }
    }

    private void generateModernItemModels1_21_2(Path generatedPackPath) {
        if (Config.packMaxVersion() < 21.19f) return;
        if (Config.packMinVersion() > 21.39f) return;

        boolean has = false;
        for (Map.Entry<Key, List<LegacyOverridesModel>> entry : plugin.itemManager().modernItemModels1_21_2().entrySet()) {
            has = true;
            Key key = entry.getKey();
            List<LegacyOverridesModel> legacyOverridesModels = entry.getValue();
            boolean first = true;
            JsonObject jsonObject = new JsonObject();
            JsonArray overrides = new JsonArray();
            for (LegacyOverridesModel model : legacyOverridesModels) {
                if (first) {
                    jsonObject.addProperty("parent", model.model());
                    if (model.hasPredicate()) {
                        overrides.add(model.toLegacyPredicateElement());
                    }
                    first = false;
                } else {
                    overrides.add(model.toLegacyPredicateElement());
                }
            }
            if (!overrides.isEmpty()) {
                jsonObject.add("overrides", overrides);
            }

            Path itemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("models")
                    .resolve("item")
                    .resolve(key.value() + ".json");
            if (Files.exists(itemPath)) {
                plugin.logger().warn("Failed to generate item model for [" + key + "] because " + itemPath.toAbsolutePath() + " already exists");
            } else {
                if (PRESET_MODERN_MODELS_ITEM.containsKey(key) || PRESET_LEGACY_MODELS_ITEM.containsKey(key)) {
                    plugin.logger().warn("Failed to generate item model for [" + key + "] because it conflicts with vanilla item");
                    continue;
                }
            }
            try {
                Files.createDirectories(itemPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + itemPath.toAbsolutePath());
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(itemPath)) {
                GsonHelper.get().toJson(jsonObject, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for [" + key + "]");
            }
        }

        if (Config.packMinVersion() < 21.19f && has) {
            plugin.logger().warn("You are using item-model component for models which requires 1.21.2+. But the min supported version is " + "1." + Config.packMinVersion());
        }
    }

    private void generateModernItemModels1_21_4(Path generatedPackPath) {
        if (Config.packMaxVersion() < 21.39f) return;
        for (Map.Entry<Key, ItemModel> entry : plugin.itemManager().modernItemModels1_21_4().entrySet()) {
            Key key = entry.getKey();
            Path itemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("items")
                    .resolve(key.value() + ".json");
            if (Files.exists(itemPath)) {
                plugin.logger().warn("Failed to generate item model for [" + key + "] because " + itemPath.toAbsolutePath() + " already exists");
            } else {
                if (PRESET_ITEMS.containsKey(key)) {
                    plugin.logger().warn("Failed to generate item model for [" + key + "] because it conflicts with vanilla item");
                    continue;
                }
            }
            try {
                Files.createDirectories(itemPath.getParent());
            } catch (IOException e) {
                plugin.logger().severe("Error creating " + itemPath.toAbsolutePath());
                continue;
            }
            JsonObject model = new JsonObject();
            model.add("model", entry.getValue().get());
            try (BufferedWriter writer = Files.newBufferedWriter(itemPath)) {
                GsonHelper.get().toJson(model, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for [" + key + "]");
            }
        }
    }

    private void generateModernItemOverrides(Path generatedPackPath) {
        if (Config.packMaxVersion() < 21.39f) return;
        for (Map.Entry<Key, TreeMap<Integer, ItemModel>> entry : plugin.itemManager().modernItemOverrides().entrySet()) {
            Key key = entry.getKey();
            Path overridedItemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("items")
                    .resolve(key.value() + ".json");

            JsonObject originalItemModel;
            if (Files.exists(overridedItemPath)) {
                try {
                    originalItemModel = GsonHelper.readJsonFile(overridedItemPath).getAsJsonObject();
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load existing item model (modern)", e);
                    continue;
                }
            } else {
                originalItemModel = PRESET_ITEMS.get(key);
                if (originalItemModel == null) {
                    plugin.logger().warn("Failed to load existing item model for [" + key + "] (modern)");
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
                plugin.logger().severe("Error creating " + overridedItemPath.toAbsolutePath());
                continue;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(overridedItemPath)) {
                GsonHelper.get().toJson(newJson, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for [" + key + "]");
            }
        }
    }

    private void generateLegacyItemOverrides(Path generatedPackPath) {
        if (Config.packMinVersion() > 21.39f) return;
        for (Map.Entry<Key, TreeSet<LegacyOverridesModel>> entry : plugin.itemManager().legacyItemOverrides().entrySet()) {
            Key key = entry.getKey();
            Path overridedItemPath = generatedPackPath
                    .resolve("assets")
                    .resolve(key.namespace())
                    .resolve("models")
                    .resolve("item")
                    .resolve(key.value() + ".json");

            JsonObject originalItemModel;
            if (Files.exists(overridedItemPath)) {
                try (BufferedReader reader = Files.newBufferedReader(overridedItemPath)) {
                    originalItemModel = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load existing item model (legacy)", e);
                    continue;
                }
            } else {
                originalItemModel = PRESET_LEGACY_MODELS_ITEM.get(key);
            }
            if (originalItemModel == null) {
                plugin.logger().warn("Failed to load item model for [" + key + "] (legacy)");
                continue;
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
                plugin.logger().severe("Error creating " + overridedItemPath.toAbsolutePath());
                continue;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(overridedItemPath)) {
                GsonHelper.get().toJson(originalItemModel, writer);
            } catch (IOException e) {
                plugin.logger().warn("Failed to save item model for [" + key + "]");
            }
        }
    }

    private void generateFonts(Path generatedPackPath) {
        // generate image font json
        for (Font font : plugin.fontManager().fonts()) {
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
                    plugin.logger().warn(fontPath + " is not a valid font json file");
                }
            } else {
                fontJson = new JsonObject();
                try {
                    Files.createDirectories(fontPath.getParent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
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

            try (FileWriter fileWriter = new FileWriter(fontPath.toFile())) {
                fileWriter.write(fontJson.toString().replace("\\\\u", "\\u"));
            } catch (IOException e) {
                throw new RuntimeException(e);
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

    private List<Pair<Path, List<Path>>> mergeFolder(Collection<Path> sourceFolders, Path targetFolder) throws IOException {
        Map<Path, List<Path>> conflictChecker = new HashMap<>();
        for (Path sourceFolder : sourceFolders) {
            if (Files.exists(sourceFolder)) {
                Files.walkFileTree(sourceFolder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = sourceFolder.relativize(file);
                        Path targetPath = targetFolder.resolve(relative);
                        List<Path> conflicts = conflictChecker.computeIfAbsent(relative, k -> new ArrayList<>());
                        if (conflicts.isEmpty()) {
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            conflicts.add(file);
                        } else {
                            for (ConditionalResolution resolution : Config.resolutions()) {
                                if (resolution.matcher().test(relative)) {
                                    resolution.resolution().run(targetPath, file);
                                    return FileVisitResult.CONTINUE;
                                }
                            }
                            conflicts.add(file);
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
}
