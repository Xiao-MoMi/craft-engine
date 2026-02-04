package net.momirealms.craftengine.core.plugin.config.lifecycle;

public final class LoadingStages {
    private LoadingStages() {}

    public static final LoadingStage TEMPLATE = new LoadingStage("template");
    public static final LoadingStage BLOCK_STATE_MAPPINGS = new LoadingStage("block_state_mapping");
    public static final LoadingStage GLOBAL_VAR = new LoadingStage("global_var");
    public static final LoadingStage TRANSLATION = new LoadingStage("translation");
    public static final LoadingStage EQUIPMENT = new LoadingStage("equipment");
    public static final LoadingStage ITEM = new LoadingStage("item");
    public static final LoadingStage BLOCK = new LoadingStage("block");
    public static final LoadingStage FURNITURE = new LoadingStage("furniture");
    public static final LoadingStage IMAGE = new LoadingStage("image");
    public static final LoadingStage RECIPE = new LoadingStage("recipe");
    public static final LoadingStage CATEGORY = new LoadingStage("category");
    public static final LoadingStage SOUND = new LoadingStage("sound");
    public static final LoadingStage JUKEBOX_SONG = new LoadingStage("jukebox_song");
    public static final LoadingStage LOOT = new LoadingStage("loot");
    public static final LoadingStage EMOJI = new LoadingStage("emoji");
    public static final LoadingStage ADVANCEMENT = new LoadingStage("advancement");
    public static final LoadingStage LANG = new LoadingStage("lang");
    public static final LoadingStage SKIP_OPTIMIZATION = new LoadingStage("skip_optimization");
    public static final LoadingStage CONFIGURED_FEATURE = new LoadingStage("configured_feature");
    public static final LoadingStage PLACED_FEATURE = new LoadingStage("placed_feature");
}
