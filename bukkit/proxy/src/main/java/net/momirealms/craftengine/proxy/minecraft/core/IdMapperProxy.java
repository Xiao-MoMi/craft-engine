package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.IdMapper")
public interface IdMapperProxy {
    IdMapperProxy INSTANCE = ASMProxyFactory.create(IdMapperProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.IdMapper");

    @MethodInvoker(name = "add")
    void add(Object target, Object key);

    @MethodInvoker(name = "size")
    int size(Object target);
}
