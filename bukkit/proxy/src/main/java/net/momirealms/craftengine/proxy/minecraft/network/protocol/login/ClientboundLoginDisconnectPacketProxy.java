package net.momirealms.craftengine.proxy.minecraft.network.protocol.login;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket")
public interface ClientboundLoginDisconnectPacketProxy {
    ClientboundLoginDisconnectPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLoginDisconnectPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object reason);
}
