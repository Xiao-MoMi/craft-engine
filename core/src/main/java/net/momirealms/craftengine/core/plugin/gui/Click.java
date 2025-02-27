package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.entity.player.Player;

public interface Click {
    Gui gui();

    Inventory clickedInventory();

    int slot();

    void cancel();

    boolean isCancelled();

    String type();

    int hotBarKey();

    Player clicker();
}
