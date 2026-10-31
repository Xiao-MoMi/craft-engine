package net.momirealms.craftengine.bedrock.geyser.proxy.session;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import io.netty.channel.EventLoop;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.geyser.session.GeyserSession")
public interface GeyserSessionProxy {
    GeyserSessionProxy INSTANCE = ASMProxyFactory.create(GeyserSessionProxy.class);

    @MethodInvoker(name = "getWorldName")
    Object worldName(Object target);

    @FieldSetter(name = "tickEventLoop")
    void tickEventLoop(Object target, EventLoop eventLoop);
}
