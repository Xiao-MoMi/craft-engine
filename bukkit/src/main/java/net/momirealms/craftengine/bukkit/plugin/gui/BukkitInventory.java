package net.momirealms.craftengine.bukkit.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.gui.Inventory;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundOpenScreenPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
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
        Object menuType = CraftContainerProxy.INSTANCE.getNotchInventoryType(this.inventory);
        int nextId = ServerPlayerProxy.INSTANCE.nextContainerCounter(nmsPlayer);
        Object menu = CraftContainerProxy.INSTANCE.newInstance(this.inventory, nmsPlayer, nextId);
        AbstractContainerMenuProxy.INSTANCE.setCheckReachable(menu, false);
        CraftEventFactoryProxy.INSTANCE.callInventoryOpenEvent(nmsPlayer, menu);
        Object packet = ClientboundOpenScreenPacketProxy.INSTANCE.newInstance(nextId, menuType, ComponentUtils.adventureToMinecraft(title));
        serverPlayer.sendPacket(packet, false);
        PlayerProxy.INSTANCE.setContainerMenu(nmsPlayer, menu);
        ServerPlayerProxy.INSTANCE.initMenu(nmsPlayer, menu);
    }

    @Override
    public void setItem(int index, Item<?> item) {
        this.inventory.setItem(index, item == null ? null : (ItemStack) item.getItem());
    }
}
