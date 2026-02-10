package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket")
public interface ClientboundSetActionBarTextPacketProxy {
    ClientboundSetActionBarTextPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetActionBarTextPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object text);
}
