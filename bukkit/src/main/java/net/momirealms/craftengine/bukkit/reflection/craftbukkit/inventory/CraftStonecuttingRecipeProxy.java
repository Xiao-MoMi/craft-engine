package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.StonecuttingRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftStonecuttingRecipe")
public interface CraftStonecuttingRecipeProxy {
    CraftStonecuttingRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftStonecuttingRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftStonecuttingRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    StonecuttingRecipe fromBukkitRecipe(StonecuttingRecipe recipe);
}
