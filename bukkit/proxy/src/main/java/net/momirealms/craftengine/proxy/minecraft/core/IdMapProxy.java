package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.IdMap")
public interface IdMapProxy {
    IdMapProxy INSTANCE = ASMProxyFactory.create(IdMapProxy.class);

    @MethodInvoker(name = "getId")
    int getId(Object target, Object value);

    @MethodInvoker(name = "byId")
    <T> T byId(Object target, int id);

    @MethodInvoker(name = "size")
    int size(Object target);
}
