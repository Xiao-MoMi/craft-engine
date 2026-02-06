package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftComplexRecipe")
public interface CraftComplexRecipeProxy {
    CraftComplexRecipeProxy INSTANCE = ASMProxyFactory.create(CraftComplexRecipeProxy.class);
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.inventory.CraftComplexRecipe");

    @FieldGetter(name = "recipe")
    Object getRecipe(Object target);
}
