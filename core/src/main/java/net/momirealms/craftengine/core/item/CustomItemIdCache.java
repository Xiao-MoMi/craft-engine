package net.momirealms.craftengine.core.item;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.momirealms.craftengine.core.util.Key;

public final class CustomItemIdCache {
    private static final Cache<String, Key> IDS = Caffeine.newBuilder().maximumSize(8192).build();

    private CustomItemIdCache() {}

    // Cache only immutable strings, never the ID of a mutable item stack.
    public static Key parse(String id) {
        return IDS.get(id, Key::of);
    }
}
