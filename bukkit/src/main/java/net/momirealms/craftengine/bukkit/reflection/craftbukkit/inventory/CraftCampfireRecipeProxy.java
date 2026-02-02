package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.CampfireRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftCampfireRecipe")
public interface CraftCampfireRecipeProxy {
    CraftCampfireRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftCampfireRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftCampfireRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    CampfireRecipe fromBukkitRecipe(CampfireRecipe recipe);
}
