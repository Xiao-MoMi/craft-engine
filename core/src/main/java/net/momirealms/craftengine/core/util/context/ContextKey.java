package net.momirealms.craftengine.core.util.context;

import net.momirealms.craftengine.core.util.Key;

public class ContextKey<T> {
    private final Key id;

    public ContextKey(Key id) {
        this.id = id;
    }

    public Key id() {
        return id;
    }

    public static <T> ContextKey<T> of(Key id) {
        return new ContextKey<>(id);
    }
}
