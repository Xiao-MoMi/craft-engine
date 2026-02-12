package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.momirealms.craftengine.core.plugin.context.Context;
import org.jetbrains.annotations.NotNull;

public final class ViewerNamedArgumentTag extends NamedArgumentTag {

    public ViewerNamedArgumentTag(@NotNull Context context) {
        super(context);
    }

    @Override
    public boolean has(@NotNull String name) {
        return name.equals("viewer_arg");
    }
}