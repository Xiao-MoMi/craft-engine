package net.momirealms.craftengine.core.util;

public final class ObjectHolder<T> {
    private T value;

    public ObjectHolder(T value) {
        this.value = value;
    }

    public ObjectHolder() {
    }

    public T value() {
        return value;
    }

    public void bindValue(T value) {
        this.value = value;
    }
}
