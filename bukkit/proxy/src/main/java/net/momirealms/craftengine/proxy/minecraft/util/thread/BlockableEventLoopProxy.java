package net.momirealms.craftengine.proxy.minecraft.util.thread;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.thread.BlockableEventLoop")
public interface BlockableEventLoopProxy {
    BlockableEventLoopProxy INSTANCE = ASMProxyFactory.create(BlockableEventLoopProxy.class);

    @MethodInvoker(name = "scheduleOnMain")
    void scheduleOnMain(Object target, Runnable runnable);
}
