package net.momirealms.craftengine.core.registry;

import com.google.common.collect.Maps;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MappedRegistry<T> implements WritableRegistry<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final Map<Key, Holder.Reference<T>> byResourceLocation = new HashMap<>(512);
    private final Map<ResourceKey<T>, Holder.Reference<T>> byResourceKey = new HashMap<>(512);
    private final List<Holder.Reference<T>> byId = new ArrayList<>(512);

    public MappedRegistry(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Override
    public Holder.Reference<T> registerForHolder(ResourceKey<T> key) {
        Objects.requireNonNull(key);
        if (!key.registry().equals(this.key.location())) {
            throw new IllegalStateException(key + " is not allowed to be registered in " + this.key);
        }
        if (this.byResourceLocation.containsKey(key.location())) {
            throw new IllegalStateException("Adding duplicate key '" + key + "' to registry");
        } else {
            Holder.Reference<T> reference = this.byResourceKey.computeIfAbsent(key, k -> Holder.Reference.create(this, k));
            this.byResourceKey.put(key, reference);
            this.byResourceLocation.put(key.location(), reference);
            this.byId.add(reference);
            return reference;
        }
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value) {
        Holder.Reference<T> holder = registerForHolder(key);
        holder.bindValue(value);
        return holder;
    }

    @Nullable
    @Override
    public T getValue(@Nullable ResourceKey<T> key) {
        return getValueFromNullable(this.byResourceKey.get(key));
    }

    @Override
    public Optional<Holder.Reference<T>> get(Key id) {
        return Optional.ofNullable(this.byResourceLocation.get(id));
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
        return Optional.ofNullable(this.byResourceKey.get(key));
    }

    @Nullable
    @Override
    public T getValue(@Nullable Key id) {
        Holder.Reference<T> reference = this.byResourceLocation.get(id);
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
        return Collections.unmodifiableSet(this.byResourceLocation.keySet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return Collections.unmodifiableSet(this.byResourceKey.keySet());
    }

    @Override
    public boolean containsKey(Key id) {
        return this.byResourceLocation.containsKey(id);
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
