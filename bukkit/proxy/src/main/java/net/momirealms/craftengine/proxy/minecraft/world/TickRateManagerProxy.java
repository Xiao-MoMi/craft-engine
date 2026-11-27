package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.TickRateManager", activeIf = "min_version=1.20.3")
public interface TickRateManagerProxy {
    TickRateManagerProxy INSTANCE = ASMProxyFactory.create(TickRateManagerProxy.class);

    @MethodInvoker(name = "runsNormally")
    boolean runsNormally(Object target);
}
