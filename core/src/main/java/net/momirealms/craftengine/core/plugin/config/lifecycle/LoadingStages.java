package net.momirealms.craftengine.core.plugin.config.lifecycle;

public final class LoadingStages {
    private LoadingStages() {}

    public static final LoadingStage TEMPLATE = new LoadingStage("templates");
    public static final LoadingStage BLOCK_STATE_MAPPING = new LoadingStage("block state mappings");
    public static final LoadingStage GLOBAL_VARIABLE = new LoadingStage("global variables");
    public static final LoadingStage TRANSLATION = new LoadingStage("translations");
    public static final LoadingStage EQUIPMENT = new LoadingStage("equipments");
    public static final LoadingStage ITEM = new LoadingStage("items");
    public static final LoadingStage BLOCK = new LoadingStage("blocks");
    public static final LoadingStage FURNITURE = new LoadingStage("furniture");
    public static final LoadingStage IMAGE = new LoadingStage("images");
    public static final LoadingStage RECIPE = new LoadingStage("recipes");
    public static final LoadingStage CATEGORY = new LoadingStage("categories");
    public static final LoadingStage SOUND = new LoadingStage("sounds");
    public static final LoadingStage JUKEBOX_SONG = new LoadingStage("jukebox songs");
    public static final LoadingStage LOOT = new LoadingStage("loots");
    public static final LoadingStage EMOJI = new LoadingStage("emojis");
    public static final LoadingStage LANG = new LoadingStage("lang");
    public static final LoadingStage SKIP_OPTIMIZATION = new LoadingStage("skip optimization");
    public static final LoadingStage CONFIGURED_FEATURE = new LoadingStage("configured features");
    public static final LoadingStage PLACED_FEATURE = new LoadingStage("placed features");
}
