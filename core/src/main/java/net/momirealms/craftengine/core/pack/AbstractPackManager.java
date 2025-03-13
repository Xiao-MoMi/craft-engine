package net.momirealms.craftengine.core.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
import net.momirealms.craftengine.core.pack.host.HostMode;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.model.ItemModel;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generator.ModelGenerator;
import net.momirealms.craftengine.core.pack.obfuscation.ObfA;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.locale.I18NData;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.SoundEvent;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

public abstract class AbstractPackManager implements PackManager {
    private static final String LEGACY_TEMPLATES = PluginProperties.getValue("legacy-templates").replace(".", "_");
    private static final String LATEST_TEMPLATES = PluginProperties.getValue("latest-templates").replace(".", "_");
    private final CraftEngine plugin;
    private final BiConsumer<Path, Path> eventDispatcher;
    private final Map<String, Pack> loadedPacks = new HashMap<>();
    private final Map<String, ConfigSectionParser> sectionParsers = new HashMap<>();
    private final TreeMap<ConfigSectionParser, List<CachedConfig>> cachedConfigs = new TreeMap<>();
    protected BiConsumer<Path, Path> zipGenerator;
    protected String packHash;
    protected UUID packUUID;

    public AbstractPackManager(CraftEngine plugin, BiConsumer<Path, Path> eventDispatcher) {
        this.plugin = plugin;
        this.eventDispatcher = eventDispatcher;
        this.zipGenerator = (p1, p2) -> {};
    }

    @Override
    public Path resourcePackPath() {
        return this.plugin.dataFolderPath()
                .resolve("generated")
                .resolve("resource_pack.zip");
    }

    @Override
    public void load() {
        this.loadPacks();
        this.loadConfigs();
        this.calculateHash();
        if (ConfigManager.hostMode() == HostMode.SELF_HOST) {
            Path path = ConfigManager.hostResourcePackPath().startsWith(".") ? plugin.dataFolderPath().resolve(ConfigManager.hostResourcePackPath()) : Path.of(ConfigManager.hostResourcePackPath());
            ResourcePackHost.instance().enable(ConfigManager.hostIP(), ConfigManager.hostPort(), path);
            ResourcePackHost.instance().setRateLimit(ConfigManager.requestRate(), ConfigManager.requestInterval(), TimeUnit.SECONDS);
        } else {
            ResourcePackHost.instance().disable();
        }
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
        if (this.sectionParsers.containsKey(parser.sectionId())) return false;
        this.sectionParsers.put(parser.sectionId(), parser);
        return true;
    }

    @Override
    public boolean unregisterConfigSectionParser(String id) {
        if (!this.sectionParsers.containsKey(id)) return false;
        this.sectionParsers.remove(id);
        return true;
    }

