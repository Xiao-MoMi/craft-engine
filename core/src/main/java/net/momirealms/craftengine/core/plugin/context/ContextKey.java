package net.momirealms.craftengine.core.plugin.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ContextKey<T> {

    static <T> ContextKey<T> direct(@NotNull String node) {
        return new Direct<>(node);
    }

    static <T> ContextKey<T> chain(@NotNull String node) {
        String[] parts = node.split("\\.");
        ContextKey<T> current = null;
        for (String part : parts) {
            current = new Chain<>(part, current);
        }
        return current;
    }

    @Nullable
    default <A> ContextKey<A> parent() {
        return null;
    }

    @NotNull
    String node();

    record Direct<T>(String node) implements ContextKey<T> {
        public Direct(@NotNull String node) {
            this.node = node;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ContextKey<?> another)) return false;
            return this.node.equals(another.node());
        }

        @Override
        public int hashCode() {
            return this.node.hashCode();
        }
    }

    class Chain<T> implements ContextKey<T> {
        private final String node;
        private final ContextKey<?> parent;

        protected Chain(@NotNull String node, @Nullable ContextKey<?> parent) {
            this.node = node;
            this.parent = parent;
        }

        @Override
        public @NotNull String node() {
            return this.node;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> ContextKey<A> parent() {
            return (ContextKey<A>) this.parent;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ContextKey<?> another)) return false;
            return this.node.equals(another.node());
        }

        @Override
        public int hashCode() {
            return this.node.hashCode();
        }
    }
}
