package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.Container")
public interface ContainerProxy {
    ContainerProxy INSTANCE = ASMProxyFactory.create(ContainerProxy.class);

    @MethodInvoker(name = "getCurrentRecipe")
    Object getCurrentRecipe(Object target);
}
