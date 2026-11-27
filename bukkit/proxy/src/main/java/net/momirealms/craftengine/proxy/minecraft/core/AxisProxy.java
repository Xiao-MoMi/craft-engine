package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.Direction$Axis")
public interface AxisProxy {
    AxisProxy INSTANCE = ASMProxyFactory.create(AxisProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> X = VALUES[0];
    Enum<?> Y = VALUES[1];
    Enum<?> Z = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();

    @MethodInvoker(name = "getRandom", isStatic = true)
    Enum<?> getRandom(@Type(clazz = RandomSourceProxy.class) Object random);
}
