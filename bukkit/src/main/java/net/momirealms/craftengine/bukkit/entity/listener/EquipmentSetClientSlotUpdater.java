package net.momirealms.craftengine.bukkit.entity.listener;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundContainerSetSlotPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class EquipmentSetClientSlotUpdater {
    private EquipmentSetClientSlotUpdater() {
    }

    public static void updateAfterEquipmentChange(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            BukkitCraftEngine.instance().scheduler().platform().runDelayed(
                    () -> update(player),
                    null,
                    player
            );
        } else {
            update(player);
        }
    }

    static void update(Player player) {
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        Object minecraftPlayer = serverPlayer.minecraftPlayer();
        if (minecraftPlayer == null) return;

        Object inventoryMenu = PlayerProxy.INSTANCE.getInventoryMenu(minecraftPlayer);
        int containerId = AbstractContainerMenuProxy.INSTANCE.getContainerId(inventoryMenu);
        PlayerInventory inventory = player.getInventory();
        int size = Math.min(inventory.getSize(), PlayerInventorySlotMapper.LAST_SUPPORTED_SLOT + 1);
        for (int inventorySlot = 0; inventorySlot < size; inventorySlot++) {
            ItemStack stack = inventory.getItem(inventorySlot);
            if (!requiresClientRefresh(stack)) continue;

            int stateId = AbstractContainerMenuProxy.INSTANCE.incrementStateId(inventoryMenu);
            Object packet = ClientboundContainerSetSlotPacketProxy.INSTANCE.newInstance(
                    containerId,
                    stateId,
                    PlayerInventorySlotMapper.toMenuSlot(inventorySlot),
                    CraftItemStackProxy.INSTANCE.asNMSCopy(stack)
            );
            serverPlayer.sendPacket(packet, false);
        }
    }

    private static boolean requiresClientRefresh(ItemStack stack) {
        if (ItemStackUtils.isEmpty(stack)) return false;
        BukkitItem item = ItemStackUtils.wrap(stack);
        return item.getDefinition()
                .filter(ItemDefinition::hasClientBoundProcessor)
                .map(definition -> definition.settings().equipmentSetPart() != null)
                .orElse(false);
    }
}
