package net.momirealms.craftengine.core.entity.furniture.element;

import net.momirealms.craftengine.core.entity.player.Player;

import java.util.function.Consumer;

public interface FurnitureElement {

    void gatherInteractableEntityId(Consumer<Integer> collector);

    void show(Player player);

    void hide(Player player);

    void refresh(Player player);

    default void deactivate() {}

    default void activate() {}
}
