package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class UniqueKey {
    private static final Map<Key, UniqueKey> CACHE = new HashMap<>(4096, 0.5f);

    private final Key key;

    private UniqueKey(Key key) {
        this.key = key;
    }

    public static UniqueKey create(Key key) {
        return CACHE.computeIfAbsent(key, UniqueKey::new);
    }

    @Nullable
    public static UniqueKey getCached(Key key) {
        return CACHE.get(key);
    }

    public Key key() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key.toString();
    }
}
