package net.momirealms.craftengine.core.util.context;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

public class ContextKey<T> {
    private final Key id;

    public ContextKey(@NotNull Key id) {
        this.id = id;
    }

    @NotNull
    public Key id() {
        return id;
    }

    @NotNull
    public static <T> ContextKey<T> of(@NotNull Key id) {
        return new ContextKey<>(id);
    }

    @NotNull
    public static <T> ContextKey<T> of(@NotNull String id) {
        return new ContextKey<>(Key.of(id));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextKey<?> that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
