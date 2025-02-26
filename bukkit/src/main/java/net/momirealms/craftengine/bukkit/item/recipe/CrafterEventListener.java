package net.momirealms.craftengine.bukkit.item.recipe;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.RecipeTypes;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CrafterEventListener implements Listener {
    private static final OptimizedIDItem<ItemStack> EMPTY = new OptimizedIDItem<>(null, null);
    private final ItemManager<ItemStack> itemManager;
    private final BukkitRecipeManager recipeManager;
    private final BukkitCraftEngine plugin;

    public CrafterEventListener(BukkitCraftEngine plugin, BukkitRecipeManager recipeManager, ItemManager<ItemStack> itemManager) {
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onCrafting(CrafterCraftEvent event) {
        CraftingRecipe recipe = event.getRecipe();
        if (!(event.getBlock().getState() instanceof Crafter crafter)) {
            return;
        }

        Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);

        // Maybe it's recipe from other plugins, then we ignore it
        if (!isCustom) {
            return;
        }

        Inventory inventory = crafter.getInventory();
        ItemStack[] ingredients = inventory.getStorageContents();

        List<OptimizedIDItem<ItemStack>> optimizedIDItems = new ArrayList<>();
        for (ItemStack itemStack : ingredients) {
            if (ItemUtils.isEmpty(itemStack)) {
                optimizedIDItems.add(EMPTY);
            } else {
                Item<ItemStack> wrappedItem = this.itemManager.wrap(itemStack);
                Optional<Holder.Reference<Key>> idHolder = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(wrappedItem.id());
                if (idHolder.isEmpty()) {
                    // an invalid item is used in recipe, we disallow it
                    event.setCancelled(true);
                    return;
                } else {
                    optimizedIDItems.add(new OptimizedIDItem<>(idHolder.get(), itemStack));
                }
            }
        }

        CraftingInput<ItemStack> input;
        if (ingredients.length == 9) {
            input = CraftingInput.of(3, 3, optimizedIDItems);
        } else if (ingredients.length == 4) {
            input = CraftingInput.of(2, 2, optimizedIDItems);
        } else {
            return;
        }

        Recipe<ItemStack> ceRecipe = this.recipeManager.getRecipe(RecipeTypes.SHAPELESS, input);
        if (ceRecipe != null) {
            event.setResult(ceRecipe.getResult(ItemBuildContext.EMPTY));
            return;
        }
        ceRecipe = this.recipeManager.getRecipe(RecipeTypes.SHAPED, input);
        if (ceRecipe != null) {
            event.setResult(ceRecipe.getResult(ItemBuildContext.EMPTY));
            return;
        }
        // clear result if not met
        event.setCancelled(true);
    }
}
