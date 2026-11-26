package net.momirealms.craftengine.bedrock;

public final class BedrockPackLayout {
    public static final String NAMESPACE = "ce";
    public static final String ID_PREFIX = NAMESPACE + ':';
    public static final String PATH_PREFIX = NAMESPACE + '/';
    public static final String GEOMETRY_PREFIX = "geometry." + NAMESPACE + '.';
    public static final String DYED_ICON_PREFIX = NAMESPACE + "_dye:";

    public static final String BOSS_BARS = "craftengine/boss_bars.json";
    public static final String BOSS_BAR_FONT_BASELINES = "font_baselines";
    public static final String SOUND_MAPPINGS = "craftengine/sounds.json";
    public static final String MODEL_DISPLAYS = "craftengine/model_displays.json";
    public static final String ELYTRA_VARIANTS = "craftengine/elytra_variants.json";
    public static final String NATIVE_MODELS = "craftengine/native_models.json";
    public static final String TRANSLATIONS = "craftengine/translations.json";
    public static final String SCOREBOARD = "craftengine/scoreboard.json";
    public static final String TITLE = "craftengine/title.json";
    public static final String FORCE_JAVA_LAYOUT = "force_java_layout";
    public static final String TEXT_VERTICAL_OFFSETS = "vertical_offsets";
    public static final String FONTS = "craftengine/fonts.json";
    public static final String CRAFTENGINE_TEXTURES = "textures/" + NAMESPACE;
    public static final String NATIVE_MODEL_TEXTURES = CRAFTENGINE_TEXTURES + "/native";
    public static final String CRAFTENGINE_PARTICLE_TEXTURES = "textures/particle/" + NAMESPACE;
    public static final String CRAFTENGINE_SOUNDS = "sounds/" + NAMESPACE;
    public static final String TERRAIN_TEXTURE_ATLAS = "textures/terrain_texture.json";
    public static final String FLIPBOOK_TEXTURES = "textures/flipbook_textures.json";
    public static final String DYE_COLOR = "dye_color";
    public static final String ICON = "icon";
    public static final String DYED_ICON = "dyed_icon";
    public static final String MAIN_HAND_DYE = ID_PREFIX + "main_hand_dye";
    public static final String OFF_HAND_DYE = ID_PREFIX + "off_hand_dye";
    public static final String HEAD_DYE = ID_PREFIX + "head_dye";
    public static final String DISPLAY_DYE = ID_PREFIX + "display_dye";
    public static final String ELYTRA_VARIANT_PROPERTY = ID_PREFIX + "elytra_variant";
    public static final String DISPLAY_BLOCK = "display_block";
    public static final String GEYSER_CUSTOM_BLOCK_PREFIX = "geyser_custom:";
    public static final String NAME = "name";
    public static final String GEOMETRY = "geometry";
    public static final String MATERIALS = "materials";
    public static final String TINT_METHODS = "tint_methods";
    public static final String ITEM_TEXTURE_ATLAS = "textures/item_texture.json";
    public static final String PLAYER_ANIMATION_CONTROLLER = "animation_controllers/" + PATH_PREFIX + "shared/player.animation_controllers.json";

    public static final String KIND = "kind";
    public static final String FORMAT_VERSION = "format_version";
    public static final String SOURCE_VERSION = "source_version";
    public static final String ENTRIES = "entries";
    public static final String WORLD_STATE_MAPPINGS = "world_state_mappings";
    public static final String VANILLA_OVERRIDES = "vanilla_overrides";

    public static final String BEDROCK_IDENTIFIER = "bedrock_identifier";
    public static final String HEAD_IDENTIFIER = "head_identifier";
    public static final String WORLD_VIEW = "world_view";
    public static final String TINT_SOURCES = "tint_sources";
    public static final String ITEM_TYPE = "item_type";
    public static final String VANILLA_MODEL_OVERRIDE = "vanilla_model_override";
    public static final String INVENTORY_VISIBLE = "inventory_visible";
    public static final String MODEL = "model";
    public static final String COMPONENTS = "components";
    public static final String RESOURCES = "resources";
    public static final String VARIANTS = "variants";
    public static final String CONDITIONS = "conditions";
    public static final String PROPERTY = "property";
    public static final String EXPECTED = "expected";
    public static final String VALUES = "values";
    public static final String NEGATED = "negated";
    public static final String SCALE = "scale";
    public static final String MIN_INCLUSIVE = "min_inclusive";
    public static final String MAX_EXCLUSIVE = "max_exclusive";

    public static final String UNSUPPORTED_REASON = "unsupported_reason";
    public static final String GENERATED = "generated";
    public static final String CLIENT_BEHAVIOR = "client_behavior";
    public static final String PLACES_BLOCK = "places_block";
    public static final String TRIMMABLE_ARMOR = "trimmable_armor";
    public static final String USE = "use";
    public static final String ANIMATION = "animation";
    public static final String MAX_DURATION_TICKS = "max_duration_ticks";
    public static final String CHARGE_DURATION_TICKS = "charge_duration_ticks";
    public static final String NATIVE_USE_ON_RELEASE = "nativeUseOnRelease";

    private BedrockPackLayout() {
    }

    public static String dyedIconKey(String identifier) {
        return DYED_ICON_PREFIX + identifier.replace(':', '/');
    }
}
