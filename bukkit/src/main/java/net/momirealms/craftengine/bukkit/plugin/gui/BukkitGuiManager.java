package net.momirealms.craftengine.bukkit.plugin.gui;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.LegacyInventoryUtils;
import net.momirealms.craftengine.core.plugin.gui.AbstractGui;
import net.momirealms.craftengine.core.plugin.gui.Gui;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.Inventory;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class BukkitGuiManager implements GuiManager, Listener {
    private final BukkitCraftEngine plugin;
    private SchedulerTask timerTask;

    public BukkitGuiManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
        if (this.timerTask != null && !this.timerTask.cancelled()) {
            this.timerTask.cancel();
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin.bootstrap());
        this.timerTask = plugin.scheduler().sync().runRepeating(this::timerTask, 20, 20);
    }

    public void timerTask() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            org.bukkit.inventory.Inventory top = !VersionHelper.isVersionNewerThan1_21() ? LegacyInventoryUtils.getTopInventory(player) : player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof CraftEngineInventoryHolder holder) {
                holder.gui().onTimer();
            }
        }
    }

    @Override
    public Inventory createInventory(Gui gui, int size) {
        CraftEngineInventoryHolder holder = new CraftEngineInventoryHolder(gui);
        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(holder, size);
        holder.holder().bindValue(inventory);
        return new BukkitInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        org.bukkit.inventory.Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftEngineInventoryHolder craftEngineInventoryHolder)) {
            return;
        }
        AbstractGui gui = (AbstractGui) craftEngineInventoryHolder.gui();
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == player.getInventory()) {
            gui.handleInventoryClick(new BukkitClick(event, gui, new BukkitInventory(player.getInventory())));
        } else if (event.getClickedInventory() == inventory) {
            gui.handleGuiClick(new BukkitClick(event, gui, new BukkitInventory(inventory)));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent event) {
        org.bukkit.inventory.Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftEngineInventoryHolder)) {
            return;
        }
        for (int raw : event.getRawSlots()) {
            if (raw < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
