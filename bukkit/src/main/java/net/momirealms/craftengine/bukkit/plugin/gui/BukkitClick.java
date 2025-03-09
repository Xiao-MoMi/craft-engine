package net.momirealms.craftengine.bukkit.plugin.gui;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.gui.Click;
import net.momirealms.craftengine.core.plugin.gui.Gui;
import net.momirealms.craftengine.core.plugin.gui.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BukkitClick implements Click {
    private final InventoryClickEvent event;
    private final Inventory inventory;
    private final Gui gui;

    public BukkitClick(InventoryClickEvent event, Gui gui, Inventory inventory) {
        this.event = event;
        this.inventory = inventory;
        this.gui = gui;
    }

    @Override
    public Gui gui() {
        return this.gui;
    }

    @Override
    public Inventory clickedInventory() {
        return this.inventory;
    }

    @Override
    public int slot() {
        return this.event.getSlot();
    }

    @Override
    public void cancel() {
        this.event.setCancelled(true);
    }

    @Override
    public boolean isCancelled() {
        return this.event.isCancelled();
    }

    @Override
    public String type() {
        return this.event.getClick().name();
    }

    @Override
    public int hotBarKey() {
        return this.event.getHotbarButton();
    }

    @Override
    public Player clicker() {
        return BukkitCraftEngine.instance().adapt((org.bukkit.entity.Player) event.getWhoClicked());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setItemOnCursor(Item<?> item) {
        this.event.setCursor((ItemStack) item.getItem());
    }

    @Override
    public Item<?> itemOnCursor() {
        ItemStack itemStack = this.event.getCursor();
        if (ItemUtils.isEmpty(itemStack)) return null;
        return BukkitItemManager.instance().wrap(itemStack);
    }

    public InventoryClickEvent event() {
        return event;
    }
}
