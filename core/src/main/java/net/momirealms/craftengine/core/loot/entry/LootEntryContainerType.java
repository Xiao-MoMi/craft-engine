package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.util.Key;

public record LootEntryContainerType<T extends LootEntryContainer>(Key id, LootEntryContainerFactory<T> factory) {
}
