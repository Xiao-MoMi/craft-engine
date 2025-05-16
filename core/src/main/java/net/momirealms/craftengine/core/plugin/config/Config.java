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
import net.momirealms.craftengine.core.pack.conflict.resolution.ResolutionConditional;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.filter.DisconnectLogFilter;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.world.InjectionTarget;
import net.momirealms.craftengine.core.world.chunk.storage.CompressionMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Config {
    private static Config instance;
    protected final CraftEngine plugin;
    private final Path configFilePath;
    private final String configVersion;
    private YamlDocument config;

    protected boolean firstTime = true;
    protected boolean debug;
    protected boolean checkUpdate;
    protected boolean metrics;
    protected boolean filterConfigurationPhaseDisconnect;

    protected boolean resource_pack$remove_tinted_leaves_particle;
    protected boolean resource_pack$generate_mod_assets;
    protected boolean resource_pack$override_uniform_font;
    protected List<ResolutionConditional> resource_pack$duplicated_files_handler;
    protected List<String> resource_pack$merge_external_folders;
    protected List<String> resource_pack$merge_external_zips;

    protected boolean resource_pack$protection$crash_tools$method_1;
    protected boolean resource_pack$protection$crash_tools$method_2;
    protected boolean resource_pack$protection$crash_tools$method_3;

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
    protected int resource_pack$protection$obfuscation$resource_location$random_atlas$amount;
    protected boolean resource_pack$protection$obfuscation$resource_location$random_atlas$use_double;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_textures;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_models;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_sounds;
    protected List<String> resource_pack$protection$obfuscation$resource_location$bypass_equipments;

    protected float resource_pack$supported_version$min;
    protected float resource_pack$supported_version$max;

    protected boolean resource_pack$delivery$kick_if_declined;
    protected boolean resource_pack$delivery$send_on_join;
    protected boolean resource_pack$delivery$resend_on_upload;
    protected boolean resource_pack$delivery$auto_upload;
    protected Path resource_pack$delivery$file_to_upload;
    protected Component resource_pack$send$prompt;

    protected int performance$max_block_chain_update_limit;
    protected int performance$max_emojis_per_parse;

    protected boolean light_system$force_update_light;
    protected boolean light_system$enable;

    protected int chunk_system$compression_method;
    protected boolean chunk_system$restore_vanilla_blocks_on_chunk_unload;
    protected boolean chunk_system$restore_custom_blocks_on_chunk_load;
    protected boolean chunk_system$sync_custom_blocks_on_chunk_load;
    protected boolean chunk_system$cache_system;
    protected boolean chunk_system$injection$use_fast_method;
    protected boolean chunk_system$injection$target;

    protected boolean furniture$handle_invalid_furniture_on_chunk_load$enable;
    protected Map<String, String> furniture$handle_invalid_furniture_on_chunk_load$mapping;
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

    protected boolean item$non_italic_tag;

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

    protected boolean emoji$chat;
    protected boolean emoji$book;
    protected boolean emoji$anvil;
    protected boolean emoji$sign;

    public Config(CraftEngine plugin) {
        this.plugin = plugin;
        this.configVersion = PluginProperties.getValue("config");
        this.configFilePath = this.plugin.dataFolderPath().resolve("config.yml");
        instance = this;
    }

    public void load() {
        if (Files.exists(this.configFilePath)) {
            this.config = this.loadYamlData(this.configFilePath.toFile());
            String configVersion = config.getString("config-version");
            if (configVersion.equals(this.configVersion)) {
                loadSettings();
                return;
            }
        }
        this.updateConfigVersion();
        loadSettings();
    }

    private void updateConfigVersion() {
        this.config = this.loadYamlConfig(
                "config.yml",
                GeneralSettings.builder()
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
                        .build()
        );
        try {
            config.save(new File(plugin.dataFolderFile(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSettings() {
        YamlDocument config = settings();
        plugin.translationManager().forcedLocale(TranslationManager.parseLocale(config.getString("forced-locale", "")));

        // basics
        debug = config.getBoolean("debug", false);
        metrics = config.getBoolean("metrics", false);
        checkUpdate = config.getBoolean("update-checker", false);
        filterConfigurationPhaseDisconnect = config.getBoolean("filter-configuration-phase-disconnect", false);
        DisconnectLogFilter.instance().setEnable(filterConfigurationPhaseDisconnect);

        // resource pack
        resource_pack$override_uniform_font = config.getBoolean("resource-pack.override-uniform-font", false);
        resource_pack$generate_mod_assets = config.getBoolean("resource-pack.generate-mod-assets", false);
        resource_pack$remove_tinted_leaves_particle = config.getBoolean("resource-pack.remove-tinted-leaves-particle", true);
        resource_pack$supported_version$min = getVersion(config.get("resource-pack.supported-version.min", "1.20").toString());
        resource_pack$supported_version$max = getVersion(config.get("resource-pack.supported-version.max", "LATEST").toString());
        resource_pack$merge_external_folders = config.getStringList("resource-pack.merge-external-folders");
        resource_pack$merge_external_zips = config.getStringList("resource-pack.merge-external-zip-files");
        resource_pack$delivery$send_on_join = config.getBoolean("resource-pack.delivery.send-on-join", true);
        resource_pack$delivery$resend_on_upload = config.getBoolean("resource-pack.delivery.resend-on-upload", true);
        resource_pack$delivery$kick_if_declined = config.getBoolean("resource-pack.delivery.kick-if-declined", true);
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
        resource_pack$protection$obfuscation$resource_location$random_atlas$amount = config.getInt("resource-pack.protection.obfuscation.resource-location.random-atlas.amount", 5);
        resource_pack$protection$obfuscation$resource_location$random_atlas$use_double = config.getBoolean("resource-pack.protection.obfuscation.resource-location.random-atlas.use-double", true);
        resource_pack$protection$obfuscation$resource_location$bypass_textures = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-textures");
        resource_pack$protection$obfuscation$resource_location$bypass_models = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-models");
        resource_pack$protection$obfuscation$resource_location$bypass_sounds = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-sounds");
        resource_pack$protection$obfuscation$resource_location$bypass_equipments = config.getStringList("resource-pack.protection.obfuscation.resource-location.bypass-equipments");

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

        // item
        item$non_italic_tag = config.getBoolean("item.non-italic-tag", false);

        // performance
        performance$max_block_chain_update_limit = config.getInt("performance.max-block-chain-update-limit", 64);
        performance$max_emojis_per_parse = config.getInt("performance.max-emojis-per-parse", 32);

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

        // furniture
        furniture$handle_invalid_furniture_on_chunk_load$enable = config.getBoolean("furniture.handle-invalid-furniture-on-chunk-load.enable", false);
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String furniture : config.getStringList("furniture.handle-invalid-furniture-on-chunk-load.remove")) {
            builder.put(furniture, "");
        }
        if (config.contains("furniture.handle-invalid-furniture-on-chunk-load.convert")) {
            Section section = config.getSection("furniture.handle-invalid-furniture-on-chunk-load.convert");
            if (section != null) {
                for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
                    builder.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        furniture$handle_invalid_furniture_on_chunk_load$mapping = builder.build();
        furniture$hide_base_entity = config.getBoolean("furniture.hide-base-entity", true);
        furniture$collision_entity_type = ColliderType.valueOf(config.getString("furniture.collision-entity-type", "interaction").toUpperCase(Locale.ENGLISH));

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

        // emoji
        emoji$chat = config.getBoolean("emoji.chat", true);
        emoji$anvil = config.getBoolean("emoji.anvil", true);
        emoji$book = config.getBoolean("emoji.book", true);
        emoji$sign = config.getBoolean("emoji.sign", true);

        Class<?> modClazz = ReflectionUtils.getClazz(CraftEngine.MOD_CLASS);
        if (modClazz != null) {
            Method setMaxChainMethod = ReflectionUtils.getStaticMethod(modClazz, void.class, new String[] {"setMaxChainUpdate"}, int.class);
            try {
                assert setMaxChainMethod != null;
                setMaxChainMethod.invoke(null, performance$max_block_chain_update_limit);
            } catch (IllegalAccessException | InvocationTargetException e) {
                plugin.logger().warn("Failed to set max chain update", e);
            }
        }

        firstTime = false;
    }

    private static float getVersion(String version) {
        if (version.equalsIgnoreCase("LATEST")) {
            version = PluginProperties.getValue("latest-version");
        }
        String[] split = version.split("\\.", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        return Float.parseFloat(split[1]);
    }

    public static String configVersion() {
        return instance.configVersion;
    }

    public static boolean debug() {
        return instance.debug;
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

    public static int maxChainUpdate() {
        return instance.performance$max_block_chain_update_limit;
    }

    public static int maxEmojisPerParse() {
        return instance.performance$max_emojis_per_parse;
    }

    public static boolean handleInvalidFurniture() {
        return instance.furniture$handle_invalid_furniture_on_chunk_load$enable;
    }

    public static Map<String, String> furnitureMappings() {
        return instance.furniture$handle_invalid_furniture_on_chunk_load$mapping;
    }

    public static boolean forceUpdateLight() {
        return instance.light_system$force_update_light;
    }

    public static boolean enableLightSystem() {
        return instance.light_system$enable;
    }

    public static float packMinVersion() {
        return instance.resource_pack$supported_version$min;
    }

    public static float packMaxVersion() {
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

    public static boolean nonItalic() {
        return instance.item$non_italic_tag;
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

    public static boolean kickOnDeclined() {
        return instance.resource_pack$delivery$kick_if_declined;
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

    public static int atlasAmount() {
        return instance.resource_pack$protection$obfuscation$resource_location$random_atlas$amount;
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
        return instance.emoji$sign;
    }

    public static boolean allowEmojiChat() {
        return instance.emoji$chat;
    }

    public static boolean allowEmojiAnvil() {
        return instance.emoji$anvil;
    }

    public static boolean allowEmojiBook() {
        return instance.emoji$book;
    }

    public static ColliderType colliderType() {
        return instance.furniture$collision_entity_type;
    }

    public static boolean enableChunkCache() {
        return instance.chunk_system$cache_system;
    }

    public static boolean fastInjection() {
        return instance.chunk_system$injection$use_fast_method;
    }

    public static boolean injectionTarget() {
        return instance.chunk_system$injection$target;
    }

    public YamlDocument loadOrCreateYamlData(String fileName) {
        File file = new File(this.plugin.dataFolderFile(), fileName);
        if (!file.exists()) {
            this.plugin.saveResource(fileName);
        }
        return this.loadYamlData(file);
    }

    public YamlDocument loadYamlConfig(String filePath, GeneralSettings generalSettings, LoaderSettings loaderSettings, DumperSettings dumperSettings, UpdaterSettings updaterSettings) {
        try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
            return YamlDocument.create(inputStream, this.plugin.resourceStream(filePath), generalSettings, loaderSettings, dumperSettings, updaterSettings);
        } catch (IOException e) {
            this.plugin.logger().severe("Failed to load config " + filePath, e);
            return null;
        }
    }

    public YamlDocument loadYamlData(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
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
