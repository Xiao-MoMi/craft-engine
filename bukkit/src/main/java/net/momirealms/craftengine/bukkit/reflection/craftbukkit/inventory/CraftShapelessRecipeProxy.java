package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.ShapelessRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftShapelessRecipe")
public interface CraftShapelessRecipeProxy {
    CraftShapelessRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftShapelessRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftShapelessRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    ShapelessRecipe fromBukkitRecipe(ShapelessRecipe recipe);
}
