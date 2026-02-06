package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.RegistryAccess")
public interface RegistryAccessProxy extends HolderLookupProxy.ProviderProxy {
    RegistryAccessProxy INSTANCE = ASMProxyFactory.create(RegistryAccessProxy.class);

}
