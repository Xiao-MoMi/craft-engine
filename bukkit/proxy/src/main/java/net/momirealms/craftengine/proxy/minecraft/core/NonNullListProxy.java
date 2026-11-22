package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.NonNullList")
public interface NonNullListProxy {
    NonNullListProxy INSTANCE = ASMProxyFactory.create(NonNullListProxy.class);

    @MethodInvoker(name = "createWithCapacity", isStatic = true)
    Object createWithCapacity(int capacity);

    @MethodInvoker(name = "withSize", isStatic = true)
    Object withSize(int size, Object value);
}