    public Path selfHostPackPath() {
        return ConfigManager.hostResourcePackPath().startsWith(".") ? plugin.dataFolderPath().resolve(ConfigManager.hostResourcePackPath()) : Path.of(ConfigManager.hostResourcePackPath());
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
                    if (Files.exists(metaFile) && Files.isRegularFile(metaFile)) {
                        YamlDocument metaYML = ConfigManager.instance().loadYamlData(metaFile.toFile());
                        namespace = metaYML.getString("namespace", namespace);
                        description = metaYML.getString("description");
                        version = metaYML.getString("version");
                        author = metaYML.getString("author");
                    }
                    Pack pack = new Pack(path, new PackMeta(author, description, version, namespace));
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
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/cooking_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/crafting_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/no_recipe.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/get_item.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/next_page_0.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/next_page_1.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/previous_page_0.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/previous_page_1.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/return.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/cooking_info.png");
        plugin.saveResource("resources/internal/resourcepack/assets/minecraft/textures/item/custom/gui/cooking_info.png.mcmeta");
        // default pack
        plugin.saveResource("resources/default/pack.yml");
        // pack meta
        plugin.saveResource("resources/default/resourcepack/pack.mcmeta");
        plugin.saveResource("resources/default/resourcepack/pack.png");
        // templates
        plugin.saveResource("resources/default/configuration/templates.yml");
        // i18n
        plugin.saveResource("resources/default/configuration/i18n.yml");
        // categories
        plugin.saveResource("resources/default/configuration/categories.yml");
        // icons
        plugin.saveResource("resources/default/configuration/icons.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/font/image/icons.png");
        // blocks
        plugin.saveResource("resources/default/configuration/blocks.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/chinese_lantern_top.png.mcmeta");
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
        // fairy flower
        plugin.saveResource("resources/default/configuration/fairy_flower.yml");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_1.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_2.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_3.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_4.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/fairy_flower_5.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/fairy_flower.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/fairy_flower_1.json");
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

    private void loadConfigs() {
        long o1 = System.nanoTime();
        for (Pack pack : loadedPacks()) {
            Pair<List<Path>, List<Path>> files = FileUtils.getConfigsDeeply(pack.configurationFolder());
            for (Path path : files.left()) {
                Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()));
                try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
                    Map<String, Object> data = yaml.load(inputStream);
                    if (data == null) continue;
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        processConfigEntry(entry, path, pack);
                    }
                } catch (IOException e) {
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
                } catch (IOException e) {
                    this.plugin.logger().warn(path, "Error loading config file", e);
                }
            }
        }
        long o2 = System.nanoTime();
        this.plugin.logger().info("Loaded packs. Took " + String.format("%.2f", ((o2 - o1) / 1_000_000.0)) + " ms");
        for (Map.Entry<ConfigSectionParser, List<CachedConfig>> entry : this.cachedConfigs.entrySet()) {
            ConfigSectionParser parser = entry.getKey();
            boolean isTemplate = parser.sectionId().equals(TemplateManager.CONFIG_SECTION_NAME);
            long t1 = System.nanoTime();
            for (CachedConfig cached : entry.getValue()) {
                for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
                    String key = configEntry.getKey();
                    try {
                        Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
                        if (configEntry.getValue() instanceof Map<?, ?> configSection0) {
                            Map<String, Object> configSection1 = castToMap(configSection0, false);
                            if ((boolean) configSection1.getOrDefault("enable", true)) {
                                parser.parseSection(cached.pack(), cached.filePath(), id, isTemplate ? configSection1 : plugin.templateManager().applyTemplates(configSection1));
                            }
                        } else {
                            this.plugin.logger().warn(cached.filePath(), "Configuration section is required for " + parser.sectionId() + "." + configEntry.getKey() + " - ");
                        }
                    } catch (Exception e) {
                        this.plugin.logger().warn(cached.filePath(), "Error loading " + parser.sectionId() + "." + key, e);
                    }
                }
            }
            long t2 = System.nanoTime();
            this.plugin.logger().info("Loaded " + parser.sectionId() + " in " + String.format("%.2f", ((t2 - t1) / 1_000_000.0)) + " ms");
        }
        this.cachedConfigs.clear();
    }

    private void processConfigEntry(Map.Entry<String, Object> entry, Path path, Pack pack) {
        if (entry.getValue() instanceof Map<?,?> typeSections0) {
            String configType = entry.getKey();
            Optional.ofNullable(this.sectionParsers.get(configType))
                    .ifPresent(parser -> {
                        this.cachedConfigs.computeIfAbsent(parser, k -> new ArrayList<>())
                                .add(new CachedConfig(castToMap(typeSections0, false), path, pack));
                    });
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
            folders.addAll(ConfigManager.foldersToMerge().stream().map(it -> plugin.dataFolderPath().getParent().resolve(it)).filter(Files::exists).toList());

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

        Path zipFile = resourcePackPath();
        try {
            this.zipGenerator.accept(generatedPackPath, zipFile);
        } catch (Exception e) {
            this.plugin.logger().severe("Error zipping resource pack", e);
        }

        long end = System.currentTimeMillis();
        this.plugin.logger().info("Finished generating resource pack in " + (end - start) + "ms");

        this.eventDispatcher.accept(generatedPackPath, zipFile);
        this.calculateHash();
    }

    private void calculateHash() {
        Path zipFile = selfHostPackPath();
        if (Files.exists(zipFile)) {
            try {
                this.packHash = computeSHA1(zipFile);
                this.packUUID = UUID.nameUUIDFromBytes(this.packHash.getBytes(StandardCharsets.UTF_8));
            } catch (IOException | NoSuchAlgorithmException e) {
                this.plugin.logger().severe("Error calculating resource pack hash", e);
            }
        } else {
            this.packHash = "";
            this.packUUID = UUID.nameUUIDFromBytes("EMPTY".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generateClientLang(Path generatedPackPath) {
        for (Map.Entry<String, I18NData> entry : this.plugin.translationManager().clientLangManager().langData().entrySet()) {
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
        if (!ConfigManager.enableSoundSystem()) return;

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
        YamlDocument preset = ConfigManager.instance().loadYamlData(blockStatesFile);
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
                plugin.logger().warn("Failed to save item model for [" + key + "]");
            }
        }
    }

    private void generateModernItemModels1_21_2(Path generatedPackPath) {
        if (ConfigManager.packMaxVersion() < 21.19f) return;
        if (ConfigManager.packMinVersion() > 21.39f) return;

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
                try (InputStream inputStream = plugin.resourceStream("internal/templates_" + LEGACY_TEMPLATES + "/" + key.namespace() + "/items/" + key.value() + ".json")) {
                    if (inputStream != null) {
                        plugin.logger().warn("Failed to generate item model for [" + key + "] because it conflicts with vanilla item");
                        continue;
                    }
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load item model", e);
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

        if (ConfigManager.packMinVersion() < 21.19f && has) {
            plugin.logger().warn("You are using item-model component for models which requires 1.21.2+. But the min supported version is " + "1." + ConfigManager.packMinVersion());
        }
    }

    private void generateModernItemModels1_21_4(Path generatedPackPath) {
        if (ConfigManager.packMaxVersion() < 21.39f) return;
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
                try (InputStream inputStream = plugin.resourceStream("internal/templates_" + LATEST_TEMPLATES + "/" + key.namespace() + "/items/" + key.value() + ".json")) {
                    if (inputStream != null) {
                        plugin.logger().warn("Failed to generate item model for [" + key + "] because it conflicts with vanilla item");
                        continue;
                    }
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load item model", e);
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
        if (ConfigManager.packMaxVersion() < 21.39f) return;
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
                    plugin.logger().warn("Failed to load existing item model", e);
                    continue;
                }
            } else {
                try (InputStream inputStream = plugin.resourceStream("internal/templates_" + LATEST_TEMPLATES + "/" + key.namespace() + "/items/" + key.value() + ".json")) {
                    if (inputStream == null) {
                        plugin.logger().warn("Failed to use [" + key + "] for base model");
                        continue;
                    }
                    originalItemModel = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load item model", e);
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
        if (ConfigManager.packMinVersion() > 21.39f) return;
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
                    plugin.logger().warn("Failed to load existing item model", e);
                    continue;
                }
            } else {
                try (InputStream inputStream = plugin.resourceStream("internal/templates_" + LEGACY_TEMPLATES + "/" + key.namespace() + "/items/" + key.value() + ".json")) {
                    if (inputStream == null) {
                        plugin.logger().warn("Failed to use [" + key + "] for base model");
                        continue;
                    }
                    originalItemModel = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                } catch (IOException e) {
                    plugin.logger().warn("Failed to load item model", e);
                    continue;
                }
            }
            if (originalItemModel == null) {
                plugin.logger().warn("Failed to load item model for [" + key + "]");
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
        for (Font font : plugin.imageManager().fontsInUse()) {
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
                providers.add(image.getJson());
            }

            try (FileWriter fileWriter = new FileWriter(fontPath.toFile())) {
                fileWriter.write(fontJson.toString().replace("\\\\u", "\\u"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (ConfigManager.resourcePack$overrideUniform()) {
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

    protected String computeSHA1(Path path) throws IOException, NoSuchAlgorithmException {
        InputStream file = Files.newInputStream(path);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = file.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        file.close();

        StringBuilder hexString = new StringBuilder(40);
        for (byte b : digest.digest()) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
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
                            for (ConditionalResolution resolution : ConfigManager.resolutions()) {
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
