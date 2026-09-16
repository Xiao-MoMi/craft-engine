package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;

public interface TransformableFurnitureElementConfig<E extends TransformableFurnitureElement> extends FurnitureElementConfig<E> {

    @NotNull WorldPosition getPos(@NotNull Furniture furniture);

    @NotNull E create(@NotNull Furniture furniture, @NotNull WorldPosition position);

    /** Creates the updated element using the previous element's entity IDs. */
    @NotNull E transform(@NotNull Furniture furniture, @NotNull E previous, @NotNull WorldPosition position);

    @Override
    default @NotNull E create(@NotNull Furniture furniture) {
        return create(furniture, getPos(furniture));
    }
}
