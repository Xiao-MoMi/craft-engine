package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.util.Key;

public record CategorySourceType<T extends CategorySource>(Key id, CategorySourceFactory<T> factory) {
}
