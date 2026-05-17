package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public interface ItemUpdaterFactory<T extends ItemUpdater> {

    T create(Key item, ConfigSection section);
}
