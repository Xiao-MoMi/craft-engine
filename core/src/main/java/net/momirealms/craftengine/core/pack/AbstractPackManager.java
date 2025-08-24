package net.momirealms.craftengine.core.pack;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Font;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionConditional;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackHosts;
import net.momirealms.craftengine.core.pack.host.impl.NoneHost;
import net.momirealms.craftengine.core.pack.model.ItemModel;
import net.momirealms.craftengine.core.pack.model.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.pack.model.rangedisptach.CustomModelDataRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.obfuscation.ObfA;
import net.momirealms.craftengine.core.pack.obfuscation.ResourcePackGenerationException;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
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
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

@SuppressWarnings("DuplicatedCode")
public abstract class AbstractPackManager implements PackManager {
    public static final Map<Key, JsonObject> PRESET_MODERN_MODELS_ITEM = new HashMap<>();
    public static final Map<Key, JsonObject> PRESET_LEGACY_MODELS_ITEM = new HashMap<>();
    public static final Map<Key, JsonObject> PRESET_MODELS_BLOCK = new HashMap<>();
    public static final Map<Key, ModernItemModel> PRESET_ITEMS = new HashMap<>();
    public static final Set<Key> VANILLA_TEXTURES = new HashSet<>();
    public static final Set<Key> VANILLA_MODELS = new HashSet<>();
    public static final String NEW_TRIM_MATERIAL = "custom";
    public static final Set<String> ALLOWED_VANILLA_EQUIPMENT = Set.of("chainmail", "diamond", "gold", "iron", "netherite");
    private static final byte[] EMPTY_IMAGE;
    private static final byte[] EMPTY_EQUIPMENT_IMAGE;
    static {
        try (ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
             ByteArrayOutputStream stream2 = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", stream1);
            EMPTY_IMAGE = stream1.toByteArray();
            ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", stream2);
            EMPTY_EQUIPMENT_IMAGE = stream2.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty images.", e);
        }
    }

