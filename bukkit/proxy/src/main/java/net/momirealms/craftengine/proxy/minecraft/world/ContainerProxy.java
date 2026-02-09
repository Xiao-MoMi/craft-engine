package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.Container")
public interface ContainerProxy {
    ContainerProxy INSTANCE = ASMProxyFactory.create(ContainerProxy.class);

    @MethodInvoker(name = "getCurrentRecipe", activeIf = "max_version=1.20.6")
    Object getCurrentRecipe(Object target);

    @MethodInvoker(name = "getContainerSize")
    int getContainerSize(Object target);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target, int index);
}
