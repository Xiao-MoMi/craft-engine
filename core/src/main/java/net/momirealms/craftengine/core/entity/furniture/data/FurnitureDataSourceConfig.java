package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface FurnitureDataSourceConfig<T> {

    @NotNull
    FurnitureDataResolver<T> bind(@NotNull Furniture furniture);
}
