package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ItemPatch {

    void applyTo(@NotNull Item item);
}
