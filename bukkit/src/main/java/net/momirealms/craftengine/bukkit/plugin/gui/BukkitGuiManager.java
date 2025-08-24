package net.momirealms.craftengine.bukkit.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.LegacyInventoryUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.gui.*;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;

public class BukkitGuiManager implements GuiManager, Listener {
    private static final boolean useNewOpenInventory = ReflectionUtils.getDeclaredMethod(InventoryView.class, void.class, new String[]{"open"}) != null;
    private final BukkitCraftEngine plugin;

    public BukkitGuiManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void openInventory(net.momirealms.craftengine.core.entity.player.Player player, GuiType guiType) {
        Player bukkitPlayer = (Player) player.platformPlayer();
        if (useNewOpenInventory) {
            switch (guiType) {
                case ANVIL -> MenuType.ANVIL.create(bukkitPlayer).open();
                case LOOM -> MenuType.LOOM.create(bukkitPlayer).open();
                case ENCHANTMENT -> MenuType.ENCHANTMENT.create(bukkitPlayer).open();
                case CRAFTING -> MenuType.CRAFTER_3X3.create(bukkitPlayer).open();
                case CARTOGRAPHY -> MenuType.CARTOGRAPHY_TABLE.create(bukkitPlayer).open();
                case SMITHING -> MenuType.SMITHING.create(bukkitPlayer).open();
                case GRINDSTONE -> MenuType.GRINDSTONE.create(bukkitPlayer).open();
            }
        } else {
            switch (guiType) {
                case ANVIL -> LegacyInventoryUtils.openAnvil(bukkitPlayer);
                case LOOM -> LegacyInventoryUtils.openLoom(bukkitPlayer);
                case GRINDSTONE -> LegacyInventoryUtils.openGrindstone(bukkitPlayer);
                case SMITHING -> LegacyInventoryUtils.openSmithingTable(bukkitPlayer);
                case CRAFTING -> LegacyInventoryUtils.openWorkbench(bukkitPlayer);
                case ENCHANTMENT -> LegacyInventoryUtils.openEnchanting(bukkitPlayer);
                case CARTOGRAPHY -> LegacyInventoryUtils.openCartographyTable(bukkitPlayer);
            }
        }
    }

    @Override
    public void updateInventoryTitle(net.momirealms.craftengine.core.entity.player.Player player, Component component) {
        Object nmsPlayer = player.serverPlayer();
        try {
            Object containerMenu = FastNMS.INSTANCE.field$Player$containerMenu(nmsPlayer);
            int containerId = CoreReflections.field$AbstractContainerMenu$containerId.getInt(containerMenu);
            Object menuType = CoreReflections.field$AbstractContainerMenu$menuType.get(containerMenu);
            Object packet = NetworkReflections.constructor$ClientboundOpenScreenPacket.newInstance(containerId, menuType, ComponentUtils.adventureToMinecraft(component));
            player.sendPacket(packet, false);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to update inventory title", e);
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
        if (!CraftBukkitReflections.clazz$MinecraftInventory.isInstance(FastNMS.INSTANCE.method$CraftInventory$getInventory(inventory))) {
            return;
        }
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
        if (!CraftBukkitReflections.clazz$MinecraftInventory.isInstance(FastNMS.INSTANCE.method$CraftInventory$getInventory(inventory))) {
            return;
        }
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
