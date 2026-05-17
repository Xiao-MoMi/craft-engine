package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.util.Key;

public record ItemProcessorType<T extends ItemProcessor>(Key id, ItemProcessorFactory<T> factory) {
}
