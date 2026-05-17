package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class Key {
    public static final String CRAFTENGINE_NAMESPACE = "craftengine";
    public static final String MINECRAFT_NAMESPACE = "minecraft";
    public final String namespace;
    public final String value;

    public Key(String namespace, String value) {
        this.namespace = namespace;
        this.value = value;
    }

    @NotNull
    public String namespace() {
        return this.namespace;
    }

    @NotNull
    public String value() {
        return this.value;
    }

    public static Key withCraftEngineNamespace(String value) {
        return new Key(CRAFTENGINE_NAMESPACE, value);
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
        return of(decompose(namespacedId, MINECRAFT_NAMESPACE));
    }

    public static Key minecraft(String namespacedId) {
        return of(decompose(namespacedId, MINECRAFT_NAMESPACE));
    }

    public static Key ce(String namespacedId) {
        return of(decompose(namespacedId, CRAFTENGINE_NAMESPACE));
    }

    public static Key from(String namespacedId) {
        return of(decompose(namespacedId, MINECRAFT_NAMESPACE));
    }

    public static Key fromNamespaceAndPath(String namespace, String path) {
        return Key.of(namespace, path);
    }

    public String[] decompose() {
        return new String[] { this.namespace, this.value };
    }

    public Key transform(UnaryOperator<String> transformer) {
        return new Key(transformer.apply(this.namespace), transformer.apply(this.value));
    }

    public boolean contains(String key) {
        return this.value.contains(key) || this.namespace.contains(key);
    }

    @Override
    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.value.hashCode();
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
        return this.value.equals(key.value) && this.namespace.equals(key.namespace);
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }

    public String asString() {
        return this.namespace + ":" + this.value;
    }

    public String asMinimalString() {
        if (this.namespace.equals(MINECRAFT_NAMESPACE)) {
            return this.value;
        }
        return asString();
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