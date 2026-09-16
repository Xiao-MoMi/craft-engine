package net.momirealms.craftengine.bukkit.item.recipe;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.recipe.CustomStoneCuttingRecipe;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;

public final class PaperRecipeEventListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStonecutterRecipeSelect(PlayerStonecutterRecipeSelectEvent event) {
        if (!Config.enableRecipeSystem()) return;
        Recipe recipe = BukkitRecipeManager.instance().recipeById(KeyUtils.namespacedKeyToKey(event.getStonecuttingRecipe().getKey())).orElse(null);
        if (recipe instanceof CustomStoneCuttingRecipe stonecuttingRecipe && stonecuttingRecipe.hasCondition()) {
            if (!stonecuttingRecipe.canUse(PlayerOptionalContext.of(BukkitAdaptor.adapt(event.getPlayer())))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        if (event.getInventory() instanceof CartographyInventory cartographyInventory) {
            if (ItemStackUtils.hasCustomItem(cartographyInventory.getStorageContents())) {
                event.setResult(new ItemStack(Material.AIR));
            }
        }
    }
}
