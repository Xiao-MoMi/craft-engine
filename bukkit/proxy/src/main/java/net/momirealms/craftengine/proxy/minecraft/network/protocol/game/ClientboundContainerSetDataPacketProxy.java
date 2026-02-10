package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket")
public interface ClientboundContainerSetDataPacketProxy {
    ClientboundContainerSetDataPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundContainerSetDataPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int containerId, int stateId, int value);
}
