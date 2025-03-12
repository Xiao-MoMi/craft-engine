package net.momirealms.craftengine.core.loot.entry;

import java.util.Map;

public interface LootEntryContainerFactory<T> {

    LootEntryContainer<T> create(Map<String, Object> arguments);
}
