package net.momirealms.craftengine.proxy.minecraft.server.packs.repository;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.packs.repository.Pack")
public interface PackProxy {
    PackProxy INSTANCE = ASMProxyFactory.create(PackProxy.class);

    @MethodInvoker(name = "open")
    Object open(Object target);
}
