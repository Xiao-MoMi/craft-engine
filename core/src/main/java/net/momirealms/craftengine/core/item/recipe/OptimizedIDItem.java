package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

public class OptimizedIDItem<T> {
    private final T rawItem;
    private final Holder<Key> idHolder;

    public OptimizedIDItem(Holder<Key> idHolder, T rawItem) {
        this.idHolder = idHolder;
        this.rawItem = rawItem;
    }

    public Holder<Key> id() {
        return idHolder;
    }

    public T rawItem() {
        return rawItem;
    }

    public boolean is(Holder<Key> id) {
        return idHolder == id;
    }

    public boolean isEmpty() {
        return idHolder == null;
    }

    @Override
    public String toString() {
        return "OptimizedIDItem{" +
                "idHolder=" + idHolder +
                '}';
    }
}
