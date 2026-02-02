package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftComplexRecipe")
public interface CraftComplexRecipeProxy {
    CraftComplexRecipeProxy INSTANCE = ReflectionHelper.getProxy(CraftComplexRecipeProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftComplexRecipeProxy.class);

    @FieldGetter(name = "recipe")
    Object recipe(Object instance);

    @FieldSetter(name = "recipe", strategy = Strategy.MH)
    void recipe(Object instance, Object recipe);
}
