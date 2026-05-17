package net.momirealms.craftengine.core.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.trade.MerchantOffer;
import net.momirealms.craftengine.core.plugin.Manageable;

import java.util.List;

public interface GuiManager extends Manageable {

    void openInventory(Player player, GuiType guiType);

    void updateInventoryTitle(Player player, Component component);

    Inventory createInventory(Gui gui, int size);

    void openMerchant(Player player, Component title, List<MerchantOffer> offers);
}
