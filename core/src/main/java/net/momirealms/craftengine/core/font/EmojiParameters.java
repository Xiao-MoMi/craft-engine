package net.momirealms.craftengine.core.font;

import net.momirealms.craftengine.core.plugin.context.ContextKey;

public final class EmojiParameters {
    private EmojiParameters() {}

    public static final ContextKey<String> KEYWORD = ContextKey.direct("keyword");
    public static final ContextKey<String> EMOJI = ContextKey.direct("emoji");
}
