package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.RegistryAccess")
public interface RegistryAccessProxy extends HolderLookupProxy.ProviderProxy {
    RegistryAccessProxy INSTANCE = ASMProxyFactory.create(RegistryAccessProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.RegistryAccess");

    @MethodInvoker(name = {"lookupOrThrow", "registryOrThrow"})
    Object lookupOrThrow(Object target, @Type(clazz = ResourceKeyProxy.class) Object resourceKey);
}
