package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public interface BlockEntityElement {

    default void show(@NotNull Player player) {}

    default void hide(@NotNull Player player) {}

    default void update(@NotNull Player player) {}

    default void deactivate() {}

    default void activate() {}
}
