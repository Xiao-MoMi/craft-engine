package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.CraftingInput", activeIf = "min_version=1.21")
public interface CraftingInputProxy {
    CraftingInputProxy INSTANCE = ASMProxyFactory.create(CraftingInputProxy.class);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target, int index);

    @MethodInvoker(name = "size")
    int size(Object target);

    @MethodInvoker(name = "ingredientCount")
    int ingredientCount(Object target);
}
