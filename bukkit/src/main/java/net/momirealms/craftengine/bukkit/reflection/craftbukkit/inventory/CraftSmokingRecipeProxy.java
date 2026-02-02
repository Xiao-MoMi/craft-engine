package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.SmokingRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftSmokingRecipe")
public interface CraftSmokingRecipeProxy {
    CraftSmokingRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftSmokingRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftSmokingRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    SmokingRecipe fromBukkitRecipe(SmokingRecipe recipe);
}
