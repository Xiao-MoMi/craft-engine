package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

public interface ItemInteraction {

    @NotNull
    Player player();

    @NotNull
    Window window();

    int windowSlot();
}
