package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.Ingredient")
public interface IngredientProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.Ingredient");
}
