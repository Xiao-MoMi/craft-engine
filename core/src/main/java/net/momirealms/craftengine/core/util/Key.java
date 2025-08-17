package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

public record Key(String namespace, String value) {
    public static final String DEFAULT_NAMESPACE = "craftengine";

    public static Key withDefaultNamespace(String value) {
        return new Key(DEFAULT_NAMESPACE, value);
    }

    public static Key of(String namespace, String value) {
        return new Key(namespace, value);
    }

    public static Key withDefaultNamespace(String namespacedId, String defaultNamespace) {
        return of(decompose(namespacedId, defaultNamespace));
    }

    public static Key of(String[] id) {
        return new Key(id[0], id[1]);
    }

    public static Key of(String namespacedId) {
        return of(decompose(namespacedId, "minecraft"));
    }

    public static Key from(String namespacedId) {
        return of(decompose(namespacedId, "minecraft"));
    }

    public static Key fromNamespaceAndPath(String namespace, String path) {
        return Key.of(namespace, path);
    }

    public String[] decompose() {
        return new String[] { this.namespace, this.value };
    }

    @Override
    public int hashCode() {
        int result = this.namespace.hashCode();
        result = 31 * result + this.value.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Key key)) return false;
        // 先比value命中率高
        return this.value.equals(key.value()) && this.namespace.equals(key.namespace());
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }

    public String asString() {
        return this.namespace + ":" + this.value;
    }

    private static String[] decompose(String id, String namespace) {
        String[] strings = new String[]{namespace, id};
        int i = id.indexOf(':');
        if (i >= 0) {
            strings[1] = id.substring(i + 1);
            if (i >= 1) {
                strings[0] = id.substring(0, i);
            }
        }
        return strings;
    }
}