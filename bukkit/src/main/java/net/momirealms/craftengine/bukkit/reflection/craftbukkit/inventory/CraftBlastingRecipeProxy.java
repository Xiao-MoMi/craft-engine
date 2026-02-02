package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.BlastingRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftBlastingRecipe")
public interface CraftBlastingRecipeProxy {
    CraftBlastingRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftBlastingRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlastingRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    BlastingRecipe fromBukkitRecipe(BlastingRecipe recipe);
}
