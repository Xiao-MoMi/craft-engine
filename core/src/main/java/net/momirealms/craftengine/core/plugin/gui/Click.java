package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;

public interface Click {
    Gui gui();

    Inventory clickedInventory();

    int slot();

    void cancel();

    boolean isCancelled();

    String type();

    int hotBarKey();

    Player clicker();

    void setItemOnCursor(Item<?> item);

    Item<?> itemOnCursor();
}
