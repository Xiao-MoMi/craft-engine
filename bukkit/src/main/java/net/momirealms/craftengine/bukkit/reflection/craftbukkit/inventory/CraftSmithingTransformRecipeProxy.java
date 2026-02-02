package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.SmithingTransformRecipe;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftSmithingTransformRecipe")
public interface CraftSmithingTransformRecipeProxy {
    CraftSmithingTransformRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftSmithingTransformRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftSmithingTransformRecipeProxy.class);

    @MethodInvoker(name = "fromBukkitRecipe", isStatic = true)
    SmithingTransformRecipe fromBukkitRecipe(SmithingTransformRecipe recipe);
}
