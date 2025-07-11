package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DynamicBoundRegistry<T> extends AbstractMappedRegistry<T> {

    public DynamicBoundRegistry(ResourceKey<? extends Registry<T>> key) {
        super(key);
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

    @Override
    public int getId(@Nullable T value) {
        throw new UnsupportedOperationException("getId is not supported for dynamic bound registry");
    }

    @Override
    public Key getKey(T value) {
        throw new UnsupportedOperationException("getKey is not supported for dynamic bound registry");
    }
}
