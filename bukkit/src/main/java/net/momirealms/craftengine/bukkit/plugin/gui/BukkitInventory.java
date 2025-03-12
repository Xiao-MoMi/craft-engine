package net.momirealms.craftengine.bukkit.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.gui.Inventory;
import org.bukkit.inventory.ItemStack;

public class BukkitInventory implements Inventory {
    private final org.bukkit.inventory.Inventory inventory;

    public BukkitInventory(org.bukkit.inventory.Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void open(Player player, Component title) {
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) player;
        Object nmsPlayer = serverPlayer.serverPlayer();
        try {
            Object menuType = Reflections.method$CraftContainer$getNotchInventoryType.invoke(null, inventory);
            int nextId = (int) Reflections.method$ServerPlayer$nextContainerCounter.invoke(nmsPlayer);
            Object menu = Reflections.constructor$CraftContainer.newInstance(inventory, nmsPlayer, nextId);
            Reflections.field$AbstractContainerMenu$checkReachable.set(menu, false);
            Object packet = Reflections.constructor$ClientboundOpenScreenPacket.newInstance(nextId, menuType, ComponentUtils.adventureToMinecraft(title));
            serverPlayer.sendPacket(packet, false);
            Reflections.field$Player$containerMenu.set(nmsPlayer, menu);
            Reflections.method$ServerPlayer$initMenu.invoke(nmsPlayer, menu);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to update inventory title", e);
        }
    }

    @Override
    public void setItem(int index, Item<?> item) {
        this.inventory.setItem(index, item == null ? null : (ItemStack) item.getItem());
    }
}
