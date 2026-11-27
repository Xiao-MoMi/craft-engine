package net.momirealms.craftengine.core.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

final class KeyUtils {
    static final Cache<String, Key> CACHE = Caffeine.newBuilder().maximumSize(8192).build();
}
