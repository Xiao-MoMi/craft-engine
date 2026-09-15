package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;

public interface TextProvider {

    String get(Context context);

    Key type();

    default boolean isConstant() {
        return false;
    }
}
