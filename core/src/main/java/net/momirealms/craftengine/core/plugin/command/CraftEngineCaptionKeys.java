package net.momirealms.craftengine.core.plugin.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;

public class CraftEngineCaptionKeys {
    public static final Caption ARGUMENT_PARSE_FAILURE_BLOCK_STATE = of("argument.parse.failure.block_state");

    private CraftEngineCaptionKeys() {
    }

    private static @NonNull Caption of(@NonNull String key) {
        return Caption.of(key);
    }
}
