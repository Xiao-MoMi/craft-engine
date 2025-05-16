package net.momirealms.craftengine.bukkit.item.listener;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ArmorEventListener implements Listener {

    // 只有在没有equippable组件的版本才生效，阻止自定义物品放到马上
    // 低版本没有自定义盔甲，所以完全不需要考虑能放置上去的情况
    @EventHandler(ignoreCancelled = true)
    public void onInteractHorse(PlayerInteractEntityEvent event) {
        if (VersionHelper.isOrAbove1_21_2()) return;
        if (event.getRightClicked() instanceof Horse horse) {
            ItemStack itemInHand = event.getPlayer().getInventory().getItem(event.getHand());
            if (horse.getInventory().getArmor() == null) {
                switch (itemInHand.getType()) {
                    case LEATHER_HORSE_ARMOR, IRON_HORSE_ARMOR, GOLDEN_HORSE_ARMOR, DIAMOND_HORSE_ARMOR -> {
                        if (CraftEngineItems.isCustomItem(itemInHand)) {
                            event.setCancelled(true);
                        }
                    }
                }
            } else if (horse.getInventory().getSaddle() == null) {
                if (itemInHand.getType() == Material.SADDLE) {
                    if (CraftEngineItems.isCustomItem(itemInHand)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    // 处理低版本的马物品栏
    @EventHandler(ignoreCancelled = true)
    public void onMoveItemInHorseInventory(InventoryClickEvent event) {
        if (VersionHelper.isOrAbove1_21_2()) return;
        if (!(event.getInventory() instanceof HorseInventory horseInventory)) {
            return;
        }
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            ItemStack currentItem = event.getCurrentItem();
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (currentItem != null && CraftEngineItems.isCustomItem(currentItem)) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getClickedInventory() == horseInventory) {
            ItemStack itemInCursor = event.getCursor();
            if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR || event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE) {
                if (!ItemUtils.isEmpty(itemInCursor) && CraftEngineItems.isCustomItem(itemInCursor)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
                int slot = event.getHotbarButton();
                if (slot != -1) {
                    ItemStack itemInHotBar = event.getWhoClicked().getInventory().getItem(slot);
                    if (!ItemUtils.isEmpty(itemInHotBar) && CraftEngineItems.isCustomItem(itemInHotBar)) {
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    ItemStack offHand = event.getWhoClicked().getInventory().getItemInOffHand();
                    if (!ItemUtils.isEmpty(offHand) && CraftEngineItems.isCustomItem(offHand)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveItemInHorseInventory(InventoryDragEvent event) {
        if (VersionHelper.isOrAbove1_21_2()) return;
        if (!(event.getInventory() instanceof HorseInventory horseInventory)) {
            return;
        }
        for (Map.Entry<Integer, ItemStack> item : event.getNewItems().entrySet()) {
            if (item.getKey() == 0 || item.getKey() == 1) {
                if (!ItemUtils.isEmpty(item.getValue()) && CraftEngineItems.isCustomItem(item.getValue())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
