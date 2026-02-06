package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.Direction$Axis")
public interface AxisProxy {
    AxisProxy INSTANCE = ASMProxyFactory.create(AxisProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object X = VALUES[0];
    Object Y = VALUES[1];
    Object Z = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}