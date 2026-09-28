package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.item.ItemKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UniqueKey {
    private static final Map<Key, UniqueKey> CACHE = new ConcurrentHashMap<>(4096, 0.5f);
    public static final UniqueKey AIR = UniqueKey.create(ItemKeys.AIR);

    private final Key key;

    private UniqueKey(Key key) {
        this.key = key;
    }

    public static UniqueKey create(Key key) {
        UniqueKey uniqueKey = key.uniqueKey;
        if (uniqueKey == null) {
            uniqueKey = CACHE.computeIfAbsent(key, UniqueKey::new);
            // Races only repeat the lookup: CACHE always returns the same immutable instance.
            key.uniqueKey = uniqueKey;
        }
        return uniqueKey;
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
