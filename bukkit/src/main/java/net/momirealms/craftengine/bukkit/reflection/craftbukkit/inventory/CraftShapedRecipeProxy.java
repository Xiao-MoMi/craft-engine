package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.ShapedRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftShapedRecipe")
public interface CraftShapedRecipeProxy {
    CraftShapedRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftShapedRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftShapedRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    ShapedRecipe fromBukkitRecipe(ShapedRecipe recipe);
}
