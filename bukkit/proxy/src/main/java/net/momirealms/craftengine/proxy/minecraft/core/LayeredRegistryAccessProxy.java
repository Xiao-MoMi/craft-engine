package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.LayeredRegistryAccess")
public interface LayeredRegistryAccessProxy {
    LayeredRegistryAccessProxy INSTANCE = ASMProxyFactory.create(LayeredRegistryAccessProxy.class);
}
