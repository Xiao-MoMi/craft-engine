package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.BrewingRecipe", activeIf = "min_version=26.3")
public interface BrewingRecipeProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.BrewingRecipe");
}
