package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface Cullable {

    void show(Player player);

    void hide(Player player);

    @Nullable
    CullingData cullingData();
}
