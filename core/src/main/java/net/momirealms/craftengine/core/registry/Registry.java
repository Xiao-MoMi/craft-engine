package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Registry<T> extends Holder.Owner<T> {

    ResourceKey<? extends Registry<T>> key();

    @Nullable
    T getValue(@Nullable ResourceKey<T> key);

    @Nullable
    T getValue(@Nullable Key id);

    @Nullable
    T getValue(int id);

    Set<Key> keySet();

    Set<Map.Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    boolean containsKey(Key id);

    boolean containsKey(ResourceKey<T> key);

    Optional<Holder.Reference<T>> get(Key id);

    Optional<Holder.Reference<T>> get(ResourceKey<T> key);
}
