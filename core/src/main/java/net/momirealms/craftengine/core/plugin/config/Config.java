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
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.network.encrypt.AESGCM;
import net.momirealms.craftengine.core.item.network.encrypt.ChaCha20;
import net.momirealms.craftengine.core.item.network.encrypt.ItemCrypto;
import net.momirealms.craftengine.core.item.network.encrypt.Xor;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
import net.momirealms.craftengine.core.pack.host.HttpClientManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.filter.DisconnectLogFilter;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.chunk.storage.CompressionMethod;
import net.momirealms.craftengine.core.world.chunk.storage.StorageType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public final class Config {
    private static Config instance;
    private final CraftEngine plugin;
    private final Path configFilePath;
    private final String configVersion;
    private YamlDocument config;
    private long lastModified;
    private long size;

    private boolean firstTime = true;
    private boolean checkUpdate;
    private boolean metrics;
    private Locale forcedLocale;

    private boolean misc$filterConfigurationPhaseDisconnect;
    private boolean misc$delayConfigurationLoad;
    private boolean misc$multi_threaded_configuration_load;
    private boolean misc$inject_packet_vents;

    private boolean debug$common;
    private boolean debug$packet;
    private boolean debug$item;
    private boolean debug$furniture;
    private boolean debug$resource_pack;
    private boolean debug$block;
    private boolean debug$entity_culling;
    private boolean debug$chunk;
    private boolean debug$print_stack_trace;
    private Set<String> debug$ignored_packets;

    private boolean resource_pack$remove_tinted_leaves_particle;
    private boolean resource_pack$generate_mod_assets;
    private boolean resource_pack$override_uniform_font;
    private List<ConditionalResolution> resource_pack$duplicated_files_handler;
    private List<String> resource_pack$merge_external_folders;
    private List<String> resource_pack$merge_external_zips;
    private Set<String> resource_pack$exclude_file_extensions;
    private Path resource_pack$path;
    private String resource_pack$description;
    private boolean resource_pack$map_plugin_compatibility$enable;
    private Path resource_pack$map_plugin_compatibility$path;

    private boolean resource_pack$protection$unprotected_copy$enable;
    private Path resource_pack$protection$unprotected_copy$path;
    private boolean resource_pack$protection$crash_tools$method_1;
    private boolean resource_pack$protection$crash_tools$method_2;
    private boolean resource_pack$protection$crash_tools$method_3;
    private boolean resource_pack$protection$crash_tools$method_4;
    private boolean resource_pack$protection$crash_tools$method_5;
    private boolean resource_pack$protection$crash_tools$method_6;
    private boolean resource_pack$protection$crash_tools$method_7;
    private boolean resource_pack$protection$crash_tools$method_8;
    private boolean resource_pack$protection$crash_tools$method_9;

    private boolean resource_pack$validation$enable;
    private boolean resource_pack$validation$fix_atlas;
    private boolean resource_pack$validation$fix_missing_texture;
    private boolean resource_pack$validation$fallback_models$fix_textures_format;
    private boolean resource_pack$validation$fallback_models$fix_element_rotation_angle;
    private boolean resource_pack$exclude_core_shaders;

    public boolean resource_pack$pack_squash$enable;
    public Path resource_pack$pack_squash$software_path;
    public Path resource_pack$pack_squash$config_path;

    private boolean resource_pack$protection$obfuscation$enable;
    private long resource_pack$protection$obfuscation$seed;
    private boolean resource_pack$protection$fake_directory;
    private boolean resource_pack$protection$escape_json;
    private boolean resource_pack$protection$break_texture;
    private boolean resource_pack$protection$obfuscation$path$anti_unzip;
    private boolean resource_pack$protection$incorrect_crc;
    private boolean resource_pack$protection$fake_file_size;
    private NumberProvider resource_pack$protection$obfuscation$overlay$length;
    private NumberProvider resource_pack$protection$obfuscation$namespace$length;
    private int resource_pack$protection$obfuscation$namespace$amount;
    private String resource_pack$protection$obfuscation$path$block_source;
    private String resource_pack$protection$obfuscation$path$item_source;
    private NumberProvider resource_pack$protection$obfuscation$path$depth;
    private NumberProvider resource_pack$protection$obfuscation$path$length;
    private boolean resource_pack$protection$obfuscation$obfuscate_item_model;
    private int resource_pack$protection$obfuscation$atlas$images_per_canvas;
    private String resource_pack$protection$obfuscation$atlas$prefix;
    private List<String> resource_pack$protection$obfuscation$bypass_textures;
    private List<String> resource_pack$protection$obfuscation$bypass_models;
    private List<String> resource_pack$protection$obfuscation$bypass_sounds;
    private List<String> resource_pack$protection$obfuscation$bypass_equipments;
    private List<String> resource_pack$protection$obfuscation$bypass_item_models;

    private boolean resource_pack$optimization$enable;
    private boolean resource_pack$optimization$texture$enable;
    private Set<String> resource_pack$optimization$texture$exlude;
    private int resource_pack$optimization$texture$zopfli_iterations;
    private boolean resource_pack$optimization$json$enable;
    private Set<String> resource_pack$optimization$json$exclude;

    private MinecraftVersion resource_pack$supported_version$min;
    private MinecraftVersion resource_pack$supported_version$max;
    private String resource_pack$overlay_format;

    private boolean resource_pack$delivery$kick_if_declined;
    private boolean resource_pack$delivery$kick_if_failed_to_apply;
    private boolean resource_pack$delivery$send_on_join;
    private boolean resource_pack$delivery$resend_on_upload;
    private boolean resource_pack$delivery$auto_upload;
    private boolean resource_pack$delivery$strict_player_uuid_validation;
    private Path resource_pack$delivery$file_to_upload;
    private boolean resource_pack$delivery$proxy$enable;
    private int resource_pack$delivery$proxy$port;
    private String resource_pack$delivery$proxy$host;
    private String resource_pack$delivery$proxy$username;
    private String resource_pack$delivery$proxy$password;
    private String resource_pack$delivery$proxy$scheme;
    private Component resource_pack$delivery$prompt;

    private int chunk_system$compression_method;
    private boolean chunk_system$restore_vanilla_blocks_on_chunk_unload;
    private boolean chunk_system$restore_custom_blocks_on_chunk_load;
    private boolean chunk_system$sync_custom_blocks_on_chunk_load;
    private boolean chunk_system$cache_system = true;
    private boolean chunk_system$injection$target;
    private boolean chunk_system$process_invalid_furniture$enable;
    private Map<String, String> chunk_system$process_invalid_furniture$mapping;
    private boolean chunk_system$process_invalid_blocks$enable;
    private Map<String, String> chunk_system$process_invalid_blocks$mapping;
    private StorageType chunk_system$storage_type;

    private boolean furniture$hide_base_entity;
    private ColliderType furniture$collision_entity_type;
    private boolean furniture$light_system;

    private boolean block$sound_system$enable;
    private boolean block$sound_system$process_cancelled_events$step;
    private boolean block$sound_system$process_cancelled_events$break;
    private String block$sound_system$silent_sound_path;
    private boolean block$simplify_adventure_break_check;
    private boolean block$simplify_adventure_place_check;
    private boolean block$predict_breaking;
    private int block$predict_breaking_interval;
    private double block$extended_interaction_range;
    private Key block$deceive_bukkit_material$default;
    private Map<Integer, Key> block$deceive_bukkit_material$overrides;
    private int block$serverside_blocks = -1;
    private boolean block$inject_bukkit_material;
    private boolean block$light_system$async_update;
    private boolean block$light_system$enable;

    private boolean recipe$enable;
    private boolean recipe$disable_vanilla_recipes$all;
    private boolean recipe$unlock_on_ingredient_obtained;
    private Set<Key> recipe$disable_vanilla_recipes$list;
    private List<String> recipe$ingredient_sources;
    private boolean recipe$inject_block_entities;

    private List<String> loot$entity_sources;

    private boolean image$illegal_characters_filter$command;
    private boolean image$illegal_characters_filter$chat;
    private boolean image$illegal_characters_filter$anvil;
    private boolean image$illegal_characters_filter$sign;
    private boolean image$illegal_characters_filter$book;
    private int image$codepoint_starting_value$default;
    private Map<Key, Integer> image$codepoint_starting_value$overrides;

    private boolean network$intercept_packets$system_chat;
    private boolean network$intercept_packets$tab_list;
    private boolean network$intercept_packets$actionbar;
    private boolean network$intercept_packets$title;
    private boolean network$intercept_packets$bossbar;
    private boolean network$intercept_packets$container;
    private boolean network$intercept_packets$team;
    private boolean network$intercept_packets$scoreboard;
    private boolean network$intercept_packets$entity_name;
    private boolean network$intercept_packets$text_display;
    private boolean network$intercept_packets$armor_stand;
    private boolean network$intercept_packets$player_info;
    private boolean network$intercept_packets$set_score;
    private boolean network$intercept_packets$item;
    private boolean network$intercept_packets$advancement;
    private boolean network$intercept_packets$player_chat;
    private boolean network$intercept_packets$dialog;
    private boolean network$disable_item_operations;
    private boolean network$disable_chat_report;
    private boolean network$mod_channel$enable;
    private boolean network$mod_channel$requires_permission;
    private boolean network$item_crypto$enable;

    private boolean item$client_bound_model;
    private boolean item$non_italic_tag;
    private boolean item$update_triggers$attack;
    private boolean item$update_triggers$click_in_inventory;
    private boolean item$update_triggers$drop;
    private boolean item$update_triggers$pick_up;
    private int item$custom_model_data_starting_value$default;
    private Map<Key, Integer> item$custom_model_data_starting_value$overrides;
    private boolean item$always_use_item_model;
    private boolean item$always_use_custom_model_data;
    private boolean item$always_generate_model_overrides;
    private Key item$default_material = ItemKeys.NETHER_BRICK;
    private boolean item$default_drop_display$enable = false;
    private String item$default_drop_display$format = null;
    private boolean item$data_fixer_upper$enable = true;
    private int item$data_fixer_upper$fallback_version = VersionHelper.WORLD_VERSION;

    private String equipment$sacrificed_vanilla_armor$type;
    private Key equipment$sacrificed_vanilla_armor$asset_id;
    private Key equipment$sacrificed_vanilla_armor$humanoid;
    private Key equipment$sacrificed_vanilla_armor$humanoid_leggings;

    private boolean emoji$contexts$chat;
    private boolean emoji$contexts$book;
    private boolean emoji$contexts$anvil;
    private boolean emoji$contexts$sign;
    private int emoji$max_emojis_per_parse;

    private boolean client_optimization$entity_culling$enable;
    private int client_optimization$entity_culling$view_distance;
    private int client_optimization$entity_culling$threads;
    private boolean client_optimization$entity_culling$ray_tracing;
    private boolean client_optimization$entity_culling$rate_limiting$enable;
    private int client_optimization$entity_culling$rate_limiting$bucket_size;
    private int client_optimization$entity_culling$rate_limiting$restore_per_tick;

    private boolean bedrock_edition_support$enable;
    private String bedrock_edition_support$player_prefix;

    public Config(CraftEngine plugin) {
        this.plugin = plugin;
        this.configVersion = PluginProperties.getValue("config");
        this.configFilePath = this.plugin.dataFolderPath().resolve("config.yml");
        instance = this;
    }

    public boolean updateConfigCache() {
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
                this.lastModified = lastModified;
                this.size = size;
                return true;
            }
        } catch (IOException e) {
            this.plugin.logger().error("Failed to update config.yml", e);
        }
        return false;
    }

    public void load() {
        boolean isUpdated = updateConfigCache();
        if (isUpdated) {
            loadFullSettings();
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
                            .addIgnoredRoute(PluginProperties.getValue("config"), "chunk-system.process-invalid-blocks.convert", '.')
                            .addIgnoredRoute(PluginProperties.getValue("config"), "chunk-system.process-invalid-furniture.convert", '.')
                            .addIgnoredRoute(PluginProperties.getValue("config"), "item.custom-model-data-starting-value.overrides", '.')
                            .addIgnoredRoute(PluginProperties.getValue("config"), "block.deceive-bukkit-material.overrides", '.')
                            .build());
        }
        try {
            this.config.save(new File(plugin.dataFolderFile(), "config.yml"));
        } catch (IOException e) {
            this.plugin.logger().warn("Could not save config.yml", e);
        }
    }

    public void loadForcedLocale() {
        YamlDocument config = settings();
        this.forcedLocale = TranslationManager.parseLocale(config.getString("forced-locale", ""));
    }

    @SuppressWarnings("DuplicatedCode")
    public void loadFullSettings() {
        YamlDocument config = settings();
        this.forcedLocale = TranslationManager.parseLocale(config.getString("forced-locale", ""));
        this.misc$delayConfigurationLoad = config.getBoolean("misc.delay-configuration-load", true);
        this.misc$multi_threaded_configuration_load = config.getBoolean("misc.multi-threaded-configuration-load", true);
        this.misc$inject_packet_vents = config.getBoolean("misc.inject-packetevents", false);

        // basics
        this.metrics = config.getBoolean("metrics", false);
        this.checkUpdate = config.getBoolean("update-checker", false);
        this.misc$filterConfigurationPhaseDisconnect = config.getBoolean("misc.filter-configuration-phase-disconnect", false);
        DisconnectLogFilter.instance().setEnable(misc$filterConfigurationPhaseDisconnect);

        // debug
        this.debug$common = config.getBoolean("debug.common", false);
        this.debug$packet = config.getBoolean("debug.packet", false);
        this.debug$item = config.getBoolean("debug.item", false);
        this.debug$furniture = config.getBoolean("debug.furniture", false);
        this.debug$resource_pack = config.getBoolean("debug.resource-pack", false);
        this.debug$block = config.getBoolean("debug.block", false);
        this.debug$entity_culling = config.getBoolean("debug.entity-culling", false);
        this.debug$chunk = config.getBoolean("debug.chunk", false);
        this.debug$ignored_packets = new HashSet<>(config.getStringList("debug.ignored-packets"));
        this.debug$print_stack_trace = config.getBoolean("debug.print-stack-trace", false);

        // resource pack
        this.resource_pack$path = resolvePath(config.getString("resource-pack.path", "./generated/resource_pack.zip"));
        this.resource_pack$description = config.getString("resource-pack.description", "<gray>CraftEngine ResourcePack</gray>");
        this.resource_pack$override_uniform_font = config.getBoolean("resource-pack.override-uniform-font", false);
        this.resource_pack$generate_mod_assets = config.getBoolean("resource-pack.generate-mod-assets", false);
        this.resource_pack$remove_tinted_leaves_particle = config.getBoolean("resource-pack.remove-tinted-leaves-particle", true);
        this.resource_pack$supported_version$min = getVersion(config.get("resource-pack.supported-version.min", "server").toString());
        this.resource_pack$supported_version$max = getVersion(config.get("resource-pack.supported-version.max", "latest").toString());
        if (this.resource_pack$supported_version$min.isAbove(this.resource_pack$supported_version$max)) {
            this.resource_pack$supported_version$min = this.resource_pack$supported_version$max;
        }
        this.resource_pack$map_plugin_compatibility$enable = config.getBoolean("resource-pack.map-plugin-compatibility.enable", false);
        this.resource_pack$map_plugin_compatibility$path = resolvePath(config.getString("resource-pack.map-plugin-compatibility.path", "./generated/resource_pack_map.zip"));
        this.resource_pack$merge_external_folders = config.getStringList("resource-pack.merge-external-folders");
        this.resource_pack$merge_external_zips = config.getStringList("resource-pack.merge-external-zip-files");
        this.resource_pack$exclude_file_extensions = new HashSet<>(config.getStringList("resource-pack.exclude-file-extensions"));
        this.resource_pack$delivery$send_on_join = config.getBoolean("resource-pack.delivery.send-on-join", true);
        this.resource_pack$delivery$resend_on_upload = config.getBoolean("resource-pack.delivery.resend-on-upload", true);
        this.resource_pack$delivery$kick_if_declined = config.getBoolean("resource-pack.delivery.kick-if-declined", true);
        this.resource_pack$delivery$kick_if_failed_to_apply = config.getBoolean("resource-pack.delivery.kick-if-failed-to-apply", true);
        this.resource_pack$delivery$auto_upload = config.getBoolean("resource-pack.delivery.auto-upload", true);
        this.resource_pack$delivery$strict_player_uuid_validation = config.getBoolean("resource-pack.delivery.strict-player-uuid-validation", true);
        this.resource_pack$delivery$file_to_upload = resolvePath(config.getString("resource-pack.delivery.file-to-upload", "./generated/resource_pack.zip"));
        this.resource_pack$delivery$proxy$enable = config.getBoolean("resource-pack.delivery.proxy.enable", false);
        this.resource_pack$delivery$proxy$port = config.getInt("resource-pack.delivery.proxy.port", 7890);
        this.resource_pack$delivery$proxy$host = config.getString("resource-pack.delivery.proxy.host", "localhost");
        this.resource_pack$delivery$proxy$username = config.getString("resource-pack.delivery.proxy.username", "");
        this.resource_pack$delivery$proxy$password = config.getString("resource-pack.delivery.proxy.password", "");
        this.resource_pack$delivery$proxy$scheme = config.getString("resource-pack.delivery.proxy.scheme", "http");
        HttpClientManager.init(
                this.resource_pack$delivery$proxy$enable,
                this.resource_pack$delivery$proxy$host,
                this.resource_pack$delivery$proxy$port,
                this.resource_pack$delivery$proxy$username,
                this.resource_pack$delivery$proxy$password
        );
        this.resource_pack$delivery$prompt = AdventureHelper.miniMessage().deserialize(config.getString("resource-pack.delivery.prompt", "<yellow>To fully experience our server, please accept our custom resource pack.</yellow>"));
        this.resource_pack$pack_squash$enable = config.getBoolean("resource-pack.pack-squash.enable", false);
        this.resource_pack$pack_squash$software_path = resolvePath(config.getString("resource-pack.pack-squash.software-path", "./packsquash/packsquash.exe"));
        this.resource_pack$pack_squash$config_path = resolvePath(config.getString("resource-pack.pack-squash.config-path", "./packsquash/config.toml"));
        this.resource_pack$protection$unprotected_copy$enable = config.getBoolean("resource-pack.protection.unprotected-copy.enable", false);
        this.resource_pack$protection$unprotected_copy$path = resolvePath(config.getString("resource-pack.protection.unprotected-copy.path", "./generated/unprotected_resource_pack.zip"));
        this.resource_pack$protection$crash_tools$method_1 = config.getBoolean("resource-pack.protection.crash-tools.method-1", false);
        this.resource_pack$protection$crash_tools$method_2 = config.getBoolean("resource-pack.protection.crash-tools.method-2", false);
        this.resource_pack$protection$crash_tools$method_3 = config.getBoolean("resource-pack.protection.crash-tools.method-3", false);
        this.resource_pack$protection$crash_tools$method_4 = config.getBoolean("resource-pack.protection.crash-tools.method-4", false);
        this.resource_pack$protection$crash_tools$method_5 = config.getBoolean("resource-pack.protection.crash-tools.method-5", false);
        this.resource_pack$protection$crash_tools$method_6 = config.getBoolean("resource-pack.protection.crash-tools.method-6", false);
        this.resource_pack$protection$crash_tools$method_7 = config.getBoolean("resource-pack.protection.crash-tools.method-7", false);
        this.resource_pack$protection$crash_tools$method_8 = config.getBoolean("resource-pack.protection.crash-tools.method-8", false);
        this.resource_pack$protection$crash_tools$method_9 = config.getBoolean("resource-pack.protection.crash-tools.method-9", false);
        this.resource_pack$protection$obfuscation$enable = VersionHelper.PREMIUM && config.getBoolean("resource-pack.protection.obfuscation.enable", false);
        this.resource_pack$protection$obfuscation$seed = config.getLong("resource-pack.protection.obfuscation.seed", 0L);
        this.resource_pack$protection$fake_directory = config.getBoolean("resource-pack.protection.fake-directory", false);
        this.resource_pack$protection$escape_json = config.getBoolean("resource-pack.protection.escape-json", false);
        this.resource_pack$protection$break_texture = config.getBoolean("resource-pack.protection.break-texture", false);
        this.resource_pack$protection$incorrect_crc = config.getBoolean("resource-pack.protection.incorrect-crc", false);
        this.resource_pack$protection$fake_file_size = config.getBoolean("resource-pack.protection.fake-file-size", false);
        this.resource_pack$protection$obfuscation$namespace$amount = config.getInt("resource-pack.protection.obfuscation.namespace.amount", 32);
        this.resource_pack$protection$obfuscation$namespace$length = NumberProviders.fromConfig(ConfigValue.of("resource-pack.protection.obfuscation.namespace.length", config.get("resource-pack.protection.obfuscation.namespace.length", 2)));
        this.resource_pack$protection$obfuscation$overlay$length = NumberProviders.fromConfig(ConfigValue.of("resource-pack.protection.obfuscation.overlay.length", config.get("resource-pack.protection.obfuscation.overlay.length", 4)));
        this.resource_pack$protection$obfuscation$path$depth = NumberProviders.fromConfig(ConfigValue.of("resource-pack.protection.obfuscation.path.depth", config.get("resource-pack.protection.obfuscation.path.depth", 4)));
        this.resource_pack$protection$obfuscation$path$length = NumberProviders.fromConfig(ConfigValue.of("resource-pack.protection.obfuscation.path.length", config.get("resource-pack.protection.obfuscation.path.length", 2)));
        this.resource_pack$protection$obfuscation$path$anti_unzip = config.getBoolean("resource-pack.protection.obfuscation.path.anti-unzip", false);
        this.resource_pack$protection$obfuscation$obfuscate_item_model = config.getBoolean("resource-pack.protection.obfuscation.obfuscate-item-model", false) && this.resource_pack$protection$obfuscation$enable;
        this.resource_pack$protection$obfuscation$path$block_source = config.getString("resource-pack.protection.obfuscation.path.block-source", "obf_block");
        this.resource_pack$protection$obfuscation$path$item_source = config.getString("resource-pack.protection.obfuscation.path.block-source", "obf_item");
        this.resource_pack$protection$obfuscation$atlas$images_per_canvas = Math.max(0, config.getInt("resource-pack.protection.obfuscation.atlas.images-per-canvas", 256));
        this.resource_pack$protection$obfuscation$atlas$prefix = config.getString("resource-pack.protection.obfuscation.atlas.prefix", "atlas");
        this.resource_pack$protection$obfuscation$bypass_textures = config.getStringList("resource-pack.protection.obfuscation.bypass-textures");
        this.resource_pack$protection$obfuscation$bypass_models = config.getStringList("resource-pack.protection.obfuscation.bypass-models");
        this.resource_pack$protection$obfuscation$bypass_sounds = config.getStringList("resource-pack.protection.obfuscation.bypass-sounds");
        this.resource_pack$protection$obfuscation$bypass_equipments = config.getStringList("resource-pack.protection.obfuscation.bypass-equipments");
        this.resource_pack$protection$obfuscation$bypass_item_models = config.getStringList("resource-pack.protection.obfuscation.bypass-item-models");
        this.resource_pack$optimization$enable = config.getBoolean("resource-pack.optimization.enable", false);
        this.resource_pack$optimization$texture$enable = config.getBoolean("resource-pack.optimization.texture.enable", true);
        this.resource_pack$optimization$texture$zopfli_iterations = config.getInt("resource-pack.optimization.texture.zopfli-iterations", 0);
        this.resource_pack$optimization$texture$exlude = config.getStringList("resource-pack.optimization.texture.exclude").stream().map(p -> {
            if (p.endsWith("/")) return p;
            if (!p.endsWith(".png")) return p + ".png";
            return p;
        }).collect(Collectors.toSet());
        this.resource_pack$optimization$json$enable = config.getBoolean("resource-pack.optimization.json.enable", true);
        this.resource_pack$optimization$json$exclude = config.getStringList("resource-pack.optimization.json.exclude").stream().map(p -> {
            if (p.endsWith("/")) return p;
            if (!p.endsWith(".json") && !p.endsWith(".mcmeta")) return p + ".json";
            return p;
        }).collect(Collectors.toSet());
        this.resource_pack$validation$enable = config.getBoolean("resource-pack.validation.enable", true);
        this.resource_pack$validation$fix_atlas = config.getBoolean("resource-pack.validation.fix-atlas", true);
        this.resource_pack$validation$fix_missing_texture = config.getBoolean("resource-pack.validation.fix-missing-texture", true);
        this.resource_pack$validation$fallback_models$fix_textures_format = config.getBoolean("resource-pack.validation.fallback-models.fix-textures-format", true);
        this.resource_pack$validation$fallback_models$fix_element_rotation_angle = config.getBoolean("resource-pack.validation.fallback-models.fix-element-rotation-angle", true);
        this.resource_pack$exclude_core_shaders = config.getBoolean("resource-pack.exclude-core-shaders", false);
        this.resource_pack$overlay_format = config.getString("resource-pack.overlay-format", "overlay_{version}");
        if (!this.resource_pack$overlay_format.contains("{version}")) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                    "config.errors_detected",
                    TranslationManager.instance().plainTranslation(
                            "resource_pack.invalid_overlay_format",
                            this.resource_pack$overlay_format
                    )
            ));
            this.resource_pack$overlay_format = "overlay_{version}";
        }
        try {
            List<?> list = config.getList("resource-pack.duplicated-files-handler");
            List<ConditionalResolution> resolutions = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                resolutions.add(ConditionalResolution.FACTORY.create(ConfigSection.of("resource-pack.duplicated-files-handler[" + i + "]", MiscUtils.castToMap(list.get(i)))));
            }
            this.resource_pack$duplicated_files_handler = resolutions;
        } catch (KnownResourceException e) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("config.errors_detected", e.getLocalizedMessage()));
            this.resource_pack$duplicated_files_handler = List.of();
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to load resource-pack.duplicated-files-handler", e);
            this.resource_pack$duplicated_files_handler = List.of();
        }

        // chunk
        this.chunk_system$compression_method = config.getInt("chunk-system.compression-method", 4);
        try {
            this.chunk_system$storage_type = StorageType.valueOf(config.getString("chunk-system.storage-type", "mca").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            this.chunk_system$storage_type = StorageType.MCA;
        }
        this.chunk_system$restore_vanilla_blocks_on_chunk_unload = config.getBoolean("chunk-system.restore-vanilla-blocks-on-chunk-unload", true);
        this.chunk_system$restore_custom_blocks_on_chunk_load = config.getBoolean("chunk-system.restore-custom-blocks-on-chunk-load", true);
        this.chunk_system$sync_custom_blocks_on_chunk_load = config.getBoolean("chunk-system.sync-custom-blocks-on-chunk-load", false);
        this.chunk_system$cache_system = config.getBoolean("chunk-system.cache-system", true);

        if (this.firstTime) {
            this.chunk_system$injection$target = config.getString("chunk-system.injection.target", "palette").equalsIgnoreCase("palette");
        }

        this.chunk_system$process_invalid_furniture$enable = config.getBoolean("chunk-system.process-invalid-furniture.enable", false);
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
        this.chunk_system$process_invalid_furniture$mapping = furnitureBuilder.build();

        this.chunk_system$process_invalid_blocks$enable = config.getBoolean("chunk-system.process-invalid-blocks.enable", false);
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
        this.chunk_system$process_invalid_blocks$mapping = blockBuilder.build();

        // furniture
        this.furniture$hide_base_entity = config.getBoolean("furniture.hide-base-entity", true);
        this.furniture$collision_entity_type = ColliderType.valueOf(config.getString("furniture.collision-entity-type", "interaction").toUpperCase(Locale.ENGLISH));
        if (this.firstTime) {
            this.furniture$light_system = config.getBoolean("furniture.light-system.enable", true);
        }

        // equipment
        this. equipment$sacrificed_vanilla_armor$type = config.getString("equipment.sacrificed-vanilla-armor.type", "chainmail").toLowerCase(Locale.ENGLISH);
        if (!AbstractPackManager.ALLOWED_VANILLA_EQUIPMENT.contains(this.equipment$sacrificed_vanilla_armor$type)) {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation(
                    "config.errors_detected",
                    TranslationManager.instance().plainTranslation(
                            "resource.equipment.invalid_sacrificed_armor",
                            this.equipment$sacrificed_vanilla_armor$type
                    )
            ));
            this.equipment$sacrificed_vanilla_armor$type = "chainmail";
        }

        this.equipment$sacrificed_vanilla_armor$asset_id = Key.of(config.getString("equipment.sacrificed-vanilla-armor.asset-id", "minecraft:chainmail"));
        this.equipment$sacrificed_vanilla_armor$humanoid = Key.of(config.getString("equipment.sacrificed-vanilla-armor.humanoid", "minecraft:trims/entity/humanoid/chainmail"));
        this.equipment$sacrificed_vanilla_armor$humanoid_leggings = Key.of(config.getString("equipment.sacrificed-vanilla-armor.humanoid-leggings", "minecraft:trims/entity/humanoid_leggings/chainmail"));

        // item
        this.item$client_bound_model = config.getBoolean("item.client-bound-model", true) && VersionHelper.PREMIUM;
        this.item$non_italic_tag = config.getBoolean("item.non-italic-tag", false);
        this.item$update_triggers$attack = config.getBoolean("item.update-triggers.attack", false);
        this.item$update_triggers$click_in_inventory = config.getBoolean("item.update-triggers.click-in-inventory", false);
        this.item$update_triggers$drop = config.getBoolean("item.update-triggers.drop", false);
        this.item$update_triggers$pick_up = config.getBoolean("item.update-triggers.pick-up", false);
        this.item$custom_model_data_starting_value$default = config.getInt("item.custom-model-data-starting-value.default", 10000);
        this.item$always_use_item_model = config.getBoolean("item.always-use-item-model", true) && VersionHelper.isOrAbove1_21_2;
        this.item$always_generate_model_overrides = config.getBoolean("item.always-generate-model-overrides", false);
        this.item$always_use_custom_model_data = this.item$always_generate_model_overrides || (config.getBoolean("item.always-use-custom-model-data", false) && VersionHelper.isOrAbove1_21_2);
        this.item$default_material = Key.of(config.getString("item.default-material", "nether_brick"));
        this.item$default_drop_display$enable = config.getBoolean("item.default-drop-display.enable", false);
        this.item$default_drop_display$format = this.item$default_drop_display$enable ? config.getString("item.default-drop-display.format", "<arg:count>x <name>"): null;
        this.item$data_fixer_upper$enable = config.getBoolean("item.data-fixer-upper.enable", true);
        String fallbackVersion = config.getString("item.data-fixer-upper.fallback-version");
        if (fallbackVersion == null || fallbackVersion.isEmpty() || fallbackVersion.equalsIgnoreCase("server")) {
            this.item$data_fixer_upper$fallback_version = VersionHelper.WORLD_VERSION;
        } else {
            this.item$data_fixer_upper$fallback_version = Integer.parseInt(fallbackVersion);
        }
        Section customModelDataOverridesSection = config.getSection("item.custom-model-data-starting-value.overrides");
        if (customModelDataOverridesSection != null) {
            Map<Key, Integer> customModelDataOverrides = new HashMap<>();
            for (Map.Entry<String, Object> entry : customModelDataOverridesSection.getStringRouteMappedValues(false).entrySet()) {
                if (entry.getValue() instanceof String s) {
                    customModelDataOverrides.put(Key.of(entry.getKey()), Integer.parseInt(s));
                } else if (entry.getValue() instanceof Integer i) {
                    customModelDataOverrides.put(Key.of(entry.getKey()), i);
                }
            }
            this.item$custom_model_data_starting_value$overrides = customModelDataOverrides;
        } else {
            this.item$custom_model_data_starting_value$overrides = Map.of();
        }

        // block
        this.block$sound_system$enable = config.getBoolean("block.sound-system.enable", true);
        this.block$sound_system$process_cancelled_events$step = config.getBoolean("block.sound-system.process-cancelled-events.step", true);
        this.block$sound_system$process_cancelled_events$break = config.getBoolean("block.sound-system.process-cancelled-events.break", true);
        this.block$sound_system$silent_sound_path = FileUtils.pathWithoutExtension(config.getString("block.sound-system.silent-sound-path", "internal/silence.ogg"));
        this.block$simplify_adventure_break_check = config.getBoolean("block.simplify-adventure-break-check", false);
        this.block$simplify_adventure_place_check = config.getBoolean("block.simplify-adventure-place-check", false);
        this.block$predict_breaking = config.getBoolean("block.predict-breaking.enable", true);
        this.block$predict_breaking_interval = Math.max(config.getInt("block.predict-breaking.interval", 10), 1);
        this.block$extended_interaction_range = Math.max(config.getDouble("block.predict-breaking.extended-interaction-range", 0.5), 0.0);
        this.block$light_system$async_update = config.getBoolean("block.light-system.async", true);
        this.block$light_system$enable = config.getBoolean("block.light-system.enable", true);
        if (this.firstTime) {
            this.block$deceive_bukkit_material$default = Key.of(config.getString("block.deceive-bukkit-material.default", "bricks"));
            this.block$deceive_bukkit_material$overrides = new HashMap<>();
            Section overridesSection = config.getSection("block.deceive-bukkit-material.overrides");
            if (overridesSection != null) {
                for (Map.Entry<String, Object> entry : overridesSection.getStringRouteMappedValues(false).entrySet()) {
                    String key = entry.getKey();
                    Key value = Key.of(String.valueOf(entry.getValue()));
                    if (key.contains("~")) {
                        int min = Integer.parseInt(key.split("~")[0]);
                        int max = Integer.parseInt(key.split("~")[1]);
                        for (int i = min; i <= max; i++) {
                            this.block$deceive_bukkit_material$overrides.put(i, value);
                        }
                    } else {
                        this.block$deceive_bukkit_material$overrides.put(Integer.valueOf(key), value);
                    }
                }
            }
            this.block$inject_bukkit_material = config.getBoolean("block.inject-bukkit-material", false);
            this.block$serverside_blocks = Math.min(config.getInt("block.serverside-blocks", 2000), 10_0000);
            if (this.block$serverside_blocks < 0) this.block$serverside_blocks = 0;
        }

        // recipe
        this.recipe$enable = config.getBoolean("recipe.enable", true);
        this.recipe$disable_vanilla_recipes$all = config.getBoolean("recipe.disable-vanilla-recipes.all", false);
        this.recipe$disable_vanilla_recipes$list = config.getStringList("recipe.disable-vanilla-recipes.list").stream().map(Key::of).collect(Collectors.toSet());
        this.recipe$ingredient_sources = config.getStringList("recipe.ingredient-sources");
        this.recipe$unlock_on_ingredient_obtained = config.getBoolean("recipe.unlock-on-ingredient-obtained", true);
        if (this.firstTime) {
            this.recipe$inject_block_entities = config.getBoolean("recipe.inject-block-entities", true);
        }

        // loot
        this.loot$entity_sources = config.getStringList("recipe.entity-sources");

        // image
        this.image$illegal_characters_filter$anvil = config.getBoolean("image.illegal-characters-filter.anvil", true);
        this.image$illegal_characters_filter$book = config.getBoolean("image.illegal-characters-filter.book", true);
        this.image$illegal_characters_filter$chat = config.getBoolean("image.illegal-characters-filter.chat", true);
        this.image$illegal_characters_filter$command = config.getBoolean("image.illegal-characters-filter.command", true);
        this.image$illegal_characters_filter$sign = config.getBoolean("image.illegal-characters-filter.sign", true);

        this.image$codepoint_starting_value$default = config.getInt("image.codepoint-starting-value.default", 0);
        Section codepointOverridesSection = config.getSection("image.codepoint-starting-value.overrides");
        if (codepointOverridesSection != null) {
            Map<Key, Integer> codepointOverrides = new HashMap<>();
            for (Map.Entry<String, Object> entry : codepointOverridesSection.getStringRouteMappedValues(false).entrySet()) {
                if (entry.getValue() instanceof String s) {
                    codepointOverrides.put(Key.of(entry.getKey()), Integer.parseInt(s));
                } else if (entry.getValue() instanceof Integer i) {
                    codepointOverrides.put(Key.of(entry.getKey()), i);
                }
            }
            this.image$codepoint_starting_value$overrides = codepointOverrides;
        } else {
            this.image$codepoint_starting_value$overrides = Map.of();
        }

        if (this.firstTime) {
            this.network$disable_chat_report = config.getBoolean("network.disable-chat-report", false);
        }
        this.network$disable_item_operations = config.getBoolean("network.disable-item-operations", false);
        this.network$intercept_packets$system_chat = config.getBoolean("network.intercept-packets.system-chat", true);
        this.network$intercept_packets$tab_list = config.getBoolean("network.intercept-packets.tab-list", true);
        this.network$intercept_packets$actionbar = config.getBoolean("network.intercept-packets.actionbar", true);
        this.network$intercept_packets$title = config.getBoolean("network.intercept-packets.title", true);
        this.network$intercept_packets$bossbar = config.getBoolean("network.intercept-packets.bossbar", true);
        this.network$intercept_packets$container = config.getBoolean("network.intercept-packets.container", true);
        this.network$intercept_packets$team = config.getBoolean("network.intercept-packets.team", true);
        this.network$intercept_packets$scoreboard = config.getBoolean("network.intercept-packets.scoreboard", true);
        this.network$intercept_packets$entity_name = config.getBoolean("network.intercept-packets.entity-name", false);
        this.network$intercept_packets$text_display = config.getBoolean("network.intercept-packets.text-display", true);
        this.network$intercept_packets$armor_stand = config.getBoolean("network.intercept-packets.armor-stand", true);
        this.network$intercept_packets$player_info = config.getBoolean("network.intercept-packets.player-info", true);
        this.network$intercept_packets$set_score = config.getBoolean("network.intercept-packets.set-score", true);
        this.network$intercept_packets$item = config.getBoolean("network.intercept-packets.item", true);
        this.network$intercept_packets$advancement = config.getBoolean("network.intercept-packets.advancement", true);
        this.network$intercept_packets$player_chat = config.getBoolean("network.intercept-packets.player-chat", true);
        this.network$intercept_packets$dialog = config.getBoolean("network.intercept-packets.dialog", true);
        this.network$mod_channel$enable = config.getBoolean("network.mod-channel.enable", true);
        this.network$mod_channel$requires_permission = config.getBoolean("network.mod-channel.requires-permission", true);
        if (this.firstTime) {
            this.network$item_crypto$enable = config.getBoolean("network.item-crypto.enable", false);
            if (this.network$item_crypto$enable) {
                String algorithm = config.getString("network.item-crypto.algorithm", "xor").toLowerCase(Locale.ROOT).replace('_', '-');
                String key = config.getString("network.item-crypto.key", "");
                if (key.isEmpty()) {
                    key = UUID.randomUUID().toString();
                }
                ItemCrypto.setAlgorithm(switch (algorithm) {
                    case "xor" -> new Xor(key);
                    case "aes-gcm" -> new AESGCM(key);
                    case "chacha20" -> new ChaCha20(key);
                    default -> null;
                });
            }
        }

        // emoji
        this.emoji$contexts$chat = config.getBoolean("emoji.contexts.chat", true);
        this.emoji$contexts$anvil = config.getBoolean("emoji.contexts.anvil", true);
        this.emoji$contexts$book = config.getBoolean("emoji.contexts.book", true);
        this.emoji$contexts$sign = config.getBoolean("emoji.contexts.sign", true);
        this.emoji$max_emojis_per_parse = config.getInt("emoji.max-emojis-per-parse", 32);

        // client optimization
        if (this.firstTime) {
            this.client_optimization$entity_culling$enable = VersionHelper.PREMIUM && config.getBoolean("client-optimization.entity-culling.enable", false);
        }
        this.client_optimization$entity_culling$view_distance = config.getInt("client-optimization.entity-culling.view-distance", 64);
        this.client_optimization$entity_culling$threads = config.getInt("client-optimization.entity-culling.threads", 1);
        this.client_optimization$entity_culling$ray_tracing = this.client_optimization$entity_culling$enable && config.getBoolean("client-optimization.entity-culling.ray-tracing", true);
        this.client_optimization$entity_culling$rate_limiting$enable = config.getBoolean("client-optimization.entity-culling.rate-limiting.enable", true);
        this.client_optimization$entity_culling$rate_limiting$bucket_size = config.getInt("client-optimization.entity-culling.rate-limiting.bucket-size", 300);
        this.client_optimization$entity_culling$rate_limiting$restore_per_tick = config.getInt("client-optimization.entity-culling.rate-limiting.restore-per-tick", 5);

        // bedrock support
        this.bedrock_edition_support$enable = config.getBoolean("bedrock-edition-support.enable", true);
        this.bedrock_edition_support$player_prefix = config.getString("bedrock-edition-support.player-prefix", "!");

        this.firstTime = false;
    }

    private static MinecraftVersion getVersion(String version) {
        if (version.equalsIgnoreCase("latest") || version.equalsIgnoreCase("latest_version")) {
            return MinecraftVersion.byName(PluginProperties.getValue("latest-version"));
        }
        if (version.equalsIgnoreCase("server") || version.equalsIgnoreCase("server_version")) {
            return VersionHelper.MINECRAFT_VERSION;
        }
        return MinecraftVersion.byName(version);
    }

    public static Locale forcedLocale() {
        return instance.forcedLocale;
    }

    public static String configVersion() {
        return instance.configVersion;
    }

    public static boolean delayConfigurationLoad() {
        return instance.misc$delayConfigurationLoad;
    }

    public static boolean injectPacketEvents() {
        return instance.misc$inject_packet_vents;
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

    public static boolean debugBlock() {
        return instance.debug$block;
    }

    public static boolean debugEntityCulling() {
        return instance.debug$entity_culling;
    }

    public static boolean debugFurniture() {
        return instance.debug$furniture;
    }

    public static boolean debugChunk() {
        return instance.debug$chunk;
    }

    public static boolean debugResourcePack() {
        return instance.debug$resource_pack;
    }

    public static boolean debugPrintStackTrace() {
        return instance.debug$print_stack_trace;
    }

    public static boolean checkUpdate() {
        return instance.checkUpdate;
    }

    public static boolean metrics() {
        return instance.metrics;
    }

    public static int serverSideBlocks() {
        return instance.block$serverside_blocks;
    }

    public static boolean injectBukkitMaterial() {
        return instance.block$inject_bukkit_material;
    }

    public static void setInjectBukkitMaterial(boolean injectBukkitMaterial) {
        instance.block$inject_bukkit_material = injectBukkitMaterial;
    }

    public static boolean alwaysUseItemModel() {
        return instance.item$always_use_item_model;
    }

    public static boolean alwaysUseCustomModelData() {
        return instance.item$always_use_custom_model_data;
    }

    public static boolean alwaysGenerateModelOverrides() {
        return instance.item$always_generate_model_overrides;
    }

    public static boolean filterConfigurationPhaseDisconnect() {
        return instance.misc$filterConfigurationPhaseDisconnect;
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

    public static boolean enableBlockLightSystem() {
        return instance.block$light_system$enable;
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

    public static boolean processCancelledStep() {
        return instance.block$sound_system$process_cancelled_events$step;
    }

    public static boolean processCancelledBreak() {
        return instance.block$sound_system$process_cancelled_events$break;
    }

    public static String silentSoundPath() {
        return instance.block$sound_system$silent_sound_path;
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
        return instance.resource_pack$delivery$prompt;
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
    public static boolean strictPlayerUuidValidation() {
        return instance.resource_pack$delivery$strict_player_uuid_validation;
    }

    public static Path fileToUpload() {
        return instance.resource_pack$delivery$file_to_upload;
    }

    public static List<ConditionalResolution> resolutions() {
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
        return instance.resource_pack$protection$crash_tools$method_4;
    }

    public static boolean crashTool5() {
        return instance.resource_pack$protection$crash_tools$method_5;
    }

    public static boolean crashTool6() {
        return instance.resource_pack$protection$crash_tools$method_6;
    }

    public static boolean crashTool7() {
        return instance.resource_pack$protection$crash_tools$method_7;
    }

    public static boolean crashTool8() {
        return instance.resource_pack$protection$crash_tools$method_8;
    }

    public static boolean crashTool9() {
        return instance.resource_pack$protection$crash_tools$method_9;
    }

    public static boolean enableObfuscation() {
        return instance.resource_pack$protection$obfuscation$enable;
    }

    public static long obfuscationSeed() {
        return instance.resource_pack$protection$obfuscation$seed;
    }

    public static boolean createFakeDirectory() {
        return instance.resource_pack$protection$fake_directory;
    }

    public static boolean escapeJson() {
        return instance.resource_pack$protection$escape_json;
    }

    public static boolean breakTexture() {
        return instance.resource_pack$protection$break_texture;
    }

    public static NumberProvider namespaceLength() {
        return instance.resource_pack$protection$obfuscation$namespace$length;
    }

    public static int namespaceAmount() {
        return instance.resource_pack$protection$obfuscation$namespace$amount;
    }

    public static String blockAtlasSource() {
        return instance.resource_pack$protection$obfuscation$path$block_source;
    }

    public static String itemAtlasSource() {
        return instance.resource_pack$protection$obfuscation$path$item_source;
    }

    public static NumberProvider pathDepth() {
        return instance.resource_pack$protection$obfuscation$path$depth;
    }

    public static NumberProvider pathLength() {
        return instance.resource_pack$protection$obfuscation$path$length;
    }

    public static boolean obfuscateItemModel() {
        return instance.resource_pack$protection$obfuscation$obfuscate_item_model;
    }

    public static NumberProvider overlayLength() {
        return instance.resource_pack$protection$obfuscation$overlay$length;
    }

    public static boolean antiUnzip() {
        return instance.resource_pack$protection$obfuscation$path$anti_unzip;
    }

    public static boolean incorrectCrc() {
        return instance.resource_pack$protection$incorrect_crc;
    }

    public static boolean fakeFileSize() {
        return instance.resource_pack$protection$fake_file_size;
    }

    public static int imagesPerCanvas() {
        return instance.resource_pack$protection$obfuscation$atlas$images_per_canvas;
    }

    public static String imageCanvasPrefix() {
        return instance.resource_pack$protection$obfuscation$atlas$prefix;
    }

    public static List<String> bypassTextures() {
        return instance.resource_pack$protection$obfuscation$bypass_textures;
    }

    public static List<String> bypassModels() {
        return instance.resource_pack$protection$obfuscation$bypass_models;
    }

    public static List<String> bypassSounds() {
        return instance.resource_pack$protection$obfuscation$bypass_sounds;
    }

    public static List<String> bypassEquipments() {
        return instance.resource_pack$protection$obfuscation$bypass_equipments;
    }

    public static List<String> bypassItemModels() {
        return instance.resource_pack$protection$obfuscation$bypass_item_models;
    }

    public static Key deceiveBukkitMaterial(int id) {
        return instance.block$deceive_bukkit_material$overrides.getOrDefault(id, instance.block$inject_bukkit_material ? null : instance.block$deceive_bukkit_material$default);
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

    public static int customModelDataStartingValue(Key material) {
        if (instance.item$custom_model_data_starting_value$overrides.containsKey(material)) {
            return instance.item$custom_model_data_starting_value$overrides.get(material);
        }
        return instance.item$custom_model_data_starting_value$default;
    }

    public static int codepointStartingValue(Key font) {
        if (instance.image$codepoint_starting_value$overrides.containsKey(font)) {
            return instance.image$codepoint_starting_value$overrides.get(font);
        }
        return instance.image$codepoint_starting_value$default;
    }

    public static int compressionMethod() {
        int id = instance.chunk_system$compression_method;
        if (id <= 0 || id > CompressionMethod.METHOD_COUNT) {
            id = 4;
        }
        return id;
    }

    public static StorageType chunkStorageType() {
        return instance.chunk_system$storage_type;
    }

    public static boolean disableChatReport() {
        return instance.network$disable_chat_report;
    }

    public static boolean enableModChannel() {
        return instance.network$mod_channel$enable;
    }

    public static boolean modChannelRequiresPermission() {
        return instance.network$mod_channel$requires_permission;
    }

    public static boolean enableItemCrypto() {
        return instance.network$item_crypto$enable;
    }

    public static boolean disableItemOperations() {
        return instance.network$disable_item_operations;
    }

    public static boolean interceptSystemChat() {
        return instance.network$intercept_packets$system_chat;
    }

    public static boolean interceptTabList() {
        return instance.network$intercept_packets$tab_list;
    }

    public static boolean interceptActionBar() {
        return instance.network$intercept_packets$actionbar;
    }

    public static boolean interceptTitle() {
        return instance.network$intercept_packets$title;
    }

    public static boolean interceptBossBar() {
        return instance.network$intercept_packets$bossbar;
    }

    public static boolean interceptContainer() {
        return instance.network$intercept_packets$container;
    }

    public static boolean interceptTeam() {
        return instance.network$intercept_packets$team;
    }

    public static boolean interceptEntityName() {
        return instance.network$intercept_packets$entity_name;
    }

    public static boolean interceptScoreboard() {
        return instance.network$intercept_packets$scoreboard;
    }

    public static boolean interceptTextDisplay() {
        return instance.network$intercept_packets$text_display;
    }

    public static boolean interceptArmorStand() {
        return instance.network$intercept_packets$armor_stand;
    }

    public static boolean interceptPlayerInfo() {
        return instance.network$intercept_packets$player_info;
    }

    public static boolean interceptSetScore() {
        return instance.network$intercept_packets$set_score;
    }

    public static boolean interceptItem() {
        return instance.network$intercept_packets$item;
    }

    public static boolean interceptAdvancement() {
        return instance.network$intercept_packets$advancement;
    }

    public static boolean interceptPlayerChat() {
        return instance.network$intercept_packets$player_chat;
    }

    public static boolean interceptDialog() {
        return instance.network$intercept_packets$dialog;
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

    public static boolean enableFurnitureLightSystem() {
        return instance.furniture$light_system;
    }

    public static boolean enableChunkCache() {
        return instance.chunk_system$cache_system;
    }

    public static boolean addNonItalicTag() {
        return instance.item$non_italic_tag;
    }

    public static boolean injectPaletteOrSection() {
        return instance.chunk_system$injection$target;
    }

    public static boolean validateResourcePack() {
        return instance.resource_pack$validation$enable;
    }

    public static boolean fixTextureAtlas() {
        return instance.resource_pack$validation$fix_atlas;
    }

    public static boolean fixMissingTexture() {
        return instance.resource_pack$validation$fix_missing_texture;
    }

    public static boolean fixTexturesFormat() {
        return instance.resource_pack$validation$fallback_models$fix_textures_format;
    }

    public static boolean fixRotationAngle() {
        return instance.resource_pack$validation$fallback_models$fix_element_rotation_angle;
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

    public static String packDescription() {
        return instance.resource_pack$description;
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

    public static boolean recipeInjectBlockEntities() {
        return instance.recipe$inject_block_entities;
    }

    public static List<String> lootEntitySources() {
        return instance.loot$entity_sources;
    }

    public static boolean unlockOnIngredientObtained() {
        return instance.recipe$unlock_on_ingredient_obtained;
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

    public static boolean asyncLightUpdate() {
        return instance.block$light_system$async_update;
    }

    public static Key defaultMaterial() {
        return instance.item$default_material;
    }

    public static Path resourcePackPath() {
        return instance.resource_pack$path;
    }

    public void setObf(boolean enable) {
        this.resource_pack$protection$obfuscation$enable = enable;
    }

    public static boolean optimizeResourcePack() {
        return instance.resource_pack$optimization$enable;
    }

    public static boolean optimizeTexture() {
        return instance.resource_pack$optimization$texture$enable;
    }

    public static Set<String> optimizeTextureExclude() {
        return instance.resource_pack$optimization$texture$exlude;
    }

    public static boolean optimizeJson() {
        return instance.resource_pack$optimization$json$enable;
    }

    public static Set<String> optimizeJsonExclude() {
        return instance.resource_pack$optimization$json$exclude;
    }

    public static int zopfliIterations() {
        return instance.resource_pack$optimization$texture$zopfli_iterations;
    }

    public static boolean enableDefaultDropDisplay() {
        return instance.item$default_drop_display$enable;
    }

    public static String defaultDropDisplayFormat() {
        return instance.item$default_drop_display$format;
    }

    public static boolean enableItemDataFixerUpper() {
        return instance.item$data_fixer_upper$enable;
    }

    public static int itemDataFixerUpperFallbackVersion() {
        return instance.item$data_fixer_upper$fallback_version;
    }

    public static boolean enableEntityCulling() {
        return instance.client_optimization$entity_culling$enable;
    }

    public static int entityCullingViewDistance() {
        return instance.client_optimization$entity_culling$view_distance;
    }

    public static int entityCullingThreads() {
        return instance.client_optimization$entity_culling$threads;
    }

    public static boolean enableEntityCullingRateLimiting() {
        return instance.client_optimization$entity_culling$rate_limiting$enable;
    }

    public static int entityCullingRateLimitingBucketSize() {
        return instance.client_optimization$entity_culling$rate_limiting$bucket_size;
    }

    public static int entityCullingRateLimitingRestorePerTick() {
        return instance.client_optimization$entity_culling$rate_limiting$restore_per_tick;
    }

    public static boolean entityCullingRayTracing() {
        return instance.client_optimization$entity_culling$ray_tracing;
    }

    public static boolean isPacketIgnored(Class<?> clazz) {
        return instance.debug$ignored_packets.contains(clazz.toString());
    }

    public static boolean multiThreadedConfigLoad() {
        return instance.misc$multi_threaded_configuration_load;
    }

    public static boolean enableBedrockEditionSupport() {
        return instance.bedrock_edition_support$enable;
    }

    public static String bedrockEditionPlayerPrefix() {
        return instance.bedrock_edition_support$player_prefix;
    }

    public static boolean enableMapPluginCompatibility() {
        return instance.resource_pack$map_plugin_compatibility$enable;
    }

    public static Path mapPluginCompatibilityPath() {
        return instance.resource_pack$map_plugin_compatibility$path;
    }

    public static boolean createUnprotectedCopy() {
        return instance.resource_pack$protection$unprotected_copy$enable;
    }

    public static Path unprotectedCopyPath() {
        return instance.resource_pack$protection$unprotected_copy$path;
    }

    public static boolean enableProxy() {
        return instance.resource_pack$delivery$proxy$enable;
    }

    public static int proxyPort() {
        return instance.resource_pack$delivery$proxy$port;
    }

    public static String proxyHost() {
        return instance.resource_pack$delivery$proxy$host;
    }

    public static String proxyUsername() {
        return instance.resource_pack$delivery$proxy$username;
    }

    public static String proxyPassword() {
        return instance.resource_pack$delivery$proxy$password;
    }

    public static String proxyScheme() {
        return instance.resource_pack$delivery$proxy$scheme;
    }

    public static boolean enablePackSquash() {
        return instance.resource_pack$pack_squash$enable;
    }

    public static Path packSquashPath() {
        return instance.resource_pack$pack_squash$software_path;
    }

    public static Path packSquashConfigPath() {
        return instance.resource_pack$pack_squash$config_path;
    }

    public YamlDocument loadYamlConfig(String filePath, GeneralSettings generalSettings, LoaderSettings loaderSettings, DumperSettings dumperSettings, UpdaterSettings updaterSettings) {
        try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
            return YamlDocument.create(inputStream, this.plugin.resourceStream(filePath), generalSettings, loaderSettings, dumperSettings, updaterSettings);
        } catch (IOException e) {
            this.plugin.logger().error("Failed to load config " + filePath, e);
            return null;
        }
    }

    public YamlDocument loadYamlData(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return YamlDocument.create(inputStream);
        } catch (IOException e) {
            this.plugin.logger().error("Failed to load config " + file, e);
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
        return FileUtils.isAbsolute(path) ? Path.of(path) : this.plugin.dataFolderPath().resolve(path);
    }

    public YamlDocument settings() {
        if (this.config == null) {
            throw new IllegalStateException("Main config not loaded");
        }
        return this.config;
    }

    public static Config instance() {
        return instance;
    }
}
