package net.momirealms.craftengine.bukkit.item.listener;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;


public final class PaperSlotChangeListener implements Listener {
    private final BukkitItemManager itemManager;

    public PaperSlotChangeListener(BukkitItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSlotChange(final PlayerInventorySlotChangeEvent event) {
        ItemStack newItemStack = event.getNewItemStack();
        Item wrap = this.itemManager.wrap(newItemStack);
        ItemDefinition itemDefinition = wrap.getDefinitionOrNull();
        if (itemDefinition != null) {
            if (!itemDefinition.settings().triggerAdvancement()) {
                event.setShouldTriggerAdvancements(false);
            }
        }
        this.itemManager.unlockRecipeOnInventoryChanged(event.getPlayer(), wrap);
    }
}
