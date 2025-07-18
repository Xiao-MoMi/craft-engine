package net.momirealms.craftengine.core.registry;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public class ConstantBoundRegistry<T> extends AbstractMappedRegistry<T> {
    protected final Reference2IntMap<T> toId = MCUtils.make(new Reference2IntOpenHashMap<>(), map -> map.defaultReturnValue(-1));
    protected final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>(512);

    public ConstantBoundRegistry(ResourceKey<? extends Registry<T>> key) {
        super(key);
    }

    @Override
    public Holder.Reference<T> registerForHolder(ResourceKey<T> key) {
        throw new IllegalArgumentException("Cannot register a holder for a MappedRegistry");
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value) {
        Objects.requireNonNull(key);
        if (!key.registry().equals(super.key.location())) {
            throw new IllegalStateException(key + " is not allowed to be registered in " + this.key);
        }
        if (this.byResourceLocation.containsKey(key.location())) {
            throw new IllegalStateException("Adding duplicate key '" + key + "' to registry");
        } else {
            Holder.Reference<T> reference = this.byResourceKey.computeIfAbsent(key, k -> Holder.Reference.createConstant(this, k, value));
            this.byResourceKey.put(key, reference);
            this.byResourceLocation.put(key.location(), reference);
            int size = this.byId.size();
            this.byId.add(reference);
            this.toId.put(value, size);
            this.byValue.put(value, reference);
            return reference;
        }
    }

    @Override
    public int getId(T value) {
        return this.toId.getInt(value);
    }

    @Override
    public Key getKey(T value) {
        Holder.Reference<T> reference = this.byValue.get(value);
        if (reference == null) return null;
        return reference.key().location();
    }
}
