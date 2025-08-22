package net.momirealms.craftengine.bukkit.item.recipe;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.item.recipe.Ingredient;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class CustomIngredientSet extends HashSet<Object> {
    private final Ingredient<ItemStack> ingredient;

    public CustomIngredientSet(@NotNull Collection<?> c, Ingredient<ItemStack> ingredient) {
        super(c);
        this.ingredient = ingredient;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null || FastNMS.INSTANCE.method$ItemStack$isEmpty(o)) {
            return false;
        }
        return this.ingredient.test(UniqueIdItem.of(BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(o))));
    }
}
