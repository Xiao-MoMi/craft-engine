package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.IdMapper")
public interface IdMapperProxy {
    IdMapperProxy INSTANCE = ASMProxyFactory.create(IdMapperProxy.class);

    @MethodInvoker(name = "add")
    void add(Object target, Object key);
}
