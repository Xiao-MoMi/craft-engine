package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface CraftRemainderFactory<T extends CraftRemainder> {

    T create(ConfigSection section);
}
