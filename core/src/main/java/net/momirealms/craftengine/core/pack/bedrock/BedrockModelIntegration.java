package net.momirealms.craftengine.core.pack.bedrock;

import java.util.Locale;
import java.util.Map;

public enum BedrockModelIntegration {
    DISABLED,
    DISPLAY_ENTITY,
    NATIVE_ENTITY;

    private static final Map<String, BedrockModelIntegration> BY_ID = Map.of(
            "disabled", DISABLED,
            "display_entity", DISPLAY_ENTITY,
            "native_entity", NATIVE_ENTITY
    );

    public static BedrockModelIntegration byId(String id) {
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }
}
