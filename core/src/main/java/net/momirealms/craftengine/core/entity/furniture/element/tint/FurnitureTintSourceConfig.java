package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.entity.furniture.Furniture;

public interface FurnitureTintSourceConfig<T extends FurnitureTintSource> {

    T create(Furniture furniture);
}
