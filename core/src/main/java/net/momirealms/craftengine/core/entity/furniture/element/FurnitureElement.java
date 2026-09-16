package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.entity.player.Player;

import java.util.function.IntConsumer;

public interface FurnitureElement {

    void gatherInteractableEntityId(IntConsumer collector);

    void show(Player player);

    void hide(Player player);

    void update(Player player);

    default boolean canSee(Player player) {
        return true;
    }

    default void deactivate() {}

    default void activate() {}
}
