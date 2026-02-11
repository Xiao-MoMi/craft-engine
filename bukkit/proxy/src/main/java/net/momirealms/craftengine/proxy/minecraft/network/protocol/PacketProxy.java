package net.momirealms.craftengine.proxy.minecraft.network.protocol;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.Packet")
public interface PacketProxy {
    PacketProxy INSTANCE = ASMProxyFactory.create(PacketProxy.class);
}
