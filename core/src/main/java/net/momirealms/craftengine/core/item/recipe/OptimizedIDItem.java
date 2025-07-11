package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.UniqueKey;

public class OptimizedIDItem<T> {
    private final T rawItem;
    private final UniqueKey uniqueId;

    public OptimizedIDItem(UniqueKey uniqueId, T rawItem) {
        this.uniqueId = uniqueId;
        this.rawItem = rawItem;
    }

    public UniqueKey id() {
        return uniqueId;
    }

    public T rawItem() {
        return rawItem;
    }

    public boolean is(UniqueKey id) {
        return uniqueId == id;
    }

    public boolean isEmpty() {
        return uniqueId == null;
    }

    @Override
    public String toString() {
        return "OptimizedIDItem{" +
                "uniqueId=" + uniqueId +
                '}';
    }
}
