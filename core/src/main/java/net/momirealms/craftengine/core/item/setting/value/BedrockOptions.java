package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public record BedrockOptions(String renderMethod) {
    public static final BedrockOptions DEFAULT = new BedrockOptions(BedrockPackMappings.DEFAULT_RENDER_METHOD);
    private static final String[] RENDER_METHOD = ConfigKeys.of("render_method");

    public static BedrockOptions fromConfig(ConfigSection section) {
        return new BedrockOptions(section.getString(RENDER_METHOD, DEFAULT.renderMethod()));
    }
}
