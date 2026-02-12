package net.momirealms.craftengine.bukkit.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
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
            Object menuType = CraftBukkitReflections.method$CraftContainer$getNotchInventoryType.invoke(null, this.inventory);
            int nextId = (int) CoreReflections.method$ServerPlayer$nextContainerCounter.invoke(nmsPlayer);
            Object menu = CraftBukkitReflections.constructor$CraftContainer.newInstance(this.inventory, nmsPlayer, nextId);
            CoreReflections.field$AbstractContainerMenu$checkReachable.set(menu, false);
            CraftBukkitReflections.method$CraftEventFactory$callInventoryOpenEvent.invoke(null, nmsPlayer, menu);
            Object packet = NetworkReflections.constructor$ClientboundOpenScreenPacket.newInstance(nextId, menuType, ComponentUtils.adventureToMinecraft(title));
            serverPlayer.sendPacket(packet, false);
            CoreReflections.field$Player$containerMenu.set(nmsPlayer, menu);
            CoreReflections.method$ServerPlayer$initMenu.invoke(nmsPlayer, menu);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to create bukkit inventory", e);
        }
    }

    @Override
    public void setItem(int index, Item<?> item) {
        this.inventory.setItem(index, item == null ? null : (ItemStack) item.getItem());
    }
}
