package net.momirealms.craftengine.bedrock;

public final class BedrockTintSource {
    // 非负数是 custom_model_data.colors 的下标，负数区分组件和无动态染色的场景。
    public static final int DYED_COLOR = -1;
    public static final int NONE = -2;
    public static final int FIREWORK = -3;
    public static final int POTION = -4;

    private BedrockTintSource() {
    }

    public static boolean isDynamic(int source) {
        return source >= DYED_COLOR || source == FIREWORK || source == POTION;
    }
}
