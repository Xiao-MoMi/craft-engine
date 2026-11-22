package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.CraftingRecipe$CraftingBookInfo", activeIf = "min_version=26.1")
public interface CraftingBookInfoProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.CraftingRecipe$CraftingBookInfo");
}
