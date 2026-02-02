package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.FurnaceRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe")
public interface CraftFurnaceRecipeProxy {
    CraftFurnaceRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftFurnaceRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftFurnaceRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    FurnaceRecipe fromBukkitRecipe(FurnaceRecipe recipe);
}
