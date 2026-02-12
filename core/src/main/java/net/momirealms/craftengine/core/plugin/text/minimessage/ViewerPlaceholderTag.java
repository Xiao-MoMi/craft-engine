package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.momirealms.craftengine.core.plugin.context.Context;
import org.jetbrains.annotations.NotNull;

public final class ViewerPlaceholderTag extends PlaceholderTag {

    public ViewerPlaceholderTag(@NotNull Context player) {
        super(player);
    }

    @Override
    public boolean has(@NotNull String name) {
        return "viewer_papi".equals(name);
    }
}
