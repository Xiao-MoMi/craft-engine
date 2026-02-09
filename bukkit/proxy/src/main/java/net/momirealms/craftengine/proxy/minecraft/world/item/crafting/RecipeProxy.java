package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.Recipe")
public interface RecipeProxy {
    RecipeProxy INSTANCE = ASMProxyFactory.create(RecipeProxy.class);
}
