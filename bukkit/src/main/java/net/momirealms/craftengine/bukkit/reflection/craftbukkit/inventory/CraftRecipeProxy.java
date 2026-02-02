package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftRecipe")
public interface CraftRecipeProxy {
    CraftRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftRecipeProxy.class);

    @MethodInvoker(name = "addToCraftingManager")
    void addToCraftingManager(Recipe instance);

    @MethodInvoker(name = "toMinecraft", isStatic = true, version = ">=1.21.2")
    Object toMinecraft(NamespacedKey key);

    @MethodInvoker(name = "toIngredient", isStatic = true)
    Object toIngredient(RecipeChoice bukkit, boolean requireNotEmpty);
}
