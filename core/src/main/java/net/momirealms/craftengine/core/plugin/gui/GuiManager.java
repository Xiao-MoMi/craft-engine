package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.plugin.Reloadable;

public interface GuiManager extends Reloadable {

    Inventory createInventory(Gui gui, int size);

    void delayedInit();
}
