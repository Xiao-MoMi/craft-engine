package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface Cullable {

    void show(Player player);

    void hide(Player player);

    default boolean cull(Player player) {
        this.hide(player);
        return false;
    }

    default void restore(Player player) {
        this.show(player);
    }

    @Nullable
    CullingData cullingData();
}
