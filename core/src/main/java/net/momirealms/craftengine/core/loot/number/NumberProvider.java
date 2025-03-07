package net.momirealms.craftengine.core.loot.number;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

public interface NumberProvider {

    float getFloat(LootContext context);

    default int getInt(LootContext context) {
        return Math.round(this.getFloat(context));
    }

    Key type();
}
