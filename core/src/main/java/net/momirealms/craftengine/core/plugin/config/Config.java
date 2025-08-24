package net.momirealms.craftengine.core.plugin.config;

import com.google.common.collect.ImmutableMap;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.furniture.ColliderType;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionConditional;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.filter.DisconnectLogFilter;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.InjectionTarget;
import net.momirealms.craftengine.core.world.chunk.storage.CompressionMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    private static Config instance;
    protected final CraftEngine plugin;
    private final Path configFilePath;
    private final String configVersion;
    private YamlDocument config;
    private long lastModified;
    private long size;

    protected boolean firstTime = true;
    protected boolean checkUpdate;
    protected boolean metrics;
    protected boolean filterConfigurationPhaseDisconnect;

    protected boolean debug$common;
    protected boolean debug$packet;
    protected boolean debug$item;
    protected boolean debug$furniture;
    protected boolean debug$resource_pack;

    protected boolean resource_pack$remove_tinted_leaves_particle;
    protected boolean resource_pack$generate_mod_assets;
    protected boolean resource_pack$override_uniform_font;
    protected List<ResolutionConditional> resource_pack$duplicated_files_handler;
    protected List<String> resource_pack$merge_external_folders;
    protected List<String> resource_pack$merge_external_zips;
    protected Set<String> resource_pack$exclude_file_extensions;

    protected boolean resource_pack$protection$crash_tools$method_1;
    protected boolean resource_pack$protection$crash_tools$method_2;
    protected boolean resource_pack$protection$crash_tools$method_3;

    protected boolean resource_pack$validate$enable;
    protected boolean resource_pack$exclude_core_shaders;

    protected boolean resource_pack$protection$obfuscation$enable;
    protected long resource_pack$protection$obfuscation$seed;
    protected boolean resource_pack$protection$obfuscation$fake_directory;
    protected boolean resource_pack$protection$obfuscation$escape_unicode;
    protected boolean resource_pack$protection$obfuscation$break_json;
    protected boolean resource_pack$protection$obfuscation$resource_location$enable;
    protected int resource_pack$protection$obfuscation$resource_location$random_namespace$length;
    protected int resource_pack$protection$obfuscation$resource_location$random_namespace$amount;
    protected String resource_pack$protection$obfuscation$resource_location$random_path$source;
    protected int resource_pack$protection$obfuscation$resource_location$random_path$depth;
    protected boolean resource_pack$protection$obfuscation$resource_location$random_path$anti_unzip;
    protected int resource_pack$protection$obfuscation$resource_location$random_atlas$images_per_canvas;
    protected boolean resource_pack$protection$obfuscation$resource_location$random_atlas$use_double;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_textures;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_models;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_sounds;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_equipments;

    protected MinecraftVersion resource_pack$supported_version$min;
    protected MinecraftVersion resource_pack$supported_version$max;
    protected String resource_pack$overlay_format;

    protected boolean resource_pack$delivery$kick_if_declined;
    protected boolean resource_pack$delivery$kick_if_failed_to_apply;
    protected boolean resource_pack$delivery$send_on_join;
    protected boolean resource_pack$delivery$resend_on_upload;
    protected boolean resource_pack$delivery$auto_upload;
    protected Path resource_pack$delivery$file_to_upload;
    protected Component resource_pack$send$prompt;

    protected boolean light_system$force_update_light;
    protected boolean light_system$enable;

    protected int chunk_system$compression_method;
    protected boolean chunk_system$restore_vanilla_blocks_on_chunk_unload;
    protected boolean chunk_system$restore_custom_blocks_on_chunk_load;
    protected boolean chunk_system$sync_custom_blocks_on_chunk_load;
    protected boolean chunk_system$cache_system;
    protected boolean chunk_system$injection$use_fast_method;
    protected boolean chunk_system$injection$target;
    protected boolean chunk_system$process_invalid_furniture$enable;
    protected Map<String, String> chunk_system$process_invalid_furniture$mapping;
    protected boolean chunk_system$process_invalid_blocks$enable;
    protected Map<String, String> chunk_system$process_invalid_blocks$mapping;

    protected boolean furniture$hide_base_entity;
    protected ColliderType furniture$collision_entity_type;

    protected boolean block$sound_system$enable;
    protected boolean block$simplify_adventure_break_check;
    protected boolean block$simplify_adventure_place_check;
    protected boolean block$predict_breaking;
    protected int block$predict_breaking_interval;
    protected double block$extended_interaction_range;

    protected boolean recipe$enable;
    protected boolean recipe$disable_vanilla_recipes$all;
    protected Set<Key> recipe$disable_vanilla_recipes$list;
    protected List<String> recipe$ingredient_sources;

    protected boolean image$illegal_characters_filter$command;
    protected boolean image$illegal_characters_filter$chat;
    protected boolean image$illegal_characters_filter$anvil;
    protected boolean image$illegal_characters_filter$sign;
    protected boolean image$illegal_characters_filter$book;
    protected boolean image$intercept_packets$system_chat;
    protected boolean image$intercept_packets$tab_list;
    protected boolean image$intercept_packets$actionbar;
    protected boolean image$intercept_packets$title;
    protected boolean image$intercept_packets$bossbar;
    protected boolean image$intercept_packets$container;
    protected boolean image$intercept_packets$team;
    protected boolean image$intercept_packets$scoreboard;
    protected boolean image$intercept_packets$entity_name;
    protected boolean image$intercept_packets$text_display;
    protected boolean image$intercept_packets$armor_stand;
    protected boolean image$intercept_packets$player_info;
    protected boolean image$intercept_packets$set_score;
    protected boolean image$intercept_packets$item;
    protected boolean image$intercept_packets$advancement;

    protected boolean item$client_bound_model;
    protected boolean item$non_italic_tag;
    protected boolean item$update_triggers$attack;
    protected boolean item$update_triggers$click_in_inventory;
    protected boolean item$update_triggers$drop;
    protected boolean item$update_triggers$pick_up;

    protected String equipment$sacrificed_vanilla_armor$type;
    protected Key equipment$sacrificed_vanilla_armor$asset_id;
    protected Key equipment$sacrificed_vanilla_armor$humanoid;
    protected Key equipment$sacrificed_vanilla_armor$humanoid_leggings;

    protected boolean emoji$contexts$chat;
    protected boolean emoji$contexts$book;
    protected boolean emoji$contexts$anvil;
    protected boolean emoji$contexts$sign;
    protected int emoji$max_emojis_per_parse;

    public Config(CraftEngine plugin) {
        this.plugin = plugin;
        this.configVersion = PluginProperties.getValue("config");
        this.configFilePath = this.plugin.dataFolderPath().resolve("config.yml");
        instance = this;
    }

    public void load() {
        // 文件不存在，则保存
        if (!Files.exists(this.configFilePath)) {
            this.plugin.saveResource("config.yml");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(this.configFilePath, BasicFileAttributes.class);
            long lastModified = attributes.lastModifiedTime().toMillis();
            long size = attributes.size();
            if (lastModified != this.lastModified || size != this.size || this.config == null) {
                byte[] configFileBytes = Files.readAllBytes(this.configFilePath);
                try (InputStream inputStream = new ByteArrayInputStream(configFileBytes)) {
                    this.config = YamlDocument.create(inputStream);
                    String configVersion = this.config.getString("config-version");
                    if (!configVersion.equals(this.configVersion)) {
                        this.updateConfigVersion(configFileBytes);
                    }
                }
                // 加载配置文件
                this.loadSettings();
                this.lastModified = lastModified;
                this.size = size;
            }
        } catch (IOException e) {
            this.plugin.logger().severe("Failed to load config.yml", e);
        }
    }

    private void updateConfigVersion(byte[] bytes) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            this.config = YamlDocument.create(inputStream, this.plugin.resourceStream("config.yml"), GeneralSettings.builder()
                            .setRouteSeparator('.')
                            .setUseDefaults(false)
                            .build(),
                    LoaderSettings
                            .builder()
                            .setAutoUpdate(true)
                            .build(),
                    DumperSettings.builder()
                            .setEscapeUnprintable(false)
                            .setScalarFormatter((tag, value, role, def) -> {
                                if (role == NodeRole.KEY) {
                                    return ScalarStyle.PLAIN;
                                } else {
                                    return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
                                }
                            })
                            .build(),
                    UpdaterSettings
                            .builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .addIgnoredRoute(PluginProperties.getValue("config"), "resource-pack.delivery.hosting", '.')
                            .build());
        }
        try {
            this.config.save(new File(plugin.dataFolderFile(), "config.yml"));
        } catch (IOException e) {
            this.plugin.logger().warn("Could not save config.yml", e);
        }
    }

    private void loadSettings() {
        YamlDocument config = settings();
        plugin.translationManager().forcedLocale(TranslationManager.parseLocale(config.getString("forced-locale", "")));

        // basics
        metrics = config.getBoolean("metrics", false);
        checkUpdate = config.getBoolean("update-checker", false);
        filterConfigurationPhaseDisconnect = config.getBoolean("filter-configuration-phase-disconnect", false);
        DisconnectLogFilter.instance().setEnable(filterConfigurationPhaseDisconnect);

        // debug
        debug$common = config.getBoolean("debug.common", false);
        debug$packet = config.getBoolean("debug.packet", false);
        debug$item = config.getBoolean("debug.item", false);
        debug$furniture = config.getBoolean("debug.furniture", false);
        debug$resource_pack = config.getBoolean("debug.resource-pack", false);

        // resource pack
        resource_pack$override_uniform_font = config.getBoolean("resource-pack.override-uniform-font", false);
        resource_pack$generate_mod_assets = config.getBoolean("resource-pack.generate-mod-assets", false);
        resource_pack$remove_tinted_leaves_particle = config.getBoolean("resource-pack.remove-tinted-leaves-particle", true);
        resource_pack$supported_version$min = getVersion(config.get("resource-pack.supported-version.min", "SERVER").toString());
        resource_pack$supported_version$max = getVersion(config.get("resource-pack.supported-version.max", "LATEST").toString());
        if (resource_pack$supported_version$min.isAbove(resource_pack$supported_version$max)) {
            resource_pack$supported_version$min = resource_pack$supported_version$max;
        }
        resource_pack$merge_external_folders = config.getStringList("resource-pack.merge-external-folders");
        resource_pack$merge_external_zips = config.getStringList("resource-pack.merge-external-zip-files");
        resource_pack$exclude_file_extensions = new HashSet<>(config.getStringList("resource-pack.exclude-file-extensions"));
        resource_pack$delivery$send_on_join = config.getBoolean("resource-pack.delivery.send-on-join", true);
        resource_pack$delivery$resend_on_upload = config.getBoolean("resource-pack.delivery.resend-on-upload", true);
        resource_pack$delivery$kick_if_declined = config.getBoolean("resource-pack.delivery.kick-if-declined", true);
        resource_pack$delivery$kick_if_failed_to_apply = config.getBoolean("resource-pack.delivery.kick-if-failed-to-apply", true);
        resource_pack$delivery$auto_upload = config.getBoolean("resource-pack.delivery.auto-upload", true);
        resource_pack$delivery$file_to_upload = resolvePath(config.getString("resource-pack.delivery.file-to-upload", "./generated/resource_pack.zip"));
        resource_pack$send$prompt = AdventureHelper.miniMessage().deserialize(config.getString("resource-pack.delivery.prompt", "<yellow>To fully experience our server, please accept our custom resource pack.</yellow>"));
        resource_pack$protection$crash_tools$method_1 = config.getBoolean("resource-pack.protection.crash-tools.method-1", false);
        resource_pack$protection$crash_tools$method_2 = config.getBoolean("resource-pack.protection.crash-tools.method-2", false);
        resource_pack$protection$crash_tools$method_3 = config.getBoolean("resource-pack.protection.crash-tools.method-3", false);
        resource_pack$protection$obfuscation$enable = config.getBoolean("resource-pack.protection.obfuscation.enable", false);
        resource_pack$protection$obfuscation$seed = config.getLong("resource-pack.protection.obfuscation.seed", 0L);
        resource_pack$protection$obfuscation$fake_directory = config.getBoolean("resource-pack.protection.obfuscation.fake-directory", false);
        resource_pack$protection$obfuscation$escape_unicode = config.getBoolean("resource-pack.protection.obfuscation.escape-unicode", false);
        resource_pack$protection$obfuscation$break_json = config.getBoolean("resource-pack.protection.obfuscation.break-json", false);
        resource_pack$protection$obfuscation$resource_location$enable = config.getBoolean("resource-pack.protection.obfuscation.resource-location.enable", false);
        resource_pack$protection$obfuscation$resource_location$random_namespace$amount = config.getInt("resource-pack.protection.obfuscation.resource-location.random-namespace.amount", 32);
        resource_pack$protection$obfuscation$resource_location$random_namespace$length = config.getInt("resource-pack.protection.obfuscation.resource-location.random-namespace.length", 8);
        resource_pack$protection$obfuscation$resource_location$random_path$depth = config.getInt("resource-pack.protection.obfuscation.resource-location.random-path.depth", 16);
        resource_pack$protection$obfuscation$resource_location$random_path$source = config.getString("resource-pack.protection.obfuscation.resource-location.random-path.source", "obf");
        resource_pack$protection$obfuscation$resource_location$random_path$anti_unzip = config.getBoolean("resource-pack.protection.obfuscation.resource-location.random-path.anti-unzip", false);
        resource_pack$protection$obfuscation$resource_location$random_atlas$images_per_canvas = config.getInt("resource-pack.protection.obfuscation.resource-location.random-atlas.images-per-canvas", 16);
        resource_pack$protection$obfuscation$resource_location$random_atlas$use_double = config.getBoolean("resource-pack.protection.obfuscation.resource-location.random-atlas.use-double", true);
        resource_pack$protection$obfuscation$resource_location$bypass_textures = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-textures");
        resource_pack$protection$obfuscation$resource_location$bypass_models = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-models");
        resource_pack$protection$obfuscation$resource_location$bypass_sounds = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-sounds");
        resource_pack$protection$obfuscation$resource_location$bypass_equipments = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-equipments");
        resource_pack$validate$enable = config.getBoolean("resource-pack.validate.enable", true);
        resource_pack$exclude_core_shaders = config.getBoolean("resource-pack.exclude-core-shaders", false);
        resource_pack$overlay_format = config.getString("resource-pack.overlay-format", "overlay_{version}");
        if (!resource_pack$overlay_format.contains("{version}")) {
            TranslationManager.instance().log("warning.config.resource_pack.invalid_overlay_format", resource_pack$overlay_format);
        }

        try {
            resource_pack$duplicated_files_handler = config.getMapList("resource-pack.duplicated-files-handler").stream().map(it -> {
                Map<String, Object> args = MiscUtils.castToMap(it, false);
                return ResolutionConditional.FACTORY.create(args);
            }).toList();
        } catch (LocalizedResourceConfigException e) {
            TranslationManager.instance().log(e.node(), e.arguments());
            resource_pack$duplicated_files_handler = List.of();
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to load resource-pack.duplicated-files-handler", e);
            resource_pack$duplicated_files_handler = List.of();
        }

        // light
        light_system$force_update_light = config.getBoolean("light-system.force-update-light", false);
        light_system$enable = config.getBoolean("light-system.enable", true);

        // chunk
        chunk_system$compression_method = config.getInt("chunk-system.compression-method", 4);
        chunk_system$restore_vanilla_blocks_on_chunk_unload = config.getBoolean("chunk-system.restore-vanilla-blocks-on-chunk-unload", true);
        chunk_system$restore_custom_blocks_on_chunk_load = config.getBoolean("chunk-system.restore-custom-blocks-on-chunk-load", true);
        chunk_system$sync_custom_blocks_on_chunk_load = config.getBoolean("chunk-system.sync-custom-blocks-on-chunk-load", false);
        chunk_system$cache_system = config.getBoolean("chunk-system.cache-system", true);
        chunk_system$injection$use_fast_method = config.getBoolean("chunk-system.injection.use-fast-method", false);
        if (firstTime) {
            chunk_system$injection$target = config.getEnum("chunk-system.injection.target", InjectionTarget.class, InjectionTarget.PALETTE) == InjectionTarget.PALETTE;
        }

        chunk_system$process_invalid_furniture$enable = config.getBoolean("chunk-system.process-invalid-furniture.enable", false);
        ImmutableMap.Builder<String, String> furnitureBuilder = ImmutableMap.builder();
        for (String furniture : config.getStringList("chunk-system.process-invalid-furniture.remove")) {
            furnitureBuilder.put(furniture, "");
        }
        if (config.contains("chunk-system.process-invalid-furniture.convert")) {
            Section section = config.getSection("chunk-system.process-invalid-furniture.convert");
            if (section != null) {
                for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
                    furnitureBuilder.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        chunk_system$process_invalid_furniture$mapping = furnitureBuilder.build();

        chunk_system$process_invalid_blocks$enable = config.getBoolean("chunk-system.process-invalid-blocks.enable", false);
        ImmutableMap.Builder<String, String> blockBuilder = ImmutableMap.builder();
        for (String furniture : config.getStringList("chunk-system.process-invalid-blocks.remove")) {
            blockBuilder.put(furniture, "");
        }
        if (config.contains("chunk-system.process-invalid-blocks.convert")) {
            Section section = config.getSection("chunk-system.process-invalid-blocks.convert");
            if (section != null) {
                for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
                    blockBuilder.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        chunk_system$process_invalid_blocks$mapping = blockBuilder.build();

        // furniture
        furniture$hide_base_entity = config.getBoolean("furniture.hide-base-entity", true);
        furniture$collision_entity_type = ColliderType.valueOf(config.getString("furniture.collision-entity-type", "interaction").toUpperCase(Locale.ENGLISH));

        // equipment
        equipment$sacrificed_vanilla_armor$type = config.getString("equipment.sacrificed-vanilla-armor.type", "chainmail").toLowerCase(Locale.ENGLISH);
        if (!AbstractPackManager.ALLOWED_VANILLA_EQUIPMENT.contains(equipment$sacrificed_vanilla_armor$type)) {
            TranslationManager.instance().log("warning.config.equipment.invalid_sacrificed_armor", equipment$sacrificed_vanilla_armor$type);
            equipment$sacrificed_vanilla_armor$type = "chainmail";
        }

        equipment$sacrificed_vanilla_armor$asset_id = Key.of(config.getString("equipment.sacrificed-vanilla-armor.asset-id", "minecraft:chainmail"));
        equipment$sacrificed_vanilla_armor$humanoid = Key.of(config.getString("equipment.sacrificed-vanilla-armor.humanoid", "minecraft:trims/entity/humanoid/chainmail"));
        equipment$sacrificed_vanilla_armor$humanoid_leggings = Key.of(config.getString("equipment.sacrificed-vanilla-armor.humanoid-leggings", "minecraft:trims/entity/humanoid_leggings/chainmail"));

        // item
        item$client_bound_model = config.getBoolean("item.client-bound-model", false);
        item$non_italic_tag = config.getBoolean("item.non-italic-tag", false);
        item$update_triggers$attack = config.getBoolean("item.update-triggers.attack", false);
        item$update_triggers$click_in_inventory = config.getBoolean("item.update-triggers.click-in-inventory", false);
        item$update_triggers$drop = config.getBoolean("item.update-triggers.drop", false);
        item$update_triggers$pick_up = config.getBoolean("item.update-triggers.pick-up", false);

        // block
        block$sound_system$enable = config.getBoolean("block.sound-system.enable", true);
        block$simplify_adventure_break_check = config.getBoolean("block.simplify-adventure-break-check", false);
        block$simplify_adventure_place_check = config.getBoolean("block.simplify-adventure-place-check", false);
        block$predict_breaking = config.getBoolean("block.predict-breaking.enable", true);
        block$predict_breaking_interval = Math.max(config.getInt("block.predict-breaking.interval", 10), 1);
        block$extended_interaction_range = Math.max(config.getDouble("block.predict-breaking.extended-interaction-range", 0.5), 0.0);

        // recipe
        recipe$enable = config.getBoolean("recipe.enable", true);
        recipe$disable_vanilla_recipes$all = config.getBoolean("recipe.disable-vanilla-recipes.all", false);
        recipe$disable_vanilla_recipes$list = config.getStringList("recipe.disable-vanilla-recipes.list").stream().map(Key::of).collect(Collectors.toSet());
        recipe$ingredient_sources = config.getStringList("recipe.ingredient-sources");

        // image
        image$illegal_characters_filter$anvil = config.getBoolean("image.illegal-characters-filter.anvil", true);
        image$illegal_characters_filter$book = config.getBoolean("image.illegal-characters-filter.book", true);
        image$illegal_characters_filter$chat = config.getBoolean("image.illegal-characters-filter.chat", true);
        image$illegal_characters_filter$command = config.getBoolean("image.illegal-characters-filter.command", true);
        image$illegal_characters_filter$sign = config.getBoolean("image.illegal-characters-filter.sign", true);
        image$intercept_packets$system_chat = config.getBoolean("image.intercept-packets.system-chat", true);
        image$intercept_packets$tab_list = config.getBoolean("image.intercept-packets.tab-list", true);
        image$intercept_packets$actionbar = config.getBoolean("image.intercept-packets.actionbar", true);
        image$intercept_packets$title = config.getBoolean("image.intercept-packets.title", true);
        image$intercept_packets$bossbar = config.getBoolean("image.intercept-packets.bossbar", true);
        image$intercept_packets$container = config.getBoolean("image.intercept-packets.container", true);
        image$intercept_packets$team = config.getBoolean("image.intercept-packets.team", true);
        image$intercept_packets$scoreboard = config.getBoolean("image.intercept-packets.scoreboard", true);
        image$intercept_packets$entity_name = config.getBoolean("image.intercept-packets.entity-name", false);
        image$intercept_packets$text_display = config.getBoolean("image.intercept-packets.text-display", true);
        image$intercept_packets$armor_stand = config.getBoolean("image.intercept-packets.armor-stand", true);
        image$intercept_packets$player_info = config.getBoolean("image.intercept-packets.player-info", true);
        image$intercept_packets$set_score = config.getBoolean("image.intercept-packets.set-score", true);
        image$intercept_packets$item = config.getBoolean("image.intercept-packets.item", true);
        image$intercept_packets$advancement = config.getBoolean("image.intercept-packets.advancement", true);

        // emoji
        emoji$contexts$chat = config.getBoolean("emoji.contexts.chat", true);
        emoji$contexts$anvil = config.getBoolean("emoji.contexts.anvil", true);
        emoji$contexts$book = config.getBoolean("emoji.contexts.book", true);
        emoji$contexts$sign = config.getBoolean("emoji.contexts.sign", true);
        emoji$max_emojis_per_parse = config.getInt("emoji.max-emojis-per-parse", 32);

        firstTime = false;
    }

    private static MinecraftVersion getVersion(String version) {
        if (version.equalsIgnoreCase("LATEST")) {
            return new MinecraftVersion(PluginProperties.getValue("latest-version"));
        }
        if (version.equalsIgnoreCase("SERVER")) {
            return VersionHelper.MINECRAFT_VERSION;
        }
        return MinecraftVersion.parse(version);
    }

    public static String configVersion() {
        return instance.configVersion;
    }

    public static boolean debugCommon() {
        return instance.debug$common;
    }

    public static boolean debugPacket() {
        return instance.debug$packet;
    }

    public static boolean debugItem() {
        return instance.debug$item;
    }

    public static boolean debugFurniture() {
        return instance.debug$furniture;
    }

    public static boolean debugResourcePack() {
        return instance.debug$resource_pack;
    }

    public static boolean checkUpdate() {
        return instance.checkUpdate;
    }

    public static boolean metrics() {
        return instance.metrics;
    }

    public static boolean filterConfigurationPhaseDisconnect() {
        return instance.filterConfigurationPhaseDisconnect;
    }

    public static boolean resourcePack$overrideUniform() {
        return instance.resource_pack$override_uniform_font;
    }

    public static int maxNoteBlockChainUpdate() {
        return 64;
    }

    public static int maxEmojisPerParse() {
        return instance.emoji$max_emojis_per_parse;
    }

    public static boolean handleInvalidFurniture() {
        return instance.chunk_system$process_invalid_furniture$enable;
    }

    public static boolean handleInvalidBlock() {
        return instance.chunk_system$process_invalid_blocks$enable;
    }

    public static Map<String, String> furnitureMappings() {
        return instance.chunk_system$process_invalid_furniture$mapping;
    }

    public static Map<String, String> blockMappings() {
        return instance.chunk_system$process_invalid_blocks$mapping;
    }

    public static boolean enableLightSystem() {
        return instance.light_system$enable;
    }

    public static MinecraftVersion packMinVersion() {
        return instance.resource_pack$supported_version$min;
    }

    public static MinecraftVersion packMaxVersion() {
        return instance.resource_pack$supported_version$max;
    }

    public static boolean enableSoundSystem() {
        return instance.block$sound_system$enable;
    }

    public static boolean simplifyAdventureBreakCheck() {
        return instance.block$simplify_adventure_break_check;
    }

    public static boolean simplifyAdventurePlaceCheck() {
        return instance.block$simplify_adventure_place_check;
    }

    public static boolean enableRecipeSystem() {
        return instance.recipe$enable;
    }

    public static boolean disableAllVanillaRecipes() {
        return instance.recipe$disable_vanilla_recipes$all;
    }

    public static Set<Key> disabledVanillaRecipes() {
        return instance.recipe$disable_vanilla_recipes$list;
    }

    public static boolean restoreVanillaBlocks() {
        return instance.chunk_system$restore_vanilla_blocks_on_chunk_unload && instance.chunk_system$restore_custom_blocks_on_chunk_load;
    }

    public static boolean restoreCustomBlocks() {
        return instance.chunk_system$restore_custom_blocks_on_chunk_load;
    }

    public static boolean syncCustomBlocks() {
        return instance.chunk_system$sync_custom_blocks_on_chunk_load;
    }

    public static List<String> foldersToMerge() {
        return instance.resource_pack$merge_external_folders;
    }

    public static List<String> zipsToMerge() {
        return instance.resource_pack$merge_external_zips;
    }

    public static Set<String> excludeFileExtensions() {
        return instance.resource_pack$exclude_file_extensions;
    }

    public static boolean kickOnDeclined() {
        return instance.resource_pack$delivery$kick_if_declined;
    }

    public static boolean kickOnFailedApply() {
        return instance.resource_pack$delivery$kick_if_failed_to_apply;
    }

    public static Component resourcePackPrompt() {
        return instance.resource_pack$send$prompt;
    }

    public static boolean sendPackOnJoin() {
        return instance.resource_pack$delivery$send_on_join;
    }

    public static boolean sendPackOnUpload() {
        return instance.resource_pack$delivery$resend_on_upload;
    }

    public static boolean autoUpload() {
        return instance.resource_pack$delivery$auto_upload;
    }

    public static Path fileToUpload() {
        return instance.resource_pack$delivery$file_to_upload;
    }

    public static List<ResolutionConditional> resolutions() {
        return instance.resource_pack$duplicated_files_handler;
    }

    public static boolean crashTool1() {
        return instance.resource_pack$protection$crash_tools$method_1;
    }

    public static boolean crashTool2() {
        return instance.resource_pack$protection$crash_tools$method_2;
    }

    public static boolean crashTool3() {
        return instance.resource_pack$protection$crash_tools$method_3;
    }

    public static boolean crashTool4() {
        return false;
    }

    public static boolean enableObfuscation() {
        return instance.resource_pack$protection$obfuscation$enable;
    }

    public static long obfuscationSeed() {
        return instance.resource_pack$protection$obfuscation$seed;
    }

    public static boolean createFakeDirectory() {
        return instance.resource_pack$protection$obfuscation$fake_directory;
    }

    public static boolean escapeUnicode() {
        return instance.resource_pack$protection$obfuscation$escape_unicode;
    }

    public static boolean breakJson() {
        return instance.resource_pack$protection$obfuscation$break_json;
    }

    public static boolean enableRandomResourceLocation() {
        return instance.resource_pack$protection$obfuscation$resource_location$enable;
    }

    public static int namespaceLength() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_namespace$length;
    }

    public static int namespaceAmount() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_namespace$amount;
    }

    public static String atlasSource() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_path$source;
    }

    public static int pathDepth() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_path$depth;
    }

    public static boolean antiUnzip() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_path$anti_unzip;
    }

    public static int imagesPerCanvas() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_atlas$images_per_canvas;
    }

    public static boolean useDouble() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_atlas$use_double;
    }

    public static List<String> bypassTextures() {
        return instance.resource_pack$protection$obfuscation$resource_location$bypass_textures;
    }

    public static List<String> bypassModels() {
        return instance.resource_pack$protection$obfuscation$resource_location$bypass_models;
    }

    public static List<String> bypassSounds() {
        return instance.resource_pack$protection$obfuscation$resource_location$bypass_sounds;
    }

    public static List<String> bypassEquipments() {
        return instance.resource_pack$protection$obfuscation$resource_location$bypass_equipments;
    }

    public static boolean generateModAssets() {
        return instance.resource_pack$generate_mod_assets;
    }

    public static boolean removeTintedLeavesParticle() {
        return instance.resource_pack$remove_tinted_leaves_particle;
    }

    public static boolean filterChat() {
        return instance.image$illegal_characters_filter$chat;
    }

    public static boolean filterAnvil() {
        return instance.image$illegal_characters_filter$anvil;
    }

    public static boolean filterCommand() {
        return instance.image$illegal_characters_filter$command;
    }

    public static boolean filterBook() {
        return instance.image$illegal_characters_filter$book;
    }

    public static boolean filterSign() {
        return instance.image$illegal_characters_filter$sign;
    }

    public static boolean hideBaseEntity() {
        return instance.furniture$hide_base_entity;
    }

    public static int compressionMethod() {
        int id = instance.chunk_system$compression_method;
        if (id <= 0 || id > CompressionMethod.METHOD_COUNT) {
            id = 4;
        }
        return id;
    }

    public static boolean interceptSystemChat() {
        return instance.image$intercept_packets$system_chat;
    }

    public static boolean interceptTabList() {
        return instance.image$intercept_packets$tab_list;
    }

    public static boolean interceptActionBar() {
        return instance.image$intercept_packets$actionbar;
    }

    public static boolean interceptTitle() {
        return instance.image$intercept_packets$title;
    }

    public static boolean interceptBossBar() {
        return instance.image$intercept_packets$bossbar;
    }

    public static boolean interceptContainer() {
        return instance.image$intercept_packets$container;
    }

    public static boolean interceptTeam() {
        return instance.image$intercept_packets$team;
    }

    public static boolean interceptEntityName() {
        return instance.image$intercept_packets$entity_name;
    }

    public static boolean interceptScoreboard() {
        return instance.image$intercept_packets$scoreboard;
    }

    public static boolean interceptTextDisplay() {
        return instance.image$intercept_packets$text_display;
    }

    public static boolean interceptArmorStand() {
        return instance.image$intercept_packets$armor_stand;
    }

    public static boolean interceptPlayerInfo() {
        return instance.image$intercept_packets$player_info;
    }

    public static boolean interceptSetScore() {
        return instance.image$intercept_packets$set_score;
    }

    public static boolean interceptItem() {
        return instance.image$intercept_packets$item;
    }

    public static boolean interceptAdvancement() {
        return instance.image$intercept_packets$advancement;
    }

    public static boolean predictBreaking() {
        return instance.block$predict_breaking;
    }

    public static int predictBreakingInterval() {
        return instance.block$predict_breaking_interval;
    }

    public static double extendedInteractionRange() {
        return instance.block$extended_interaction_range;
    }

    public static boolean allowEmojiSign() {
        return instance.emoji$contexts$sign;
    }

    public static boolean allowEmojiChat() {
        return instance.emoji$contexts$chat;
    }

    public static boolean allowEmojiAnvil() {
        return instance.emoji$contexts$anvil;
    }

    public static boolean allowEmojiBook() {
        return instance.emoji$contexts$book;
    }

    public static ColliderType colliderType() {
        return instance.furniture$collision_entity_type;
    }

    public static boolean enableChunkCache() {
        return instance.chunk_system$cache_system;
    }

    public static boolean addNonItalicTag() {
        return instance.item$non_italic_tag;
    }

    public static boolean fastInjection() {
        return instance.chunk_system$injection$use_fast_method;
    }

    public static boolean injectionTarget() {
        return instance.chunk_system$injection$target;
    }

    public static boolean validateResourcePack() {
        return instance.resource_pack$validate$enable;
    }

    public static boolean excludeShaders() {
        return instance.resource_pack$exclude_core_shaders;
    }

    public static String createOverlayFolderName(String version) {
        return instance.resource_pack$overlay_format.replace("{version}", version);
    }

    public static Key sacrificedAssetId() {
        return instance.equipment$sacrificed_vanilla_armor$asset_id;
    }

    public static Key sacrificedHumanoid() {
        return instance.equipment$sacrificed_vanilla_armor$humanoid;
    }

    public static Key sacrificedHumanoidLeggings() {
        return instance.equipment$sacrificed_vanilla_armor$humanoid_leggings;
    }

    public static String sacrificedVanillaArmorType() {
        return instance.equipment$sacrificed_vanilla_armor$type;
    }

    public static boolean globalClientboundModel() {
        return instance.item$client_bound_model;
    }

    public static List<String> recipeIngredientSources() {
        return instance.recipe$ingredient_sources;
    }

    public static boolean triggerUpdateAttack() {
        return instance.item$update_triggers$attack;
    }

    public static boolean triggerUpdateClick() {
        return instance.item$update_triggers$click_in_inventory;
    }

    public static boolean triggerUpdatePickUp() {
        return instance.item$update_triggers$pick_up;
    }

    public static boolean triggerUpdateDrop() {
        return instance.item$update_triggers$drop;
    }

    public void setObf(boolean enable) {
        this.resource_pack$protection$obfuscation$enable = enable;
    }

    public YamlDocument loadOrCreateYamlData(String fileName) {
        Path path = this.plugin.dataFolderPath().resolve(fileName);
        if (!Files.exists(path)) {
            this.plugin.saveResource(fileName);
        }
        return this.loadYamlData(path);
    }

    public YamlDocument loadYamlConfig(String filePath, GeneralSettings generalSettings, LoaderSettings loaderSettings, DumperSettings dumperSettings, UpdaterSettings updaterSettings) {
        try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
            return YamlDocument.create(inputStream, this.plugin.resourceStream(filePath), generalSettings, loaderSettings, dumperSettings, updaterSettings);
        } catch (IOException e) {
            this.plugin.logger().severe("Failed to load config " + filePath, e);
            return null;
        }
    }

    public YamlDocument loadYamlData(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return YamlDocument.create(inputStream);
        } catch (IOException e) {
            this.plugin.logger().severe("Failed to load config " + file, e);
            return null;
        }
    }

    public Path resolveConfig(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
        filePath = filePath.replace('\\', '/');
        Path configFile = this.plugin.dataFolderPath().resolve(filePath);
        // if the config doesn't exist, create it based on the template in the resources dir
        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFile.getParent());
            } catch (IOException ignored) {
            }
            try (InputStream is = this.plugin.resourceStream(filePath)) {
                if (is == null) {
                    throw new IllegalArgumentException("The embedded resource '" + filePath + "' cannot be found");
                }
                Files.copy(is, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return configFile;
    }

    private Path resolvePath(String path) {
        return path.startsWith(".") ? CraftEngine.instance().dataFolderPath().resolve(path) : Path.of(path);
    }

    public YamlDocument settings() {
        if (config == null) {
            throw new IllegalStateException("Main config not loaded");
        }
        return config;
    }

    public static Config instance() {
        return instance;
    }
}
