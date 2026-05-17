package net.momirealms.craftengine.proxy.minecraft.world.damagesource;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.damagesource.DamageSources")
public interface DamageSourcesProxy {
    DamageSourcesProxy INSTANCE = ASMProxyFactory.create(DamageSourcesProxy.class);

    @MethodInvoker(name = "fall")
    Object fall(Object target);

    @MethodInvoker(name = "fallingBlock")
    Object fallingBlock(Object target, @Type(clazz = EntityProxy.class) Object attacker);
}
