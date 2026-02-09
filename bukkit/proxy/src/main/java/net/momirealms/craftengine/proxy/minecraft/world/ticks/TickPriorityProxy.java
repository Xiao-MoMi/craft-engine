package net.momirealms.craftengine.proxy.minecraft.world.ticks;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.ticks.TickPriority")
public interface TickPriorityProxy {
    TickPriorityProxy INSTANCE = ASMProxyFactory.create(TickPriorityProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object EXTREMELY_HIGH = VALUES[0];
    Object VERY_HIGH = VALUES[1];
    Object HIGH = VALUES[2];
    Object NORMAL = VALUES[3];
    Object LOW = VALUES[4];
    Object VERY_LOW = VALUES[5];
    Object EXTREMELY_LOW = VALUES[6];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();

    @MethodInvoker(name = "getValue")
    int getValue(Object target);
}
