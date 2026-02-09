package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Collection;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket")
public interface ClientboundUpdateAttributesPacketProxy {
    ClientboundUpdateAttributesPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundUpdateAttributesPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int entityId, Collection<?> attributes);
}
