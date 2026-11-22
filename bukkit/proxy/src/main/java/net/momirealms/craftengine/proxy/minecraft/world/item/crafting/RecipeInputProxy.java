package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeInput", activeIf = "min_version=1.21")
public interface RecipeInputProxy {
    RecipeInputProxy INSTANCE = ASMProxyFactory.create(RecipeInputProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.RecipeInput");

    @MethodInvoker(name = "getItem")
    Object getItem(Object target, int index);

    @ReflectionProxy(name = "net.minecraft.world.item.crafting.SingleRecipeInput", activeIf = "min_version=1.21")
    interface Single {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.SingleRecipeInput");
    }

    @ReflectionProxy(name = "net.minecraft.world.item.crafting.SmithingRecipeInput", activeIf = "min_version=1.21")
    interface Smithing {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.SmithingRecipeInput");
    }

    @ReflectionProxy(name = "net.minecraft.world.item.crafting.BrewingInput", activeIf = "min_version=26.3")
    interface Brewing {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.BrewingInput");
    }
}
