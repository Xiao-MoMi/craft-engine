package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;

/** An element whose entity IDs can be reused by a compatible configuration. */
public interface TransformableFurnitureElement extends FurnitureElement {

    @NotNull WorldPosition position();

    int entityId();

    default void update(Player player, TransformableFurnitureElement previous) {
        update(player);
    }
}
