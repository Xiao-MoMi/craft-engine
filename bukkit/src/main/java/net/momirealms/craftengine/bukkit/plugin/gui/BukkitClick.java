package net.momirealms.craftengine.bukkit.plugin.gui;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.gui.Click;
import net.momirealms.craftengine.core.plugin.gui.Gui;
import net.momirealms.craftengine.core.plugin.gui.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;

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
        return this.event.getAction().name();
    }

    @Override
    public int hotBarKey() {
        return this.event.getHotbarButton();
    }

    @Override
    public Player clicker() {
        return BukkitCraftEngine.instance().adapt((org.bukkit.entity.Player) event.getWhoClicked());
    }

    public InventoryClickEvent event() {
        return event;
    }
}
