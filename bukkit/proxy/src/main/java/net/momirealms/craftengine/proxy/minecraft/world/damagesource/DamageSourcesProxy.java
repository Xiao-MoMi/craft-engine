package net.momirealms.craftengine.proxy.minecraft.world.damagesource;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.damagesource.DamageSources")
public interface DamageSourcesProxy {
    DamageSourcesProxy INSTANCE = ASMProxyFactory.create(DamageSourcesProxy.class);

    @MethodInvoker(name = "fall")
    Object fall(Object target);
}
