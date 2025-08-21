package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;

public record ItemUpdateResult(Item<?> finalItem, boolean replaced, boolean updated) {
}
