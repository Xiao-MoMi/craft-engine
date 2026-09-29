package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.TickRateManager")
public interface TickRateManagerProxy {
    TickRateManagerProxy INSTANCE = ASMProxyFactory.create(TickRateManagerProxy.class);

    @MethodInvoker(name = "runsNormally")
    boolean runsNormally(Object target);
}
