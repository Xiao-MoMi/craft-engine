package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.concurrent.atomic.AtomicInteger;

@ReflectionProxy(name = "net.minecraft.world.entity.Entity")
public interface EntityProxy {
    EntityProxy INSTANCE = ASMProxyFactory.create(EntityProxy.class);
    AtomicInteger ENTITY_COUNTER = INSTANCE.getEntityCounter();

    @FieldGetter(name = "ENTITY_COUNTER")
    AtomicInteger getEntityCounter();

    @FieldGetter(name = "xo")
    double getXo(Object target);

    @FieldSetter(name = "xo")
    void setXo(Object target, double xo);

    @FieldGetter(name = "yo")
    double getYo(Object target);

    @FieldSetter(name = "yo")
    void setYo(Object target, double yo);

    @FieldGetter(name = "zo")
    double getZo(Object target);

    @FieldSetter(name = "zo")
    void setZo(Object target, double zo);
}
