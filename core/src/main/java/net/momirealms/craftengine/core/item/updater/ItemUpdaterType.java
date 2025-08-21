package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public interface ItemUpdaterType<I> {

    ItemUpdater<I> create(Key item, Map<String, Object> args);
}
