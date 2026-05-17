package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.FireworkStarFadeRecipe")
public interface FireworkStarFadeRecipeProxy {
    FireworkStarFadeRecipeProxy INSTANCE = ASMProxyFactory.create(FireworkStarFadeRecipeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.FireworkStarFadeRecipe");

    @FieldGetter(name = "target", activeIf = "min_version=26.1")
    Object getTarget(Object target);

    @FieldGetter(name = "dye", activeIf = "min_version=26.1")
    Object getDye(Object target);

    @FieldGetter(name = "result", activeIf = "min_version=26.1")
    Object getResult(Object target);
}
