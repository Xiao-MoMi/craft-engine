package net.momirealms.craftengine.proxy.minecraft.world.ticks;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.ticks.TickPriority")
public interface TickPriorityProxy {
    TickPriorityProxy INSTANCE = ASMProxyFactory.create(TickPriorityProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> EXTREMELY_HIGH = VALUES[0];
    Enum<?> VERY_HIGH = VALUES[1];
    Enum<?> HIGH = VALUES[2];
    Enum<?> NORMAL = VALUES[3];
    Enum<?> LOW = VALUES[4];
    Enum<?> VERY_LOW = VALUES[5];
    Enum<?> EXTREMELY_LOW = VALUES[6];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();

    @MethodInvoker(name = "getValue")
    int getValue(Object target);
}
