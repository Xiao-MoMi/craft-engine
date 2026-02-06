package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.Direction")
public interface DirectionProxy {
    DirectionProxy INSTANCE = ASMProxyFactory.create(DirectionProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object DOWN = VALUES[0];
    Object UP = VALUES[1];
    Object NORTH = VALUES[2];
    Object SOUTH = VALUES[3];
    Object WEST = VALUES[4];
    Object EAST = VALUES[5];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();

    @MethodInvoker(name = "getAxis")
    Object getAxis(Object target);
}
