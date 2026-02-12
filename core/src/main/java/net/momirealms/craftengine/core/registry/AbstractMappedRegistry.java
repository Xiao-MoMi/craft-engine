package net.momirealms.craftengine.core.registry;

import com.google.common.collect.Maps;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractMappedRegistry<T> implements WritableRegistry<T> {
    protected final ResourceKey<? extends Registry<T>> key;
    protected final Map<Key, Holder.Reference<T>> byIdentifier;
    protected final Map<ResourceKey<T>, Holder.Reference<T>> byResourceKey;
    protected final List<Holder.Reference<T>> byId;

    protected AbstractMappedRegistry(ResourceKey<? extends Registry<T>> key, int expectedSize) {
        this.key = key;
        this.byIdentifier = new HashMap<>(expectedSize);
        this.byResourceKey = new HashMap<>(expectedSize);
        this.byId = new ArrayList<>(expectedSize);
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Nullable
    @Override
    public T getValue(@Nullable ResourceKey<T> key) {
        return getValueFromNullable(this.byResourceKey.get(key));
    }

    @Override
    public Optional<Holder.Reference<T>> get(Key id) {
        return Optional.ofNullable(this.byIdentifier.get(id));
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
        return Optional.ofNullable(this.byResourceKey.get(key));
    }

    @Nullable
    @Override
    public T getValue(@Nullable Key id) {
        Holder.Reference<T> reference = this.byIdentifier.get(id);
        return getValueFromNullable(reference);
    }

    @Override
    public @Nullable T getValue(int id) {
        if (id < 0 || id >= this.byId.size()) return null;
        return getValueFromNullable(this.byId.get(id));
    }

    @Nullable
    private static <T> T getValueFromNullable(@Nullable Holder.Reference<T> entry) {
        return entry != null ? entry.value() : null;
    }

    @Override
    public Set<Key> keySet() {
        return Collections.unmodifiableSet(this.byIdentifier.keySet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return Collections.unmodifiableSet(this.byResourceKey.keySet());
    }

    @Override
    public boolean containsKey(Key id) {
        return this.byIdentifier.containsKey(id);
    }

    @Override
    public boolean containsKey(ResourceKey<T> key) {
        return this.byResourceKey.containsKey(key);
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.unmodifiableSet(Maps.transformValues(this.byResourceKey, Holder::value).entrySet());
    }

    @Override
    public boolean isEmpty() {
        return this.byResourceKey.isEmpty();
    }
}
