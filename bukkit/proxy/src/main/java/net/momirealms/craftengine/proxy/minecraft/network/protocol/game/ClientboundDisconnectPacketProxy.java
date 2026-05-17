package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ClientboundDisconnectPacket", "net.minecraft.network.protocol.game.ClientboundDisconnectPacket"})
public interface ClientboundDisconnectPacketProxy {
    ClientboundDisconnectPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundDisconnectPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object reason);
}
