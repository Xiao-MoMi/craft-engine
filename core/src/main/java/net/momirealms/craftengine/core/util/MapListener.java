package net.momirealms.craftengine.core.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class MapListener<K, V> implements Map<K, V> {
    private final Map<K, V> delegateMap;
    private final Consumer<V> putListener;

    public MapListener(Map<K, V> original, Consumer<V> putListener) {
        this.delegateMap = original;
        this.putListener = putListener;
    }

    @Override
    public int size() {
        return this.delegateMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegateMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.delegateMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegateMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.delegateMap.get(key);
    }

    @Override
    public @Nullable V put(K key, V value) {
        putListener.accept(value);
        return this.delegateMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return this.delegateMap.remove(key);
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            putListener.accept(entry.getValue());
        }
        this.delegateMap.putAll(map);
    }

    @Override
    public void clear() {
        this.delegateMap.clear();
    }

    @Override
    public @NonNull Set<K> keySet() {
        return this.delegateMap.keySet();
    }

    @Override
    public @NonNull Collection<V> values() {
        return this.delegateMap.values();
    }

    @Override
    public @NonNull Set<Entry<K, V>> entrySet() {
        return this.delegateMap.entrySet();
    }
}
