package net.momirealms.craftengine.proxy.minecraft.world.damagesource;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.damagesource.DamageSource")
public interface DamageSourceProxy {
    DamageSourceProxy INSTANCE = ASMProxyFactory.create(DamageSourceProxy.class);

    @FieldGetter(name = "causingEntity")
    Object getCausingEntity(Object target);

    @FieldGetter(name = "directEntity")
    Object getDirectEntity(Object target);
}
