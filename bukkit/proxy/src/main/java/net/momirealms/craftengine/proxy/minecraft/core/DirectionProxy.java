package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.Direction")
public interface DirectionProxy {
    DirectionProxy INSTANCE = ASMProxyFactory.create(DirectionProxy.class);
    @SuppressWarnings("unchecked")
    Class<? extends Enum<?>> CLASS = (Class<? extends Enum<?>>) SparrowClass.find("net.minecraft.core.Direction");
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> DOWN = VALUES[0];
    Enum<?> UP = VALUES[1];
    Enum<?> NORTH = VALUES[2];
    Enum<?> SOUTH = VALUES[3];
    Enum<?> WEST = VALUES[4];
    Enum<?> EAST = VALUES[5];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();

    @MethodInvoker(name = "getAxis")
    Object getAxis(Object target);

    @MethodInvoker(name = "getOpposite")
    Object getOpposite(Object target);

    @MethodInvoker(name = "orderedByNearest", isStatic = true)
    Enum<?>[] orderedByNearest(@Type(clazz = EntityProxy.class) Object entity);
}
