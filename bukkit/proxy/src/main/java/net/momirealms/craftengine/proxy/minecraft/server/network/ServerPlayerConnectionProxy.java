package net.momirealms.craftengine.proxy.minecraft.server.network;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.network.ServerPlayerConnection")
public interface ServerPlayerConnectionProxy {
    ServerPlayerConnectionProxy INSTANCE = ASMProxyFactory.create(ServerPlayerConnectionProxy.class);

    @MethodInvoker(name = "getPlayer")
    Object getPlayer(Object target);

    @MethodInvoker(name = "send")
    void send(Object target, @Type(clazz = PacketProxy.class) Object packet);
}
