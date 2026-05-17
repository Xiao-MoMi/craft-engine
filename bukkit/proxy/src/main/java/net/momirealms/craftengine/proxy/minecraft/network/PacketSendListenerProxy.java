package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.PacketSendListener")
public interface PacketSendListenerProxy {
    PacketSendListenerProxy INSTANCE = ASMProxyFactory.create(PacketSendListenerProxy.class);

    @MethodInvoker(name = "thenRun", isStatic = true)
    Object thenRun(Runnable runnable);
}
