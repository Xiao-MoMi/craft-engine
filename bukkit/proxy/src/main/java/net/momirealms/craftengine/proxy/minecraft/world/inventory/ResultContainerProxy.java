package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.ResultContainer")
public interface ResultContainerProxy {
    ResultContainerProxy INSTANCE = ASMProxyFactory.create(ResultContainerProxy.class);

    @MethodInvoker(name = "getRecipeUsed")
    Object getRecipeUsed(Object target);
}
