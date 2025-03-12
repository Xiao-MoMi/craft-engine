package net.momirealms.craftengine.core.util;

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

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Key key)) return false;
        return this.namespace.equals(key.namespace()) && this.value.equals(key.value());
    }

    @Override
    public String toString() {
        return namespace + ":" + value;
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