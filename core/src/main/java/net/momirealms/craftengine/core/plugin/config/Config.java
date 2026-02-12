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
import net.momirealms.craftengine.core.pack.conflict.resolution.ConditionalResolution;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
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
    protected Locale forcedLocale;

    protected boolean misc$filterConfigurationPhaseDisconnect;
    protected boolean misc$delayConfigurationLoad;
    protected boolean misc$inject_packet_vents;

    protected boolean debug$common;
    protected boolean debug$packet;
    protected boolean debug$item;
    protected boolean debug$furniture;
    protected boolean debug$resource_pack;
    protected boolean debug$block;
    protected boolean debug$entity_culling;
    protected Set<String> debug$ignored_packets;

    protected boolean resource_pack$remove_tinted_leaves_particle;
    protected boolean resource_pack$generate_mod_assets;
    protected boolean resource_pack$override_uniform_font;
    protected List<ConditionalResolution> resource_pack$duplicated_files_handler;
    protected List<String> resource_pack$merge_external_folders;
    protected List<String> resource_pack$merge_external_zips;
    protected Set<String> resource_pack$exclude_file_extensions;
    protected Path resource_pack$path;
    protected String resource_pack$description;

    protected boolean resource_pack$protection$unprotected_copy;
    protected boolean resource_pack$protection$crash_tools$method_1;
    protected boolean resource_pack$protection$crash_tools$method_2;
    protected boolean resource_pack$protection$crash_tools$method_3;
    protected boolean resource_pack$protection$crash_tools$method_4;
    protected boolean resource_pack$protection$crash_tools$method_5;
    protected boolean resource_pack$protection$crash_tools$method_6;
    protected boolean resource_pack$protection$crash_tools$method_7;
    protected boolean resource_pack$protection$crash_tools$method_8;
    protected boolean resource_pack$protection$crash_tools$method_9;

    protected boolean resource_pack$validation$enable;
    protected boolean resource_pack$validation$fix_atlas;
    protected boolean resource_pack$validation$fix_missing_texture;
    protected boolean resource_pack$exclude_core_shaders;

    protected boolean resource_pack$protection$obfuscation$enable;
    protected long resource_pack$protection$obfuscation$seed;
    protected boolean resource_pack$protection$fake_directory;
    protected boolean resource_pack$protection$escape_json;
    protected boolean resource_pack$protection$break_texture;
    protected boolean resource_pack$protection$obfuscation$path$anti_unzip;
    protected boolean resource_pack$protection$incorrect_crc;
    protected boolean resource_pack$protection$fake_file_size;
    protected NumberProvider resource_pack$protection$obfuscation$overlay$length;
    protected NumberProvider resource_pack$protection$obfuscation$namespace$length;
    protected int resource_pack$protection$obfuscation$namespace$amount;
    protected String resource_pack$protection$obfuscation$path$block_source;
    protected String resource_pack$protection$obfuscation$path$item_source;
    protected NumberProvider resource_pack$protection$obfuscation$path$depth;
    protected NumberProvider resource_pack$protection$obfuscation$path$length;
    protected int resource_pack$protection$obfuscation$atlas$images_per_canvas;
    protected String resource_pack$protection$obfuscation$atlas$prefix;
    protected List<String> resource_pack$protection$obfuscation$bypass_textures;
    protected List<String> resource_pack$protection$obfuscation$bypass_models;
    protected List<String> resource_pack$protection$obfuscation$bypass_sounds;
    protected List<String> resource_pack$protection$obfuscation$bypass_equipments;

    protected boolean resource_pack$optimization$enable;
    protected boolean resource_pack$optimization$texture$enable;
    protected Set<String> resource_pack$optimization$texture$exlude;
    protected int resource_pack$optimization$texture$zopfli_iterations;
    protected boolean resource_pack$optimization$json$enable;
    protected Set<String> resource_pack$optimization$json$exclude;

    protected MinecraftVersion resource_pack$supported_version$min;
    protected MinecraftVersion resource_pack$supported_version$max;
    protected String resource_pack$overlay_format;

    protected boolean resource_pack$delivery$kick_if_declined;
    protected boolean resource_pack$delivery$kick_if_failed_to_apply;
    protected boolean resource_pack$delivery$send_on_join;
    protected boolean resource_pack$delivery$resend_on_upload;
    protected boolean resource_pack$delivery$auto_upload;
    protected boolean resource_pack$delivery$strict_player_uuid_validation;
    protected Path resource_pack$delivery$file_to_upload;
    protected Component resource_pack$send$prompt;

    protected boolean light_system$force_update_light;
    protected boolean light_system$async_update;
    protected boolean light_system$enable;

    protected int chunk_system$compression_method;
    protected boolean chunk_system$restore_vanilla_blocks_on_chunk_unload;
    protected boolean chunk_system$restore_custom_blocks_on_chunk_load;
    protected boolean chunk_system$sync_custom_blocks_on_chunk_load;
    protected boolean chunk_system$cache_system = true;
    protected boolean chunk_system$injection$use_fast_method;
    protected boolean chunk_system$injection$target;
    protected boolean chunk_system$process_invalid_furniture$enable;
    protected Map<String, String> chunk_system$process_invalid_furniture$mapping;
    protected boolean chunk_system$process_invalid_blocks$enable;
    protected Map<String, String> chunk_system$process_invalid_blocks$mapping;

    protected boolean furniture$hide_base_entity;
    protected ColliderType furniture$collision_entity_type;

    protected boolean block$sound_system$enable;
    protected boolean block$sound_system$process_cancelled_events$step;
    protected boolean block$sound_system$process_cancelled_events$break;
    protected boolean block$simplify_adventure_break_check;
    protected boolean block$simplify_adventure_place_check;
    protected boolean block$predict_breaking;
    protected int block$predict_breaking_interval;
    protected double block$extended_interaction_range;
    protected boolean block$chunk_relighter;
    protected Key block$deceive_bukkit_material$default;
    protected Map<Integer, Key> block$deceive_bukkit_material$overrides;
    protected int block$serverside_blocks = -1;

    protected boolean recipe$enable;
    protected boolean recipe$disable_vanilla_recipes$all;
    protected Set<Key> recipe$disable_vanilla_recipes$list;
    protected List<String> recipe$ingredient_sources;

    protected boolean image$illegal_characters_filter$command;
    protected boolean image$illegal_characters_filter$chat;
    protected boolean image$illegal_characters_filter$anvil;
    protected boolean image$illegal_characters_filter$sign;
    protected boolean image$illegal_characters_filter$book;
    protected int image$codepoint_starting_value$default;
    protected Map<Key, Integer> image$codepoint_starting_value$overrides;

    protected boolean network$intercept_packets$system_chat;
    protected boolean network$intercept_packets$tab_list;
    protected boolean network$intercept_packets$actionbar;
    protected boolean network$intercept_packets$title;
    protected boolean network$intercept_packets$bossbar;
    protected boolean network$intercept_packets$container;
    protected boolean network$intercept_packets$team;
    protected boolean network$intercept_packets$scoreboard;
    protected boolean network$intercept_packets$entity_name;
    protected boolean network$intercept_packets$text_display;
    protected boolean network$intercept_packets$armor_stand;
    protected boolean network$intercept_packets$player_info;
    protected boolean network$intercept_packets$set_score;
    protected boolean network$intercept_packets$item;
    protected boolean network$intercept_packets$advancement;
    protected boolean network$intercept_packets$player_chat;
    protected boolean network$disable_item_operations;
    protected boolean network$disable_chat_report;

    protected boolean item$client_bound_model;
    protected boolean item$non_italic_tag;
    protected boolean item$update_triggers$attack;
    protected boolean item$update_triggers$click_in_inventory;
    protected boolean item$update_triggers$drop;
    protected boolean item$update_triggers$pick_up;
    protected int item$custom_model_data_starting_value$default;
    protected Map<Key, Integer> item$custom_model_data_starting_value$overrides;
    protected boolean item$always_use_item_model;
    protected boolean item$always_use_custom_model_data;
    protected boolean item$always_generate_model_overrides;
    protected String item$default_material = "";
    protected boolean item$default_drop_display$enable = false;
    protected String item$default_drop_display$format = null;
    protected boolean item$data_fixer_upper$enable = true;
    protected int item$data_fixer_upper$fallback_version = 3463;

    protected String equipment$sacrificed_vanilla_armor$type;
    protected Key equipment$sacrificed_vanilla_armor$asset_id;
    protected Key equipment$sacrificed_vanilla_armor$humanoid;
    protected Key equipment$sacrificed_vanilla_armor$humanoid_leggings;

    protected boolean emoji$contexts$chat;
    protected boolean emoji$contexts$book;
    protected boolean emoji$contexts$anvil;
    protected boolean emoji$contexts$sign;
    protected int emoji$max_emojis_per_parse;

    protected boolean client_optimization$entity_culling$enable;
    protected int client_optimization$entity_culling$view_distance;
    protected int client_optimization$entity_culling$threads;
    protected boolean client_optimization$entity_culling$ray_tracing;
    protected boolean client_optimization$entity_culling$rate_limiting$enable;
    protected int client_optimization$entity_culling$rate_limiting$bucket_size;
    protected int client_optimization$entity_culling$rate_limiting$restore_per_tick;

    protected boolean bedrock_edition_support$enable;
    protected String bedrock_edition_support$player_prefix;

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
            this.plugin.logger().severe("Failed to update config.yml", e);
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
        forcedLocale = TranslationManager.parseLocale(config.getString("forced-locale", ""));
    }

    @SuppressWarnings("DuplicatedCode")
    public void loadFullSettings() {
        YamlDocument config = settings();
        forcedLocale = TranslationManager.parseLocale(config.getString("forced-locale", ""));
        misc$delayConfigurationLoad = config.getBoolean("misc.delay-configuration-load", false);
        misc$inject_packet_vents = config.getBoolean("misc.inject-packetevents", false);

        // basics
        metrics = config.getBoolean("metrics", false);
        checkUpdate = config.getBoolean("update-checker", false);
        misc$filterConfigurationPhaseDisconnect = config.getBoolean("misc.filter-configuration-phase-disconnect", false);
        DisconnectLogFilter.instance().setEnable(misc$filterConfigurationPhaseDisconnect);

        // debug
        debug$common = config.getBoolean("debug.common", false);
        debug$packet = config.getBoolean("debug.packet", false);
        debug$item = config.getBoolean("debug.item", false);
        debug$furniture = config.getBoolean("debug.furniture", false);
        debug$resource_pack = config.getBoolean("debug.resource-pack", false);
        debug$block = config.getBoolean("debug.block", false);
        debug$entity_culling = config.getBoolean("debug.entity-culling", false);
        debug$ignored_packets = new HashSet<>(config.getStringList("debug.ignored-packets"));

        // resource pack
        resource_pack$path = resolvePath(config.getString("resource-pack.path", "./generated/resource_pack.zip"));
        resource_pack$description = config.getString("resource-pack.description", "<gray>CraftEngine ResourcePack</gray>");
        resource_pack$override_uniform_font = config.getBoolean("resource-pack.override-uniform-font", false);
        resource_pack$generate_mod_assets = config.getBoolean("resource-pack.generate-mod-assets", false);
        resource_pack$remove_tinted_leaves_particle = config.getBoolean("resource-pack.remove-tinted-leaves-particle", true);
        resource_pack$supported_version$min = getVersion(config.get("resource-pack.supported-version.min", "server").toString());
        resource_pack$supported_version$max = getVersion(config.get("resource-pack.supported-version.max", "latest").toString());
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
        resource_pack$delivery$strict_player_uuid_validation = config.getBoolean("resource-pack.delivery.strict-player-uuid-validation", true);
        resource_pack$delivery$file_to_upload = resolvePath(config.getString("resource-pack.delivery.file-to-upload", "./generated/resource_pack.zip"));
        resource_pack$send$prompt = AdventureHelper.miniMessage().deserialize(config.getString("resource-pack.delivery.prompt", "<yellow>To fully experience our server, please accept our custom resource pack.</yellow>"));
        resource_pack$protection$unprotected_copy = config.getBoolean("resource-pack.protection.unprotected-copy", false);
        resource_pack$protection$crash_tools$method_2 = config.getBoolean("resource-pack.protection.crash-tools.method-2", false);
        resource_pack$protection$crash_tools$method_3 = config.getBoolean("resource-pack.protection.crash-tools.method-3", false);
        resource_pack$protection$crash_tools$method_4 = config.getBoolean("resource-pack.protection.crash-tools.method-4", false);
        resource_pack$protection$crash_tools$method_5 = config.getBoolean("resource-pack.protection.crash-tools.method-5", false);
        resource_pack$protection$crash_tools$method_6 = config.getBoolean("resource-pack.protection.crash-tools.method-6", false);
        resource_pack$protection$crash_tools$method_7 = config.getBoolean("resource-pack.protection.crash-tools.method-7", false);
        resource_pack$protection$crash_tools$method_8 = config.getBoolean("resource-pack.protection.crash-tools.method-8", false);
        resource_pack$protection$crash_tools$method_9 = config.getBoolean("resource-pack.protection.crash-tools.method-9", false);
        resource_pack$protection$obfuscation$enable = VersionHelper.PREMIUM && config.getBoolean("resource-pack.protection.obfuscation.enable", false);
        resource_pack$protection$obfuscation$seed = config.getLong("resource-pack.protection.obfuscation.seed", 0L);
        resource_pack$protection$fake_directory = config.getBoolean("resource-pack.protection.fake-directory", false);
        resource_pack$protection$escape_json = config.getBoolean("resource-pack.protection.escape-json", false);
        resource_pack$protection$break_texture = config.getBoolean("resource-pack.protection.break-texture", false);
        resource_pack$protection$incorrect_crc = config.getBoolean("resource-pack.protection.incorrect-crc", false);
        resource_pack$protection$fake_file_size = config.getBoolean("resource-pack.protection.fake-file-size", false);
        resource_pack$protection$obfuscation$namespace$amount = config.getInt("resource-pack.protection.obfuscation.namespace.amount", 32);
        resource_pack$protection$obfuscation$namespace$length = NumberProviders.fromObject(config.get("resource-pack.protection.obfuscation.namespace.length", 2));
        resource_pack$protection$obfuscation$overlay$length = NumberProviders.fromObject(config.get("resource-pack.protection.obfuscation.overlay.length", 4));
        resource_pack$protection$obfuscation$path$depth = NumberProviders.fromObject(config.get("resource-pack.protection.obfuscation.path.depth", 4));
        resource_pack$protection$obfuscation$path$length = NumberProviders.fromObject(config.get("resource-pack.protection.obfuscation.path.length", 2));
        resource_pack$protection$obfuscation$path$block_source = config.getString("resource-pack.protection.obfuscation.path.block-source", "obf_block");
        resource_pack$protection$obfuscation$path$item_source = config.getString("resource-pack.protection.obfuscation.path.block-source", "obf_item");
        resource_pack$protection$obfuscation$path$anti_unzip = config.getBoolean("resource-pack.protection.obfuscation.path.anti-unzip", false);
        resource_pack$protection$obfuscation$atlas$images_per_canvas = Math.max(0, config.getInt("resource-pack.protection.obfuscation.atlas.images-per-canvas", 256));
        resource_pack$protection$obfuscation$atlas$prefix = config.getString("resource-pack.protection.obfuscation.atlas.prefix", "atlas");
        resource_pack$protection$obfuscation$bypass_textures = config.getStringList("resource-pack.protection.obfuscation.bypass-textures");
        resource_pack$protection$obfuscation$bypass_models = config.getStringList("resource-pack.protection.obfuscation.bypass-models");
        resource_pack$protection$obfuscation$bypass_sounds = config.getStringList("resource-pack.protection.obfuscation.bypass-sounds");
        resource_pack$protection$obfuscation$bypass_equipments = config.getStringList("resource-pack.protection.obfuscation.bypass-equipments");
        resource_pack$optimization$enable = config.getBoolean("resource-pack.optimization.enable", false);
        resource_pack$optimization$texture$enable = config.getBoolean("resource-pack.optimization.texture.enable", true);
        resource_pack$optimization$texture$zopfli_iterations = config.getInt("resource-pack.optimization.texture.zopfli-iterations", 0);
        resource_pack$optimization$texture$exlude = config.getStringList("resource-pack.optimization.texture.exclude").stream().map(p -> {
            if (p.endsWith("/")) return p;
            if (!p.endsWith(".png")) return p + ".png";
            return p;
        }).collect(Collectors.toSet());
        resource_pack$optimization$json$enable = config.getBoolean("resource-pack.optimization.json.enable", true);
        resource_pack$optimization$json$exclude = config.getStringList("resource-pack.optimization.json.exclude").stream().map(p -> {
            if (p.endsWith("/")) return p;
            if (!p.endsWith(".json") && !p.endsWith(".mcmeta")) return p + ".json";
            return p;
        }).collect(Collectors.toSet());
        resource_pack$validation$enable = config.getBoolean("resource-pack.validation.enable", true);
        resource_pack$validation$fix_atlas = config.getBoolean("resource-pack.validation.fix-atlas", true);
        resource_pack$validation$fix_missing_texture = config.getBoolean("resource-pack.validation.fix-missing-texture", true);
        resource_pack$exclude_core_shaders = config.getBoolean("resource-pack.exclude-core-shaders", false);
        resource_pack$overlay_format = config.getString("resource-pack.overlay-format", "overlay_{version}");
        if (!resource_pack$overlay_format.contains("{version}")) {
            TranslationManager.instance().log("warning.config.resource_pack.invalid_overlay_format", resource_pack$overlay_format);
        }

        try {
            resource_pack$duplicated_files_handler = config.getMapList("resource-pack.duplicated-files-handler").stream().map(it -> {
                Map<String, Object> args = MiscUtils.castToMap(it, false);
                return (ConditionalResolution) ConditionalResolution.FACTORY.create(args);
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
        light_system$async_update = config.getBoolean("light-system.async-update", true);
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
        item$client_bound_model = config.getBoolean("item.client-bound-model", true) && VersionHelper.PREMIUM;
        item$non_italic_tag = config.getBoolean("item.non-italic-tag", false);
        item$update_triggers$attack = config.getBoolean("item.update-triggers.attack", false);
        item$update_triggers$click_in_inventory = config.getBoolean("item.update-triggers.click-in-inventory", false);
        item$update_triggers$drop = config.getBoolean("item.update-triggers.drop", false);
        item$update_triggers$pick_up = config.getBoolean("item.update-triggers.pick-up", false);
        item$custom_model_data_starting_value$default = config.getInt("item.custom-model-data-starting-value.default", 10000);
        item$always_use_item_model = config.getBoolean("item.always-use-item-model", true) && VersionHelper.isOrAbove1_21_2();
        item$always_generate_model_overrides = config.getBoolean("item.always-generate-model-overrides", false);
        item$always_use_custom_model_data = item$always_generate_model_overrides || (config.getBoolean("item.always-use-custom-model-data", false) && VersionHelper.isOrAbove1_21_2());
        item$default_material = config.getString("item.default-material", "");
        item$default_drop_display$enable = config.getBoolean("item.default-drop-display.enable", false);
        item$default_drop_display$format = item$default_drop_display$enable ? config.getString("item.default-drop-display.format", "<arg:count>x <name>"): null;
        item$data_fixer_upper$enable = config.getBoolean("item.data-fixer-upper.enable", true);
        item$data_fixer_upper$fallback_version = config.getInt("item.data-fixer-upper.fallback-version", 3463);

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
            item$custom_model_data_starting_value$overrides = customModelDataOverrides;
        } else {
            item$custom_model_data_starting_value$overrides = Map.of();
        }

        // block
        block$sound_system$enable = config.getBoolean("block.sound-system.enable", true);
        block$sound_system$process_cancelled_events$step = config.getBoolean("block.sound-system.process-cancelled-events.step", true);
        block$sound_system$process_cancelled_events$break = config.getBoolean("block.sound-system.process-cancelled-events.break", true);
        block$simplify_adventure_break_check = config.getBoolean("block.simplify-adventure-break-check", false);
        block$simplify_adventure_place_check = config.getBoolean("block.simplify-adventure-place-check", false);
        block$predict_breaking = config.getBoolean("block.predict-breaking.enable", true);
        block$predict_breaking_interval = Math.max(config.getInt("block.predict-breaking.interval", 10), 1);
        block$extended_interaction_range = Math.max(config.getDouble("block.predict-breaking.extended-interaction-range", 0.5), 0.0);
        block$chunk_relighter = config.getBoolean("block.chunk-relighter", true);
        if (firstTime) {
            block$deceive_bukkit_material$default = Key.of(config.getString("block.deceive-bukkit-material.default", "bricks"));
            block$deceive_bukkit_material$overrides = new HashMap<>();
            Section overridesSection = config.getSection("block.deceive-bukkit-material.overrides");
            if (overridesSection != null) {
                for (Map.Entry<String, Object> entry : overridesSection.getStringRouteMappedValues(false).entrySet()) {
                    String key = entry.getKey();
                    Key value = Key.of(String.valueOf(entry.getValue()));
                    if (key.contains("~")) {
                        int min = Integer.parseInt(key.split("~")[0]);
                        int max = Integer.parseInt(key.split("~")[1]);
                        for (int i = min; i <= max; i++) {
                            block$deceive_bukkit_material$overrides.put(i, value);
                        }
                    } else {
                        block$deceive_bukkit_material$overrides.put(Integer.valueOf(key), value);
                    }
                }
            }
            block$serverside_blocks = Math.min(config.getInt("block.serverside-blocks", 2000), 10_0000);
            if (block$serverside_blocks < 0) block$serverside_blocks = 0;
        }

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

        image$codepoint_starting_value$default = config.getInt("image.codepoint-starting-value.default", 0);
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
            image$codepoint_starting_value$overrides = codepointOverrides;
        } else {
            image$codepoint_starting_value$overrides = Map.of();
        }

        if (firstTime) {
            network$disable_chat_report = config.getBoolean("network.disable-chat-report", false);
        }
        network$disable_item_operations = config.getBoolean("network.disable-item-operations", false);
        network$intercept_packets$system_chat = config.getBoolean("network.intercept-packets.system-chat", true);
        network$intercept_packets$tab_list = config.getBoolean("network.intercept-packets.tab-list", true);
        network$intercept_packets$actionbar = config.getBoolean("network.intercept-packets.actionbar", true);
        network$intercept_packets$title = config.getBoolean("network.intercept-packets.title", true);
        network$intercept_packets$bossbar = config.getBoolean("network.intercept-packets.bossbar", true);
        network$intercept_packets$container = config.getBoolean("network.intercept-packets.container", true);
        network$intercept_packets$team = config.getBoolean("network.intercept-packets.team", true);
        network$intercept_packets$scoreboard = config.getBoolean("network.intercept-packets.scoreboard", true);
        network$intercept_packets$entity_name = config.getBoolean("network.intercept-packets.entity-name", false);
        network$intercept_packets$text_display = config.getBoolean("network.intercept-packets.text-display", true);
        network$intercept_packets$armor_stand = config.getBoolean("network.intercept-packets.armor-stand", true);
        network$intercept_packets$player_info = config.getBoolean("network.intercept-packets.player-info", true);
        network$intercept_packets$set_score = config.getBoolean("network.intercept-packets.set-score", true);
        network$intercept_packets$item = config.getBoolean("network.intercept-packets.item", true);
        network$intercept_packets$advancement = config.getBoolean("network.intercept-packets.advancement", true);
        network$intercept_packets$player_chat = config.getBoolean("network.intercept-packets.player-chat", true);

        // emoji
        emoji$contexts$chat = config.getBoolean("emoji.contexts.chat", true);
        emoji$contexts$anvil = config.getBoolean("emoji.contexts.anvil", true);
        emoji$contexts$book = config.getBoolean("emoji.contexts.book", true);
        emoji$contexts$sign = config.getBoolean("emoji.contexts.sign", true);
        emoji$max_emojis_per_parse = config.getInt("emoji.max-emojis-per-parse", 32);

        // client optimization
        if (firstTime) {
            client_optimization$entity_culling$enable = VersionHelper.PREMIUM && config.getBoolean("client-optimization.entity-culling.enable", false);
        }
        client_optimization$entity_culling$view_distance = config.getInt("client-optimization.entity-culling.view-distance", 64);
        client_optimization$entity_culling$threads = config.getInt("client-optimization.entity-culling.threads", 1);
        client_optimization$entity_culling$ray_tracing = client_optimization$entity_culling$enable && config.getBoolean("client-optimization.entity-culling.ray-tracing", true);
        client_optimization$entity_culling$rate_limiting$enable = config.getBoolean("client-optimization.entity-culling.rate-limiting.enable", true);
        client_optimization$entity_culling$rate_limiting$bucket_size = config.getInt("client-optimization.entity-culling.rate-limiting.bucket-size", 300);
        client_optimization$entity_culling$rate_limiting$restore_per_tick = config.getInt("client-optimization.entity-culling.rate-limiting.restore-per-tick", 5);

        // bedrock support
        bedrock_edition_support$enable = config.getBoolean("bedrock-edition-support.enable", true);
        bedrock_edition_support$player_prefix = config.getString("bedrock-edition-support.player-prefix", "!");

        firstTime = false;
    }

    private static MinecraftVersion getVersion(String version) {
        if (version.equalsIgnoreCase("latest")) {
            return MinecraftVersion.byName(PluginProperties.getValue("latest-version"));
        }
        if (version.equalsIgnoreCase("server")) {
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

    public static boolean debugResourcePack() {
        return instance.debug$resource_pack;
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

    public static boolean processCancelledStep() {
        return instance.block$sound_system$process_cancelled_events$step;
    }

    public static boolean processCancelledBreak() {
        return instance.block$sound_system$process_cancelled_events$break;
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

    public static Key deceiveBukkitMaterial(int id) {
        return instance.block$deceive_bukkit_material$overrides.getOrDefault(id, instance.block$deceive_bukkit_material$default);
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

    public static boolean disableChatReport() {
        return instance.network$disable_chat_report;
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
        return instance.resource_pack$validation$enable;
    }

    public static boolean fixTextureAtlas() {
        return instance.resource_pack$validation$fix_atlas;
    }

    public static boolean fixMissingTexture() {
        return instance.resource_pack$validation$fix_missing_texture;
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

    public static boolean enableChunkRelighter() {
        return instance.block$chunk_relighter;
    }

    public static boolean asyncLightUpdate() {
        return instance.light_system$async_update;
    }

    public static String defaultMaterial() {
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

    public static boolean enableBedrockEditionSupport() {
        return instance.bedrock_edition_support$enable;
    }

    public static String bedrockEditionPlayerPrefix() {
        return instance.bedrock_edition_support$player_prefix;
    }

    public static boolean createUnprotectedCopy() {
        return instance.resource_pack$protection$unprotected_copy;
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
        return FileUtils.isAbsolute(path) ? Path.of(path) : this.plugin.dataFolderPath().resolve(path);
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
