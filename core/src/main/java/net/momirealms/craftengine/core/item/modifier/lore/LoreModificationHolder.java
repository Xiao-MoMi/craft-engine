package net.momirealms.craftengine.core.item.modifier.lore;

import org.jetbrains.annotations.NotNull;

public record LoreModificationHolder(LoreModification modification, int priority) implements Comparable<LoreModificationHolder> {

    @Override
    public int compareTo(@NotNull LoreModificationHolder o) {
        return Integer.compare(priority, o.priority);
    }
}
