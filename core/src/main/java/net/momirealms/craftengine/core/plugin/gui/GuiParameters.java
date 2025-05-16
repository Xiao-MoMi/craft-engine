package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.plugin.context.ContextKey;

public final class GuiParameters {
    private GuiParameters() {}

    public static final ContextKey<String> MAX_PAGE = ContextKey.direct("max_page");
    public static final ContextKey<String> CURRENT_PAGE = ContextKey.direct("current_page");
    public static final ContextKey<String> COOKING_TIME = ContextKey.direct("cooking_time");
    public static final ContextKey<String> COOKING_EXPERIENCE = ContextKey.direct("cooking_experience");
}
