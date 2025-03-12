package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.util.ResourceKey;

public interface WritableRegistry<T> extends Registry<T> {

    Holder.Reference<T> registerForHolder(ResourceKey<T> key);

    Holder.Reference<T> register(ResourceKey<T> key, T value);

    boolean isEmpty();
}
