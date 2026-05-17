package net.momirealms.craftengine.core.block.property;

import net.momirealms.craftengine.core.util.Key;

public record PropertyType<T extends Comparable<T>>(Key id, PropertyFactory<T> factory) {
}
