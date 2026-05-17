package net.momirealms.craftengine.bukkit.item.listener;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class SlotChangeListener implements Listener {
    private final BukkitItemManager itemManager;

    public SlotChangeListener(BukkitItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSlotChange(final PlayerInventorySlotChangeEvent event) {
        ItemStack newItemStack = event.getNewItemStack();
        Item wrap = this.itemManager.wrap(newItemStack);
        Optional<ItemDefinition> optionalCustomItem = wrap.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            if (!itemDefinition.settings().triggerAdvancement()) {
                event.setShouldTriggerAdvancements(false);
            }
        }
        this.itemManager.unlockRecipeOnInventoryChanged(event.getPlayer(), wrap);
    }
}
