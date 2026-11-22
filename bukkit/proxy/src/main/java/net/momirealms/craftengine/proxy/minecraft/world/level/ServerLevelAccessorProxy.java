package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.ServerLevelAccessor")
public interface ServerLevelAccessorProxy {
    ServerLevelAccessorProxy INSTANCE = ASMProxyFactory.create(ServerLevelAccessorProxy.class);

    @MethodInvoker(name = "getLevel")
    Object getLevel(Object target);
}