    private final CraftEngine plugin;
    private final BiConsumer<Path, Path> eventDispatcher;
    private final Map<String, Pack> loadedPacks = new HashMap<>();
    private final Map<String, ConfigParser> sectionParsers = new HashMap<>();
    private final JsonObject vanillaAtlas;
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
        try (InputStream inputStream = plugin.resourceStream("internal/atlases/blocks.json")) {
            this.vanillaAtlas = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read internal/atlases/blocks.json", e);
        }
    }

    private void initInternalData() {
        loadInternalData("internal/models/item/legacy/_all.json", PRESET_LEGACY_MODELS_ITEM::put);
        loadInternalData("internal/models/item/_all.json", PRESET_MODERN_MODELS_ITEM::put);
        loadInternalData("internal/models/block/_all.json", PRESET_MODELS_BLOCK::put);
        loadModernItemModel("internal/items/_all.json", PRESET_ITEMS::put);

        loadInternalList("models", "block/", VANILLA_MODELS::add);
        loadInternalList("models", "item/", VANILLA_MODELS::add);

        loadInternalList("textures", "", VANILLA_TEXTURES::add);
        VANILLA_MODELS.add(Key.of("minecraft", "builtin/entity"));
        for (int i = 0; i < 256; i++) {
            VANILLA_TEXTURES.add(Key.of("minecraft", "font/unicode_page_" + String.format("%02x", i)));
        }
    }

    private void loadModernItemModel(String path, BiConsumer<Key, ModernItemModel> callback) {
        try (InputStream inputStream = this.plugin.resourceStream(path)) {
            if (inputStream != null) {
                JsonObject allModelsItems = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
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

    private void loadInternalList(String type, String prefix, Consumer<Key> callback) {
        try (InputStream inputStream = this.plugin.resourceStream("internal/" + type + "/" + prefix + "_list.json")) {
            if (inputStream != null) {
                JsonObject listJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                JsonArray fileList = listJson.getAsJsonArray("files");
                for (JsonElement element : fileList) {
                    if (element instanceof JsonPrimitive primitive) {
                        callback.accept(Key.of(prefix + FileUtils.pathWithoutExtension(primitive.getAsString())));
                    }
                }
                JsonArray directoryList = listJson.getAsJsonArray("directories");
                for (JsonElement element : directoryList) {
                    if (element instanceof JsonPrimitive primitive) {
                        loadInternalList(type, prefix + primitive.getAsString() + "/", callback);
                    }
                }
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load internal _list.json" + prefix, e);
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
        Object hostingObj = Config.instance().settings().get("resource-pack.delivery.hosting");
        Map<String, Object> arguments;
        if (hostingObj instanceof Map<?,?>) {
            arguments = MiscUtils.castToMap(hostingObj, false);
        } else if (hostingObj instanceof List<?> list && !list.isEmpty()) {
            arguments = MiscUtils.castToMap(list.get(0), false);
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
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to load resource pack host", e);
            this.resourcePackHost = NoneHost.INSTANCE;
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
                   } catch (ResourcePackGenerationException e) {
                       this.plugin.logger().warn("Failed to generate resource pack: " + e.getMessage());
                   } catch (Exception e) {
                       this.plugin.logger().warn("Failed to generate zip files\n" + new StringWriter(){{e.printStackTrace(new PrintWriter(this));}}.toString().replaceAll("\\.[Il]{2,}", "").replaceAll("/[Il]{2,}", ""));
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
                    String namespace = path.getFileName().toString();
                    if (namespace.charAt(0) == '.') {
                        continue;
                    }
                    Path metaFile = path.resolve("pack.yml");
                    String description = null;
                    String version = null;
                    String author = null;
                    boolean enable = true;
                    if (Files.exists(metaFile) && Files.isRegularFile(metaFile)) {
                        Yaml yaml = new Yaml(new StringKeyConstructor(path, new LoaderOptions()));
                        try (InputStream is = Files.newInputStream(metaFile)) {
                            Map<String, Object> data = yaml.load(is);
                            enable = ResourceConfigUtils.getAsBoolean(data.getOrDefault("enable", true), "enable");
                            namespace = data.getOrDefault("namespace", namespace).toString();
                            description = Optional.ofNullable(data.get("description")).map(String::valueOf).orElse(null);
                            version = Optional.ofNullable(data.get("version")).map(String::valueOf).orElse(null);
                            author = Optional.ofNullable(data.get("author")).map(String::valueOf).orElse(null);
                        } catch (IOException e) {
                            this.plugin.logger().warn("Failed to load " + metaFile, e);
                        }
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
        plugin.saveResource("resources/remove_shulker_head/resourcepack/1_20_5_remove_shulker_head_overlay/minecraft/shaders/core/rendertype_entity_solid.fsh");
        plugin.saveResource("resources/remove_shulker_head/resourcepack/assets/minecraft/textures/entity/shulker/shulker_white.png");
        plugin.saveResource("resources/remove_shulker_head/pack.yml");
        plugin.saveResource("resources/legacy_armor/resourcepack/assets/minecraft/textures/trims/entity/humanoid/chainmail.png");
        plugin.saveResource("resources/legacy_armor/resourcepack/assets/minecraft/textures/trims/entity/humanoid_leggings/chainmail.png");
        plugin.saveResource("resources/legacy_armor/configuration/chainmail.yml");
        plugin.saveResource("resources/legacy_armor/pack.yml");
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
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_side.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_on.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/copper_coil_on_side.png");
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
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flame_elytra.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/broken_flame_elytra.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/entity/equipment/wings/flame_elytra.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/cap.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/cap.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/pebble.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/pebble.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_1.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_2.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/block/custom/pebble_3.json");

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
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_trapdoor.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_door_top.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/block/custom/palm_door_bottom.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/palm_door.png");
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
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_ceiling.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_ground.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/models/item/custom/flower_basket_wall.json");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flower_basket.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/item/custom/flower_basket_2d.png");
        // tooltip
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_background.png.mcmeta");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png");
        plugin.saveResource("resources/default/resourcepack/assets/minecraft/textures/gui/sprites/tooltip/topaz_frame.png.mcmeta");
    }

    private TreeMap<ConfigParser, List<CachedConfigSection>> updateCachedConfigFiles() {
        TreeMap<ConfigParser, List<CachedConfigSection>> cachedConfigs = new TreeMap<>();
        Map<Path, CachedConfigFile> previousFiles = this.cachedConfigFiles;
        this.cachedConfigFiles = new Object2ObjectOpenHashMap<>(32);
        for (Pack pack : loadedPacks()) {
            if (!pack.enabled()) continue;
            Path configurationFolderPath = pack.configurationFolder();
            if (!Files.isDirectory(configurationFolderPath)) continue;
            try {
                Files.walkFileTree(configurationFolderPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) {
                        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml")) {
                            CachedConfigFile cachedFile = previousFiles.get(path);
                            long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                            long size = attrs.size();
                            if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                                AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                            } else {
                                try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                                    Yaml yaml = new Yaml(new StringKeyConstructor(path, new LoaderOptions()));
                                    Map<String, Object> data = yaml.load(inputStream);
                                    if (data == null)  return FileVisitResult.CONTINUE;
                                    cachedFile = new CachedConfigFile(data, pack, lastModifiedTime, size);
                                    AbstractPackManager.this.cachedConfigFiles.put(path, cachedFile);
                                } catch (IOException e) {
                                    AbstractPackManager.this.plugin.logger().severe("Error while reading config file: " + path, e);
                                    return FileVisitResult.CONTINUE;
                                } catch (LocalizedException e) {
                                    e.setArgument(0, path.toString());
                                    TranslationManager.instance().log(e.node(), e.arguments());
                                    return FileVisitResult.CONTINUE;
                                }
                            }
                            for (Map.Entry<String, Object> entry : cachedFile.config().entrySet()) {
                                processConfigEntry(entry, path, cachedFile.pack(), (p, c) ->
                                        cachedConfigs.computeIfAbsent(p, k -> new ArrayList<>()).add(c)
                                );
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                this.plugin.logger().severe("Error while reading config file", e);
            }
        }
        return cachedConfigs;
    }

    private void loadResourceConfigs(Predicate<ConfigParser> predicate) {
        long o1 = System.nanoTime();
        TreeMap<ConfigParser, List<CachedConfigSection>> cachedConfigs = this.updateCachedConfigFiles();
        long o2 = System.nanoTime();
        this.plugin.logger().info("Loaded packs. Took " + String.format("%.2f", ((o2 - o1) / 1_000_000.0)) + " ms");
        for (Map.Entry<ConfigParser, List<CachedConfigSection>> entry : cachedConfigs.entrySet()) {
            ConfigParser parser = entry.getKey();
            if (!predicate.test(parser)) continue;
            long t1 = System.nanoTime();
            parser.preProcess();
            for (CachedConfigSection cached : entry.getValue()) {
                for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
                    String key = configEntry.getKey();
                    Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
                    try {
                        if (parser.supportsParsingObject()) {
                            // do not apply templates
                            parser.parseObject(cached.pack(), cached.filePath(), id, configEntry.getValue());
                        } else {
                            if (configEntry.getValue() instanceof Map<?, ?> configSection0) {
                                Map<String, Object> config = castToMap(configSection0, false);
                                if ((boolean) config.getOrDefault("debug", false)) {
                                    this.plugin.logger().info(GsonHelper.get().toJson(this.plugin.templateManager().applyTemplates(id, config)));
                                }
                                if ((boolean) config.getOrDefault("enable", true)) {
                                    parser.parseSection(cached.pack(), cached.filePath(), id, MiscUtils.castToMap(this.plugin.templateManager().applyTemplates(id, config), false));
                                }
                            } else {
                                TranslationManager.instance().log("warning.config.structure.not_section", cached.filePath().toString(), cached.prefix() + "." + key, configEntry.getValue().getClass().getSimpleName());
                            }
                        }
                    } catch (LocalizedException e) {
                        printWarningRecursively(e, cached.filePath(), cached.prefix() + "." + key);
                    } catch (Exception e) {
                        this.plugin.logger().warn("Unexpected error loading file " + cached.filePath() + " - '" + parser.sectionId()[0] + "." + key + "'. Please find the cause according to the stacktrace or seek developer help. Additional info: " + GsonHelper.get().toJson(configEntry.getValue()), e);
                    }
                }
            }
            parser.postProcess();
            long t2 = System.nanoTime();
            this.plugin.logger().info("Loaded " + parser.sectionId()[0] + " in " + String.format("%.2f", ((t2 - t1) / 1_000_000.0)) + " ms");
        }
    }

    private void printWarningRecursively(LocalizedException e, Path path, String prefix) {
        for (Throwable t : e.getSuppressed()) {
            if (t instanceof LocalizedException suppressed) {
                printWarningRecursively(suppressed, path, prefix);
            }
        }
        if (e instanceof LocalizedResourceConfigException exception) {
            exception.setPath(path);
            exception.setId(prefix);
        }
        TranslationManager.instance().log(e.node(), e.arguments());
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
    public void generateResourcePack() throws IOException {
        this.plugin.logger().info("Generating resource pack...");
        long time1 = System.currentTimeMillis();

        // get the target location
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            // firstly merge existing folders
            Path generatedPackPath = fs.getPath("resource_pack");
            List<Pair<String, List<Path>>> duplicated = this.updateCachedAssets(fs);
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
            this.generatePackMetadata(generatedPackPath.resolve("pack.mcmeta"), revisions);
            if (Config.excludeShaders()) {
                this.removeAllShaders(generatedPackPath);
            }
            long time2 = System.currentTimeMillis();
            this.plugin.logger().info("Generated resource pack in " + (time2 - time1) + "ms");
            if (Config.validateResourcePack()) {
                this.validateResourcePack(generatedPackPath);
            }
            long time3 = System.currentTimeMillis();
            this.plugin.logger().info("Validated resource pack in " + (time3 - time2) + "ms");
            Path finalPath = resourcePackPath();
            Files.createDirectories(finalPath.getParent());
            if (!VersionHelper.PREMIUM) {
                Config.instance().setObf(false);
                this.plugin.logger().warn("Resource pack obfuscation requires Premium Edition.");
            }
            try {
                this.zipGenerator.accept(generatedPackPath, finalPath);
            } catch (Exception e) {
                this.plugin.logger().severe("Error zipping resource pack", e);
            }
            long time4 = System.currentTimeMillis();
            this.plugin.logger().info("Created resource pack zip file in " + (time4 - time3) + "ms");
            this.eventDispatcher.accept(generatedPackPath, finalPath);
        }
    }

    private void generatePackMetadata(Path path, Set<Revision> revisions) throws IOException {
        JsonObject rawMeta;
        boolean changed = false;
        if (!Files.exists(path)) {
            rawMeta = new JsonObject();
            changed = true;
        } else {
            rawMeta = GsonHelper.readJsonFile(path).getAsJsonObject();
        }
        if (!rawMeta.has("pack")) {
            JsonObject pack = new JsonObject();
            rawMeta.add("pack", pack);
            pack.addProperty("pack_format", Config.packMinVersion().packFormat());
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", Config.packMinVersion().packFormat());
            supportedFormats.addProperty("max_inclusive", Config.packMaxVersion().packFormat());
            pack.add("supported_formats", supportedFormats);
            changed = true;
        }
        if (revisions.isEmpty()) {
            if (changed) {
                GsonHelper.writeJsonFile(rawMeta, path);
            }
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
            JsonObject formats = new JsonObject();
            entry.add("formats", formats);
            formats.addProperty("min_inclusive", revision.minPackVersion());
            formats.addProperty("max_inclusive", revision.maxPackVersion());
            entry.addProperty("directory", Config.createOverlayFolderName(revision.versionString()));
            entries.add(entry);
        }
        GsonHelper.writeJsonFile(rawMeta, path);
    }

    private void removeAllShaders(Path path) {
        List<Path> rootPaths;
        try {
            rootPaths = FileUtils.collectOverlays(path);
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

    private void processAtlas(JsonObject atlasJsonObject, BiConsumer<String, String> directory, Consumer<Key> existing, Consumer<Key> included) {
        JsonArray sources = atlasJsonObject.getAsJsonArray("sources");
        if (sources != null) {
            for (JsonElement source : sources) {
                if (!(source instanceof JsonObject sourceJson)) continue;
                String type = Optional.ofNullable(sourceJson.get("type")).map(JsonElement::getAsString).orElse(null);
                if (type == null) continue;
                switch (type) {
                    case "directory", "minecraft:directory" -> {
                        JsonElement source0 = sourceJson.get("source");
                        JsonElement prefix = sourceJson.get("prefix");
                        if (prefix == null || source0 == null) continue;
                        directory.accept(prefix.getAsString(), source0.getAsString() + "/");
                    }
                    case "single", "minecraft:single" -> {
                        JsonElement resource = sourceJson.get("resource");
                        if (resource == null) continue;
                        included.accept(Key.of(resource.getAsString()));
                    }
                    case "unstitch", "minecraft:unstitch" -> {
                        JsonElement resource = sourceJson.get("resource");
                        if (resource == null) continue;
                        included.accept(Key.of(resource.getAsString()));
                        JsonArray regions = sourceJson.getAsJsonArray("regions");
                        if (regions != null) {
                            for (JsonElement region : regions) {
                                if (!(region instanceof JsonObject regionJson)) continue;
                                JsonElement sprite = regionJson.get("sprite");
                                if (sprite == null) continue;
                                existing.accept(Key.of(sprite.getAsString()));
                            }
                        }
                    }
                    case "filter", "minecraft:filter" -> {
                        // todo filter
                    }
                    case "paletted_permutations", "minecraft:paletted_permutations" -> {
                        JsonArray textures = sourceJson.getAsJsonArray("textures");
                        if (textures == null) continue;
                        JsonObject permutationsJson = sourceJson.getAsJsonObject("permutations");
                        if (permutationsJson == null) continue;
                        String separator = sourceJson.has("separator") ? sourceJson.get("separator").getAsString() : "_";
                        List<String> permutations = new ArrayList<>(permutationsJson.keySet());
                        for (JsonElement texture : textures) {
                            if (!(texture instanceof JsonPrimitive texturePath)) continue;
                            for (String permutation : permutations) {
                                existing.accept(Key.of(texturePath.getAsString() + separator + permutation));
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void validateResourcePack(Path path) {
        Path[] rootPaths;
        try {
            rootPaths = FileUtils.collectOverlays(path).toArray(new Path[0]);
        } catch (IOException e) {
            plugin.logger().warn("Failed to collect overlays for " + path.toAbsolutePath(), e);
            return;
        }

        Multimap<Key, Key> imageToFonts = ArrayListMultimap.create(); // 图片到字体的映射
        Multimap<Key, Key> modelToItems = ArrayListMultimap.create(); // 模型到物品的映射
        Multimap<Key, String> modelToBlocks = ArrayListMultimap.create(); // 模型到方块的映射
        Multimap<Key, Key> imageToModels = ArrayListMultimap.create(); // 纹理到模型的映射
        Set<Key> collectedModels = new HashSet<>();

        Set<Key> texturesInAtlas = new HashSet<>();
        Set<Key> existingTextures = new HashSet<>(VANILLA_TEXTURES);
        Map<String, String> directoryMapper = new HashMap<>();
        processAtlas(this.vanillaAtlas, directoryMapper::put, existingTextures::add, texturesInAtlas::add);

        for (Path rootPath : rootPaths) {
            Path assetsPath = rootPath.resolve("assets");
            if (!Files.isDirectory(assetsPath)) continue;
            List<Path> namespaces;
            try {
                namespaces = FileUtils.collectNamespaces(assetsPath);
            } catch (IOException e) {
                plugin.logger().warn("Failed to collect namespaces for " + assetsPath.toAbsolutePath(), e);
                return;
            }
            for (Path namespacePath : namespaces) {
                Path atlasesFile = namespacePath.resolve("atlases").resolve("blocks.json");
                if (Files.exists(atlasesFile)) {
                    try {
                        JsonObject atlasJsonObject = GsonHelper.readJsonFile(atlasesFile).getAsJsonObject();
                        processAtlas(atlasJsonObject, directoryMapper::put, existingTextures::add, texturesInAtlas::add);
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", atlasesFile.toAbsolutePath().toString());
                    }
                }

                Path fontPath = namespacePath.resolve("font");
                if (Files.isDirectory(fontPath)) {
                    try {
                        Files.walkFileTree(fontPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)  {
                                if (!isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject fontJson;
                                try {
                                    fontJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                JsonArray providers = fontJson.getAsJsonArray("providers");
                                if (providers != null) {
                                    Key fontName = Key.of(namespacePath.getFileName().toString(), FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                    for (JsonElement provider : providers) {
                                        if (provider instanceof JsonObject providerJO && providerJO.has("type")) {
                                            String type = providerJO.get("type").getAsString();
                                            if (type.equals("bitmap") && providerJO.has("file")) {
                                                String pngFile = providerJO.get("file").getAsString();
                                                Key resourceLocation = Key.of(FileUtils.pathWithoutExtension(pngFile));
                                                imageToFonts.put(resourceLocation, fontName);
                                            }
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        plugin.logger().warn("Failed to validate font", e);
                    }
                }

                Path itemsPath = namespacePath.resolve("items");
                if (Files.isDirectory(itemsPath)) {
                    try {
                        Files.walkFileTree(itemsPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!isJsonFile(file)) return FileVisitResult.CONTINUE;
                                JsonObject itemJson;
                                try {
                                    itemJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                Key item = Key.of(namespacePath.getFileName().toString(), FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                collectItemModelsDeeply(itemJson, (resourceLocation) -> modelToItems.put(resourceLocation, item));
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        plugin.logger().warn("Failed to validate items", e);
                    }
                }

                Path blockStatesPath = namespacePath.resolve("blockstates");
                if (Files.isDirectory(blockStatesPath)) {
                    try {
                        Files.walkFileTree(blockStatesPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!isJsonFile(file)) return FileVisitResult.CONTINUE;
                                String blockId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                                JsonObject blockStateJson;
                                try {
                                    blockStateJson = GsonHelper.readJsonFile(file).getAsJsonObject();
                                } catch (IOException | JsonParseException e) {
                                    TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", file.toAbsolutePath().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                if (blockStateJson.has("multipart")) {
                                    collectMultipart(blockStateJson.getAsJsonArray("multipart"), (location) -> modelToBlocks.put(location, blockId));
                                } else if (blockStateJson.has("variants")) {
                                    collectVariants(blockId, blockStateJson.getAsJsonObject("variants"), modelToBlocks::put);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        plugin.logger().warn("Failed to validate blockstates", e);
                    }
                }
            }
        }

        // 验证font的贴图是否存在
        label: for (Map.Entry<Key, Collection<Key>> entry : imageToFonts.asMap().entrySet()) {
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

        // 验证物品模型是否存在，验证的同时去收集贴图
        label: for (Map.Entry<Key, Collection<Key>> entry : modelToItems.asMap().entrySet()) {
            Key modelResourceLocation = entry.getKey();
            boolean alreadyChecked = !collectedModels.add(modelResourceLocation);
            if (alreadyChecked || VANILLA_MODELS.contains(modelResourceLocation)) continue;
            String modelPath = "assets/" + modelResourceLocation.namespace() + "/models/" + modelResourceLocation.value() + ".json";
            for (Path rootPath : rootPaths) {
                Path modelJsonPath = rootPath.resolve(modelPath);
                if (Files.exists(rootPath.resolve(modelPath))) {
                    JsonObject modelJson;
                    try {
                        modelJson = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                        continue;
                    }
                    verifyParentModelAndCollectTextures(modelResourceLocation, modelJson, rootPaths, imageToModels, collectedModels);
                    continue label;
                }
            }
            TranslationManager.instance().log("warning.config.resource_pack.generation.missing_item_model", entry.getValue().stream().distinct().toList().toString(), modelPath);
        }

        // 验证方块模型是否存在，验证的同时去收集贴图
        label: for (Map.Entry<Key, Collection<String>> entry : modelToBlocks.asMap().entrySet()) {
            Key modelResourceLocation = entry.getKey();
            boolean alreadyChecked = !collectedModels.add(modelResourceLocation);
            if (alreadyChecked || VANILLA_MODELS.contains(modelResourceLocation)) continue;
            String modelPath = "assets/" + modelResourceLocation.namespace() + "/models/" + modelResourceLocation.value() + ".json";
            for (Path rootPath : rootPaths) {
                Path modelJsonPath = rootPath.resolve(modelPath);
                if (Files.exists(modelJsonPath)) {
                    JsonObject jsonObject;
                    try {
                        jsonObject = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                    } catch (IOException | JsonParseException e) {
                        TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                        continue;
                    }
                    verifyParentModelAndCollectTextures(modelResourceLocation, jsonObject, rootPaths, imageToModels, collectedModels);
                    continue label;
                }
            }
            TranslationManager.instance().log("warning.config.resource_pack.generation.missing_block_model", entry.getValue().stream().distinct().toList().toString(), modelPath);
        }

        // 验证贴图是否存在
        boolean enableObf = Config.enableObfuscation() && Config.enableRandomResourceLocation();
        label: for (Map.Entry<Key, Collection<Key>> entry : imageToModels.asMap().entrySet()) {
            Key key = entry.getKey();
            // 已经存在的贴图，直接过滤
            if (existingTextures.contains(key)) continue;
            // 直接在single中被指定的贴图，只检测是否存在
            if (enableObf || texturesInAtlas.contains(key)) {
                String imagePath = "assets/" + key.namespace() + "/textures/" + key.value() + ".png";
                for (Path rootPath : rootPaths) {
                    if (Files.exists(rootPath.resolve(imagePath))) {
                        continue label;
                    }
                }
                TranslationManager.instance().log("warning.config.resource_pack.generation.missing_model_texture", entry.getValue().stream().distinct().toList().toString(), imagePath);
            } else {
                for (Map.Entry<String, String> directorySource : directoryMapper.entrySet()) {
                    String prefix = directorySource.getKey();
                    if (key.value().startsWith(prefix)) {
                        String imagePath = "assets/" + key.namespace() + "/textures/" + directorySource.getValue() + key.value().substring(prefix.length()) + ".png";
                        for (Path rootPath : rootPaths) {
                            if (Files.exists(rootPath.resolve(imagePath))) {
                                continue label;
                            }
                        }
                        TranslationManager.instance().log("warning.config.resource_pack.generation.missing_model_texture", entry.getValue().stream().distinct().toList().toString(), imagePath);
                        continue label;
                    }
                }
                TranslationManager.instance().log("warning.config.resource_pack.generation.texture_not_in_atlas", key.toString());
            }
        }
    }

    private void verifyParentModelAndCollectTextures(Key sourceModelLocation, JsonObject sourceModelJson, Path[] rootPaths, Multimap<Key, Key> imageToModels, Set<Key> collected) {
        if (sourceModelJson.has("parent")) {
            Key parentResourceLocation = Key.from(sourceModelJson.get("parent").getAsString());
            if (collected.add(parentResourceLocation) && !VANILLA_MODELS.contains(parentResourceLocation)) {
                String parentModelPath = "assets/" + parentResourceLocation.namespace() + "/models/" + parentResourceLocation.value() + ".json";
                label: {
                    for (Path rootPath : rootPaths) {
                        Path modelJsonPath = rootPath.resolve(parentModelPath);
                        if (Files.exists(modelJsonPath)) {
                            JsonObject jsonObject;
                            try {
                                jsonObject = GsonHelper.readJsonFile(modelJsonPath).getAsJsonObject();
                            } catch (IOException | JsonParseException e) {
                                TranslationManager.instance().log("warning.config.resource_pack.generation.malformatted_json", modelJsonPath.toAbsolutePath().toString());
                                break label;
                            }
                            verifyParentModelAndCollectTextures(parentResourceLocation, jsonObject, rootPaths, imageToModels, collected);
                            break label;
                        }
                    }
                    TranslationManager.instance().log("warning.config.resource_pack.generation.missing_parent_model", sourceModelLocation.asString(), parentModelPath);
                }
            }
        }
        if (sourceModelJson.has("textures")) {
            JsonObject textures = sourceModelJson.get("textures").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String value = entry.getValue().getAsString();
                if (value.charAt(0) == '#') continue;
                Key textureResourceLocation = Key.from(value);
                imageToModels.put(textureResourceLocation, sourceModelLocation);
            }
        }
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

    private static boolean isJsonFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName.endsWith(".json") || fileName.endsWith(".mcmeta");
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
        if (Config.packMaxVersion().isBelow(MinecraftVersions.V1_21_5)) return;
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

    private void generateEquipments(Path generatedPackPath, Consumer<Revision> callback) {
        // asset id + 是否有上身 + 是否有腿
        List<Tuple<Key, Boolean, Boolean>> collectedTrims = new ArrayList<>();

        // 为trim类型提供的两个兼容性值
        boolean needLegacyCompatibility = Config.packMinVersion().isBelow(MinecraftVersions.V1_21_2);
        boolean needModernCompatibility = Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2);

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
                } catch (Exception ignored) {
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
        if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_4)) {
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
        if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2) && Config.packMinVersion().isBelow(MinecraftVersions.V1_21_4)) {
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
            if (Config.packMinVersion().isBelow(MinecraftVersions.V1_21_2)) {
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
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2)) {
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
            if (Config.packMinVersion().isBelow(MinecraftVersions.V1_21_2)) {
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
            if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2)) {
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

    private void generateClientLang(Path generatedPackPath) {
        for (Map.Entry<String, I18NData> entry : this.plugin.translationManager().clientLangData().entrySet()) {
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
        if (Config.packMaxVersion().isBelow(MinecraftVersions.V1_21_2)) return;
        if (Config.packMinVersion().isAtOrAbove(MinecraftVersions.V1_21_4)) return;

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

    private void generateModernItemModels1_21_4(Path generatedPackPath, Consumer<Revision> callback) {
        if (Config.packMaxVersion().isBelow(MinecraftVersions.V1_21_4)) return;
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
        if (Config.packMaxVersion().isBelow(MinecraftVersions.V1_21_4)) return;
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

            Map<Float, ItemModel> entries = new HashMap<>();
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

            ModernItemModel newItemModel = new ModernItemModel(rangeDispatch, handAnimationOnSwap, oversizedInGui);
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
        if (Config.packMinVersion().isAtOrAbove(MinecraftVersions.V1_21_4)) return;
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

    private List<Pair<String, List<Path>>> updateCachedAssets(@Nullable FileSystem fs) throws IOException {
        Map<String, List<Path>> conflictChecker = new Object2ObjectOpenHashMap<>(Math.max(128, this.cachedAssetFiles.size()));
        Map<Path, CachedAssetFile> previousFiles = this.cachedAssetFiles;
        this.cachedAssetFiles = new Object2ObjectOpenHashMap<>(Math.max(128, this.cachedAssetFiles.size()));

        List<Path> folders = new ArrayList<>();
        folders.addAll(loadedPacks().stream()
                .filter(Pack::enabled)
                .map(Pack::resourcePackFolder)
                .toList());
        folders.addAll(Config.foldersToMerge().stream()
                .map(it -> this.plugin.dataFolderPath().getParent().resolve(it))
                .filter(Files::exists)
                .toList());
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
        List<Path> externalZips = Config.zipsToMerge().stream()
                .map(it -> this.plugin.dataFolderPath().getParent().resolve(it))
                .filter(Files::exists)
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().endsWith(".zip"))
                .toList();
        for (Path zip : externalZips) {
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
                                Map<String, List<Path>> conflictChecker, Map<Path, CachedAssetFile> previousFiles) throws IOException {
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
                        updateConflictChecker(fs, conflictChecker, entry, sourcePath, entryPathInZip, cachedAsset.data());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
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
            for (ResolutionConditional resolution : Config.resolutions()) {
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
}
