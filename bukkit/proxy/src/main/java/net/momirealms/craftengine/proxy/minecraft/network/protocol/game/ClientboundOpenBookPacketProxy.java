package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundOpenBookPacket")
public interface ClientboundOpenBookPacketProxy {
    ClientboundOpenBookPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundOpenBookPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = InteractionHandProxy.class) Object hand);
}
