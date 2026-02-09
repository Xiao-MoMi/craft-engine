package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.RegistryAccess")
public interface RegistryAccessProxy extends HolderLookupProxy.ProviderProxy {
    RegistryAccessProxy INSTANCE = ASMProxyFactory.create(RegistryAccessProxy.class);

    @MethodInvoker(name = "registryOrThrow")
    Object registryOrThrow(Object target, @Type(clazz = ResourceKeyProxy.class) Object resourceKey);
}
