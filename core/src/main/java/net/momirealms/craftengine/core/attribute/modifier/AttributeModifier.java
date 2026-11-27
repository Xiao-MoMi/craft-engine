package net.momirealms.craftengine.core.attribute.modifier;

import net.momirealms.craftengine.core.attribute.modifier.removal.RemovalCondition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public interface AttributeModifier {

    Key id();

    double amount(Context context);

    Key operation();

    boolean test(Context context);

    @Nullable
    default RemovalCondition removalCondition() {
        return null;
    }

    default int updateInterval() {
        return 0;
    }

    default boolean isDynamic() {
        return true;
    }
}
