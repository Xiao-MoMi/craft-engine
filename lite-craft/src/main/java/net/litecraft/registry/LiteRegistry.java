package net.litecraft.registry;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class LiteRegistry {

    private final JavaPlugin plugin;
    private final Map<String, Object> customBlocks = new HashMap<>();
    private final Map<String, Integer> customItemModelData = new HashMap<>();

    public LiteRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerBlock(String id, Object customBlockInstance) {
        customBlocks.put(id, customBlockInstance);
    }

    public void registerItem(String id, int modelData) {
        customItemModelData.put(id, modelData);
    }

    public ItemStack createCustomItem(ItemStack base, String id) {
        if (!customItemModelData.containsKey(id)) return base;

        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customItemModelData.get(id));
            item.setItemMeta(meta);
        }
        return item;
    }

    public Object getBlock(String id) {
        return customBlocks.get(id);
    }

    public void registerShapedRecipe(String id, ItemStack result, String[] pattern, Map<Character, Material> ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(pattern);
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        plugin.getServer().addRecipe(recipe);
    }
}
