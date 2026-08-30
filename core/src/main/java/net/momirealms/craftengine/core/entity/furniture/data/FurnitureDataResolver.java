package net.momirealms.craftengine.core.entity.furniture.data;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FurnitureDataResolver<T> {

    @Nullable
    T resolve();
}
