package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.UpdateInterval", activeIf = "min_version=26.3")
public interface UpdateIntervalProxy {
    UpdateIntervalProxy INSTANCE = ASMProxyFactory.create(UpdateIntervalProxy.class);
    @MethodInvoker(name = "periodic", isStatic = true)
    Object periodic(int ticks);
}
