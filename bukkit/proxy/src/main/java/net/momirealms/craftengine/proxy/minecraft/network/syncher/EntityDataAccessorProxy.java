package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.syncher.EntityDataAccessor")
public interface EntityDataAccessorProxy {
    EntityDataAccessorProxy INSTANCE = ASMProxyFactory.create(EntityDataAccessorProxy.class);
}
