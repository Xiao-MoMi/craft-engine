package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.util.Key;

public record FurnitureTintSourceType<T extends FurnitureTintSource>(Key id, FurnitureTintSourceConfigFactory<T> factory) {
}
