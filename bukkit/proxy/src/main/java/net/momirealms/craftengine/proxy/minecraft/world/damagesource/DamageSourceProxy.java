package net.momirealms.craftengine.proxy.minecraft.world.damagesource;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.damagesource.DamageSource")
public interface DamageSourceProxy {
    DamageSourceProxy INSTANCE = ASMProxyFactory.create(DamageSourceProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.damagesource.DamageSource");

    @FieldGetter(name = "causingEntity")
    Object getCausingEntity(Object target);

    @FieldGetter(name = "directEntity")
    Object getDirectEntity(Object target);

    @FieldGetter(name = "critical", optional = true)
    boolean isCritical(Object target);

    @FieldGetter(name = "type")
    Object getType(Object target);
}
