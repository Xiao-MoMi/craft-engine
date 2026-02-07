package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.Registry")
public interface RegistryProxy {
    RegistryProxy INSTANCE = ASMProxyFactory.create(RegistryProxy.class);

    @MethodInvoker(name = "registerForHolder", isStatic = true)
    Object registerForHolder$0(@Type(clazz = RegistryProxy.class) Object registry,
                               @Type(clazz = ResourceKeyProxy.class) Object resourceKey,
                               Object value);

    @MethodInvoker(name = "registerForHolder", isStatic = true)
    Object registerForHolder$1(@Type(clazz = RegistryProxy.class) Object registry,
                               @Type(clazz = IdentifierProxy.class) Object identifier,
                               Object value);

    @MethodInvoker(name = "asLookup", activeIf = "max_version=1.21.1")
    Object asLookup(Object target);
}
